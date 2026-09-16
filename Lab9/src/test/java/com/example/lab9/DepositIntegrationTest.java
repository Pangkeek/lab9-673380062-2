package com.example.lab9;

import com.example.lab9.model.Account;
import com.example.lab9.repository.AccountRepository;
import com.example.lab9.repository.DepositRepository;
import com.example.lab9.service.DepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ใช้ฐานข้อมูล lab9_test แยก เพราะทดสอบแล้วล้างข้อมูลทุกครั้ง
@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:5432/lab9_test}",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DepositIntegrationTest {
    @Autowired AccountRepository accounts;
    @SpyBean DepositRepository deposits;
    @Autowired DepositService service;
    private Long accountId;

    @BeforeEach
    void setup() {
        reset(deposits);
        deposits.deleteAll();
        accounts.deleteAll();
        Account account = new Account();
        account.setAccountNumber("1234567890");
        account.setOwnerName("Student");
        account.setBalance(0.0);
        accountId = accounts.saveAndFlush(account).getId();
    }

    @Test
    void successfulDepositCommitsBothTables() {
        service.deposit(accountId, 1000.0);
        assertEquals(1000.0, accounts.findById(accountId).orElseThrow().getBalance());
        assertEquals(1, deposits.count());
        assertEquals(1000.0, deposits.findAll().get(0).getAmount());
        assertEquals(accountId, deposits.findAll().get(0).getAccount().getId());
    }

    @Test
    void failureAfterBothWritesRollsBackBothTables() {
        service.deposit(accountId, 1000.0);
        failAfterSavingHistory();
        assertThrows(RuntimeException.class, () -> service.deposit(accountId, 1000.0));
        assertEquals(1000.0, accounts.findById(accountId).orElseThrow().getBalance());
        assertEquals(1, deposits.count());
    }

    @Test
    void withoutServiceTransactionBothWritesRemainAfterFailure() {
        service.deposit(accountId, 1000.0);
        // โยน error หลัง Repository กลับมาแล้ว จึงอยู่นอก Transaction ของ Repository
        DepositRepository failingHistory = mock(DepositRepository.class);
        doAnswer(invocation -> {
            deposits.saveAndFlush(invocation.getArgument(0));
            throw new RuntimeException("Test Rollback");
        }).when(failingHistory).saveAndFlush(any());
        // สร้างเองเพื่อข้าม Spring proxy จำลองการเอา @Transactional ออก
        DepositService withoutTransaction = new DepositService(accounts, failingHistory);
        assertThrows(RuntimeException.class, () -> withoutTransaction.deposit(accountId, 1000.0));
        assertEquals(2000.0, accounts.findById(accountId).orElseThrow().getBalance());
        assertEquals(2, deposits.count());
    }

    @Test
    void invalidAmountsDoNotChangeData() {
        for (Double amount : new Double[]{null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(ResponseStatusException.class, () -> service.deposit(accountId, amount));
        }
        assertEquals(0.0, accounts.findById(accountId).orElseThrow().getBalance());
        assertEquals(0, deposits.count());
    }

    @Test
    void missingAccountDoesNotCreateHistory() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deposit(Long.MAX_VALUE, 1000.0));
        assertEquals(404, error.getStatusCode().value());
        assertEquals(0, deposits.count());
    }

    private void failAfterSavingHistory() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new RuntimeException("Test Rollback");
        }).when(deposits).saveAndFlush(any());
    }
}
