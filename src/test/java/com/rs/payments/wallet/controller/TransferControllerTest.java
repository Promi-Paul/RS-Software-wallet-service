package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TransferController transferController;

    @Test
    @DisplayName("Should transfer funds successfully")
    void shouldTransferFundsSuccessfully() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(amount);

        TransferResponse response = new TransferResponse();
        response.setFromWalletId(fromWalletId);
        response.setToWalletId(toWalletId);
        response.setAmount(amount);
        response.setStatus("COMPLETED");

        when(walletService.transfer(fromWalletId, toWalletId, amount)).thenReturn(response);

        ResponseEntity<TransferResponse> result = transferController.transfer(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(walletService, times(1)).transfer(fromWalletId, toWalletId, amount);
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds")
    void shouldThrowExceptionWhenInsufficientFunds() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(amount);

        when(walletService.transfer(fromWalletId, toWalletId, amount))
                .thenThrow(new BadRequestException("Insufficient funds"));

        assertThrows(BadRequestException.class, () -> transferController.transfer(request));
        verify(walletService, times(1)).transfer(fromWalletId, toWalletId, amount);
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(amount);

        when(walletService.transfer(fromWalletId, toWalletId, amount))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        assertThrows(ResourceNotFoundException.class, () -> transferController.transfer(request));
        verify(walletService, times(1)).transfer(fromWalletId, toWalletId, amount);
    }
}
