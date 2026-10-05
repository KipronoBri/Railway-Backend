package com.accountservice.service;

import com.accountservice.dto.AccountResponse;
import com.accountservice.dto.CreateAccountRequest;
import com.accountservice.entity.Account;
import com.accountservice.entity.AccountStatus;
import com.accountservice.entity.AccountType;
import com.accountservice.exception.AccountNotActiveException;
import com.accountservice.exception.AccountNotFoundExcepton;
import com.accountservice.exception.EmailAlreadyExistException;
import com.accountservice.exception.InsufficientFundsException;
import com.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request) {

        log.info("Creating account for: {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())) {

            throw new EmailAlreadyExistException("Account already exist for Email:" + request.getEmail());
        }

        /** use builder and annotate the  Account class with @Bulder
         * Account account = Account.builder().accountHolderName(request.getAccountHolderName()) and others
         *
         */
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
        );
        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    /**
     * Get account by account number
     *
     * @param accountNumber
     */
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundExcepton("Account Not found"));
        return mapToResponse(account);
    }

    /**
     * Get Balance
     *
     * @param accountNumber
     */
    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundExcepton("Account Not found"));
        return account.getBalance();
    }

    /**
     * Block account - called by fraud detection via kafka
     *
     * @param accountNumber
     */
    public void blockAccount(String accountNumber) {
        log.info("Blocking account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundExcepton("Account not found"));

        account.setAccountStatus(AccountStatus.BLOCKED);
        log.info("Account blocked: {}", accountNumber);

    }

    /**
     * Deduct balance from sender account
     * Called by Transaction Service
     *
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Deducting balance {} from account : {}", amount, accountNumber);

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundExcepton("Account not found"));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {

            throw new AccountNotActiveException("Account Not Active " + accountNumber);
        }
        if (account.getBalance().compareTo(amount) < 0) {

            throw new InsufficientFundsException("Not enough balance for account" + accountNumber);

        }
        account.setBalance(account.getBalance().subtract(amount));
        log.info("Balancee updated. new Balance : {}", account.getBalance());
    }

    /**
     * Credit Balance
     * Called by Transaction Service via kafka
     *
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber, BigDecimal amount) {

        log.info("Crediting  {} to account : {}", amount, accountNumber);
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundExcepton("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Balance Credited. New Balance: {}", account.getBalance());

    }


    //Generate unique 12-digit  account number
    private String generateAccountNumber() {
        String accountNumber;

        do {

            long number = secureRandom.nextLong(1000000000000L);
            accountNumber = String.format("%012d", number);

        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }


    private AccountResponse mapToResponse(Account account) {

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setAccountStatus(account.getAccountStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;

    }
}
