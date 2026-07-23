package com.erp.system.finance.config;

import com.erp.system.finance.entity.Account;
import com.erp.system.finance.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Accounts already exist, skipping seed");
            return;
        }

        Account[] defaults = {
            createAccount("1000", "Cash", "ASSET"),
            createAccount("1100", "Accounts Receivable", "ASSET"),
            createAccount("1200", "Inventory", "ASSET"),
            createAccount("2000", "Accounts Payable", "LIABILITY"),
            createAccount("3000", "Owner's Equity", "EQUITY"),
            createAccount("4000", "Revenue", "REVENUE"),
            createAccount("5000", "Cost of Goods Sold", "EXPENSE"),
            createAccount("6000", "Salaries Expense", "EXPENSE"),
            createAccount("7000", "Rent Expense", "EXPENSE"),
        };

        accountRepository.saveAll(java.util.List.of(defaults));
        log.info("Created {} default accounts", defaults.length);
    }

    private Account createAccount(String code, String name, String type) {
        return Account.builder()
                .accountCode(code)
                .accountName(name)
                .accountType(type)
                .description("Default " + type.toLowerCase() + " account")
                .build();
    }
}
