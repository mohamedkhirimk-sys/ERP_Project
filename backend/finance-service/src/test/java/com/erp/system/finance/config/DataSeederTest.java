package com.erp.system.finance.config;

import com.erp.system.finance.entity.Account;
import com.erp.system.finance.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void seedsAllDefaultAccountsWhenNoneExist() {
        for (String code : List.of("1000", "1100", "1200", "2000", "2200", "3000", "4000", "5000", "6000", "7000")) {
            when(accountRepository.findByAccountCode(code)).thenReturn(Optional.empty());
        }

        new DataSeeder(accountRepository).run();

        ArgumentCaptor<List<Account>> captor = ArgumentCaptor.forClass(List.class);
        verify(accountRepository).saveAll(captor.capture());
        List<Account> seeded = captor.getValue();
        assertThat(seeded).hasSize(10);
        assertThat(seeded).extracting(Account::getAccountCode)
                .contains("1000", "1100", "1200", "2000", "2200", "3000", "4000", "5000", "6000", "7000");
    }

    @Test
    void addsOnlyMissingAccountsWhenSomeExist() {
        when(accountRepository.findByAccountCode("1000")).thenReturn(Optional.of(account("1000")));
        when(accountRepository.findByAccountCode("1100")).thenReturn(Optional.of(account("1100")));
        when(accountRepository.findByAccountCode("1200")).thenReturn(Optional.of(account("1200")));
        when(accountRepository.findByAccountCode("2000")).thenReturn(Optional.of(account("2000")));
        when(accountRepository.findByAccountCode("3000")).thenReturn(Optional.of(account("3000")));
        when(accountRepository.findByAccountCode("4000")).thenReturn(Optional.of(account("4000")));
        when(accountRepository.findByAccountCode("5000")).thenReturn(Optional.of(account("5000")));
        when(accountRepository.findByAccountCode("6000")).thenReturn(Optional.of(account("6000")));
        when(accountRepository.findByAccountCode("7000")).thenReturn(Optional.of(account("7000")));
        when(accountRepository.findByAccountCode("2200")).thenReturn(Optional.empty());

        new DataSeeder(accountRepository).run();

        ArgumentCaptor<List<Account>> captor = ArgumentCaptor.forClass(List.class);
        verify(accountRepository).saveAll(captor.capture());
        List<Account> seeded = captor.getValue();
        assertThat(seeded).hasSize(1);
        assertThat(seeded.get(0).getAccountCode()).isEqualTo("2200");
        assertThat(seeded.get(0).getAccountName()).contains("Tax Payable");
    }

    @Test
    void skipsSeedWhenAllAccountsExist() {
        for (String code : List.of("1000", "1100", "1200", "2000", "2200", "3000", "4000", "5000", "6000", "7000")) {
            when(accountRepository.findByAccountCode(code)).thenReturn(Optional.of(account(code)));
        }

        new DataSeeder(accountRepository).run();

        verify(accountRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private Account account(String code) {
        return Account.builder().accountCode(code).accountName(code).build();
    }
}
