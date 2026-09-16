package com.example.lab9.service;

import com.example.lab9.model.Account;
import com.example.lab9.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(Account account) {
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()
                || account.getOwnerName() == null || account.getOwnerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account number and owner name are required");
        }
        Double balance = account.getBalance();
        if (balance == null || !Double.isFinite(balance) || balance < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance must be zero or positive");
        }
        account.setId(null); // สร้างบัญชีใหม่ ไม่แก้บัญชีเดิมจาก id ที่ส่งมา
        return accountRepository.save(account);
    }

    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}
