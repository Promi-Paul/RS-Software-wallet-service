package com.rs.payments.wallet.controller;

import java.math.BigDecimal;
import java.util.UUID;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    @Test
    @DisplayName("Should create wallet with 201 Created status and zero balance")
    void shouldCreateWalletWithCreatedStatus() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.ZERO);

        when(walletService.createWalletForUser(userId)).thenReturn(wallet);

        // When
        ResponseEntity<Wallet> response = walletController.createWallet(request);

        // Then
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        assertEquals(wallet, response.getBody());
        assertEquals(BigDecimal.ZERO, response.getBody().getBalance());
        verify(walletService, times(1)).createWalletForUser(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);

        when(walletService.createWalletForUser(userId))
            .thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> walletController.createWallet(request));
        verify(walletService, times(1)).createWalletForUser(userId);
    }

    @Test
    @DisplayName("Should throw exception when user already has wallet")
    void shouldThrowExceptionWhenUserAlreadyHasWallet() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);

        when(walletService.createWalletForUser(userId))
            .thenThrow(new BadRequestException("User already has a wallet"));

        // When & Then
        assertThrows(BadRequestException.class, () -> walletController.createWallet(request));
        verify(walletService, times(1)).createWalletForUser(userId);
    }
}
