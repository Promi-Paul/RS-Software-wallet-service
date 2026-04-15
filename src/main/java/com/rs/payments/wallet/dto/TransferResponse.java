package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(description = "Transfer result details")
public class TransferResponse {

    @Schema(description = "Source wallet ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID fromWalletId;

    @Schema(description = "Destination wallet ID", example = "f290f1ee-6c54-4b01-90e6-d701748f0852")
    private UUID toWalletId;

    @Schema(description = "Transferred amount", example = "25.00")
    private BigDecimal amount;

    @Schema(description = "Transfer status", example = "COMPLETED")
    private String status;
}
