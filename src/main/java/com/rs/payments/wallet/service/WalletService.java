package com.rs.payments.wallet.service;

import java.math.BigDecimal;
import java.util.UUID;
import com.rs.payments.wallet.model.Wallet;

public interface WalletService {
    Wallet createWalletForUser(UUID userId);
    Wallet deposit(UUID walletId, BigDecimal amount);
}