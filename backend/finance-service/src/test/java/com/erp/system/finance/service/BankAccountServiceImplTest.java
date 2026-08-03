package com.erp.system.finance.service;

import com.erp.system.finance.dto.BankAccountRequest;
import com.erp.system.finance.dto.BankAccountResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.BankAccount;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.BankAccountRepository;
import com.erp.system.finance.service.impl.BankAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountService service;

    @BeforeEach
    void setUp() {
        service = new BankAccountServiceImpl(accountRepository, bankAccountRepository);
    }

    private BankAccount bank(long id, String name, String number, String accountCode) {
        Account account = Account.builder()
                .id(id + 10L)
                .accountCode(accountCode)
                .accountName("Banque " + name)
                .accountType("ASSET")
                .balance(BigDecimal.ZERO)
                .build();
        return BankAccount.builder().id(id).name(name).accountNumber(number).account(account).build();
    }

    @Test
    void createBankAccountCreatesBankWithLinkedChartAccount() {
        when(accountRepository.findMaxCashCode()).thenReturn(null);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BankAccountResponse response = service.create(
                BankAccountRequest.builder().name("BNP").accountNumber("FR30001").build());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("BNP");
        assertThat(response.getAccountNumber()).isEqualTo("FR30001");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account chartAccount = accountCaptor.getValue();
        assertThat(chartAccount.getAccountCode()).isEqualTo("1010");
        assertThat(chartAccount.getAccountName()).isEqualTo("BNP");
        assertThat(chartAccount.getAccountType()).isEqualTo("ASSET");
        assertThat(chartAccount.getBalance()).isEqualByComparingTo("0.00");

        ArgumentCaptor<BankAccount> bankCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(bankCaptor.capture());
        assertThat(bankCaptor.getValue().getAccount()).isSameAs(chartAccount);
        assertThat(response.getAccountCode()).isEqualTo("1010");
        assertThat(response.getAccountId()).isEqualTo(chartAccount.getId());
    }

    @Test
    void createBankAccountAssignsSequentialChartCodes() {
        when(accountRepository.findMaxCashCode()).thenReturn("1010");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        BankAccountResponse response = service.create(
                BankAccountRequest.builder().name("CIC").accountNumber("FR30002").build());

        assertThat(response.getAccountCode()).isEqualTo("1020");
    }

    @Test
    void createBankAccountRejectsDuplicateAccountNumber() {
        when(bankAccountRepository.existsByAccountNumber("FR30001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                BankAccountRequest.builder().name("BNP").accountNumber("FR30001").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createBankAccountRejectsBlankFields() {
        assertThatThrownBy(() -> service.create(
                BankAccountRequest.builder().name(" ").accountNumber("FR30001").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> service.create(
                BankAccountRequest.builder().name("BNP").accountNumber(" ").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account number");
    }

    @Test
    void getAllReturnsAllBanks() {
        when(bankAccountRepository.findAll()).thenReturn(List.of(
                bank(1L, "BNP", "FR30001", "1010"),
                bank(2L, "CIC", "FR30002", "1020")));

        List<BankAccountResponse> response = service.getAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("BNP");
        assertThat(response.get(1).getAccountCode()).isEqualTo("1020");
    }

    @Test
    void getByIdReturnsBank() {
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));

        BankAccountResponse response = service.getById(1L);

        assertThat(response.getName()).isEqualTo("BNP");
        assertThat(response.getAccountCode()).isEqualTo("1010");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(bankAccountRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bank account not found: 9");
    }
}
