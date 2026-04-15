package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.impl.WalletServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTransferTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet fromWallet;
    private Wallet toWallet;
    private UUID fromWalletId;
    private UUID toWalletId;

    @BeforeEach
    void setUp() {
        fromWalletId = UUID.randomUUID();
        toWalletId = UUID.randomUUID();

        User fromUser = new User();
        fromUser.setId(UUID.randomUUID());

        User toUser = new User();
        toUser.setId(UUID.randomUUID());

        fromWallet = new Wallet();
        fromWallet.setId(fromWalletId);
        fromWallet.setUser(fromUser);
        fromWallet.setBalance(new BigDecimal("200.00"));

        toWallet = new Wallet();
        toWallet.setId(toWalletId);
        toWallet.setUser(toUser);
        toWallet.setBalance(new BigDecimal("50.00"));
    }

    // --- Transfer tests ---

    @Test
    void transfer_shouldDebitSenderAndCreditReceiver() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = walletService.transfer(fromWalletId, toWalletId, new BigDecimal("75.00"));

        assertThat(fromWallet.getBalance()).isEqualByComparingTo("125.00");
        assertThat(toWallet.getBalance()).isEqualByComparingTo("125.00");
        assertThat(response.getFromWalletId()).isEqualTo(fromWalletId);
        assertThat(response.getToWalletId()).isEqualTo(toWalletId);
        assertThat(response.getAmount()).isEqualByComparingTo("75.00");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void transfer_shouldCreateTwoTransactionRecords() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.transfer(fromWalletId, toWalletId, new BigDecimal("50.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        List<Transaction> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(t -> {
            assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(t.getWallet().getId()).isEqualTo(fromWalletId);
        });
        assertThat(saved).anySatisfy(t -> {
            assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(t.getWallet().getId()).isEqualTo(toWalletId);
        });
    }

    @Test
    void transfer_shouldThrowBadRequest_whenInsufficientFunds() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));

        assertThatThrownBy(() -> walletService.transfer(fromWalletId, toWalletId, new BigDecimal("999.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowResourceNotFound_whenFromWalletMissing() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.transfer(fromWalletId, toWalletId, new BigDecimal("10.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transfer_shouldThrowResourceNotFound_whenToWalletMissing() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.transfer(fromWalletId, toWalletId, new BigDecimal("10.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transfer_shouldThrowBadRequest_whenAmountIsZero() {
        assertThatThrownBy(() -> walletService.transfer(fromWalletId, toWalletId, BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_shouldThrowBadRequest_whenAmountIsNegative() {
        assertThatThrownBy(() -> walletService.transfer(fromWalletId, toWalletId, new BigDecimal("-10.00")))
                .isInstanceOf(BadRequestException.class);
    }

    // --- Balance inquiry tests ---

    @Test
    void getBalance_shouldReturnCurrentBalance() {
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));

        BigDecimal balance = (BigDecimal) walletService.getBalance(fromWalletId);

        assertThat(balance).isEqualByComparingTo("200.00");
    }

    @Test
    void getBalance_shouldThrowResourceNotFound_whenWalletMissing() {
        UUID unknownId = UUID.randomUUID();
        when(walletRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getBalance(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }
}
