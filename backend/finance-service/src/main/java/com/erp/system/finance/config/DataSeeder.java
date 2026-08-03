package com.erp.system.finance.config;

import com.erp.system.finance.entity.Account;
import com.erp.system.finance.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        Account[] defaults = {
            createAccount("1000", "Cash", "ASSET"),
            createAccount("1100", "Accounts Receivable", "ASSET"),
            createAccount("1200", "Inventory", "ASSET"),
            createAccount("2000", "Accounts Payable", "LIABILITY"),
            createAccount("2200", "Tax Payable (TVA collectée)", "LIABILITY"),
            createAccount("3000", "Owner's Equity", "EQUITY"),
            createAccount("4000", "Revenue", "REVENUE"),
            createAccount("5000", "Cost of Goods Sold", "EXPENSE"),
            createAccount("6000", "Salaries Expense", "EXPENSE"),
            createAccount("7000", "Rent Expense", "EXPENSE"),
        };

        List<Account> missing = java.util.Arrays.stream(defaults)
                .filter(account -> accountRepository.findByAccountCode(account.getAccountCode()).isEmpty())
                .toList();
        if (missing.isEmpty()) {
            log.info("All default accounts already present, skipping seed");
            return;
        }
        accountRepository.saveAll(missing);
        log.info("Created {} missing default accounts", missing.size());
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
