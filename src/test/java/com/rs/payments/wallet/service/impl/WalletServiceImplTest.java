package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    @DisplayName("Should create wallet for existing user")
    void shouldCreateWalletForExistingUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // The service saves the user, which cascades to wallet. 
        // We mock save to return the user.
        when(userRepository.save(user)).thenReturn(user);

        // When
        Wallet result = walletService.createWalletForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());

        // Verify interactions
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> walletService.createWalletForUser(userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should withdraw funds successfully")
    void shouldWithdrawFundsSuccessfully() {
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("30.00");
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(new BigDecimal("100.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.withdraw(walletId, withdrawAmount);

        assertNotNull(result);
        assertEquals(new BigDecimal("70.00"), result.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw resource not found when wallet does not exist")
    void shouldThrowResourceNotFoundWhenWalletNotFoundForWithdraw() {
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("30.00");

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.withdraw(walletId, withdrawAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw bad request for invalid withdraw amount")
    void shouldThrowBadRequestForInvalidWithdrawAmount() {
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("-10.00");
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(new BigDecimal("100.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(BadRequestException.class, () -> walletService.withdraw(walletId, withdrawAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw bad request when withdraw amount exceeds balance")
    void shouldThrowBadRequestWhenWithdrawAmountExceedsBalance() {
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(new BigDecimal("100.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(BadRequestException.class, () -> walletService.withdraw(walletId, withdrawAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
