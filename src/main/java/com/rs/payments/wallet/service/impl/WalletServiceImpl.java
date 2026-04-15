package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet createWalletForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getWallet() != null) {
            throw new BadRequestException("User already has a wallet");
        }

        Wallet wallet = new Wallet();
        wallet.setBalance(INITIAL_BALANCE);
        wallet.setUser(user);
        user.setWallet(wallet);

        user = userRepository.save(user);
        return user.getWallet();
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWallet(walletId);
        validateAmount(amount);

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);
        recordTransaction(wallet, amount, TransactionType.DEPOSIT, "Deposit");

        return wallet;
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWallet(walletId);
        validateAmount(amount);
        validateSufficientBalance(wallet, amount);

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet = walletRepository.save(wallet);
        recordTransaction(wallet, amount, TransactionType.WITHDRAWAL, "Withdrawal");

        return wallet;
    }

    @Override
    @Transactional
    public TransferResponse transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        validateAmount(amount);

        Wallet fromWallet = findWallet(fromWalletId);
        Wallet toWallet = findWallet(toWalletId);

        validateSufficientBalance(fromWallet, amount);

        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        recordTransaction(fromWallet, amount, TransactionType.TRANSFER_OUT, "Transfer out to " + toWalletId);
        recordTransaction(toWallet, amount, TransactionType.TRANSFER_IN, "Transfer in from " + fromWalletId);

        return buildTransferResponse(fromWalletId, toWalletId, amount);
    }

    @Override
    public BigDecimal getBalance(UUID walletId) {
        return findWallet(walletId).getBalance();
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private Wallet findWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }
    }

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }
    }

    private void recordTransaction(Wallet wallet, BigDecimal amount, TransactionType type, String description) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    private TransferResponse buildTransferResponse(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        TransferResponse response = new TransferResponse();
        response.setFromWalletId(fromWalletId);
        response.setToWalletId(toWalletId);
        response.setAmount(amount);
        response.setStatus("COMPLETED");
        return response;
    }
}
