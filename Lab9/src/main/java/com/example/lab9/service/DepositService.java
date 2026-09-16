package com.example.lab9.service;

import com.example.lab9.model.Account;
import com.example.lab9.model.DepositTransaction;
import com.example.lab9.repository.AccountRepository;
import com.example.lab9.repository.DepositRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepositService {
    private final AccountRepository accountRepository;
    private final DepositRepository depositRepository;

    public DepositService(AccountRepository accountRepository, DepositRepository depositRepository) {
        this.accountRepository = accountRepository;
        this.depositRepository = depositRepository;
    }

    public void deposit(Long accountId, Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        double newBalance = account.getBalance() + amount;
        if (!Double.isFinite(newBalance)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance is too large");
        }

        // การเพิ่มยอดเงินและบันทึกประวัติอยู่ใน Transaction เดียวกัน
        account.setBalance(newBalance);
        accountRepository.saveAndFlush(account);

        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(amount);
        transaction.setAccount(account);
        depositRepository.saveAndFlush(transaction);

        // เปิดบรรทัดนี้เพื่อทดลองข้อ 12 และ 13 แล้วปิดหลังทดลองเสร็จ
        throw new RuntimeException("Test Rollback");
    }
}
