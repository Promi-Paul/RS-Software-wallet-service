package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.WithdrawRequest;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "APIs for managing user wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Create a new wallet for a user",
            description = "Creates a new wallet for the specified user ID with a zero balance.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Wallet created successfully",
                            content = @Content(schema = @Schema(implementation = Wallet.class))),
                    @ApiResponse(responseCode = "400", description = "User already has a wallet"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWalletForUser(request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @Operation(
            summary = "Deposit funds into a wallet",
            description = "Deposits the specified amount into the wallet and creates a transaction record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deposit successful",
                            content = @Content(schema = @Schema(implementation = Wallet.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid amount"),
                    @ApiResponse(responseCode = "404", description = "Wallet not found")
            }
    )
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Wallet> deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest request) {
        Wallet wallet = walletService.deposit(id, request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    @Operation(
            summary = "Withdraw funds from a wallet",
            description = "Withdraws the specified amount from the wallet and creates a transaction record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawal successful",
                            content = @Content(schema = @Schema(implementation = Wallet.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid amount or insufficient funds"),
                    @ApiResponse(responseCode = "404", description = "Wallet not found")
            }
    )
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Wallet> withdraw(@PathVariable UUID id, @Valid @RequestBody WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(id, request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    @Operation(
            summary = "Get wallet balance",
            description = "Returns the current balance of the specified wallet.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                            content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Wallet not found")
            }
    )
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {
        BalanceResponse response = new BalanceResponse(id, walletService.getBalance(id));
        return ResponseEntity.ok(response);
    }
}
