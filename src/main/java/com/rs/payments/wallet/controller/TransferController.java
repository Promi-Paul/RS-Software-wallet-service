package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@Tag(name = "Transfers", description = "APIs for peer-to-peer wallet transfers")
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Transfer funds between wallets",
            description = "Transfers funds atomically from one wallet to another.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Transfer successful",
                            content = @Content(schema = @Schema(implementation = TransferResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid amount or insufficient funds"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = walletService.transfer(request.getFromWalletId(), request.getToWalletId(), request.getAmount());
        return ResponseEntity.ok(response);
    }
}
