package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(description = "Request to transfer funds between wallets")
public class TransferRequest {

    @NotNull
    @Schema(description = "Source wallet ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID fromWalletId;

    @NotNull
    @Schema(description = "Destination wallet ID", example = "f290f1ee-6c54-4b01-90e6-d701748f0852")
    private UUID toWalletId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Amount to transfer", example = "25.00", minimum = "0.01")
    private BigDecimal amount;
}
