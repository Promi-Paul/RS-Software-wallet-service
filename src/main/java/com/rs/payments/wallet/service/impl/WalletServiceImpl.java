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

    public WalletServiceImpl(UserRepository userRepository, WalletRepository walletRepository, TransactionRepository transactionRepository) {
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

        user = userRepository.save(user); // Cascade saves wallet
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

    private Wallet findWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
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

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }
    }

    @Override
    public TransferResponse transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        throw new UnsupportedOperationException("Transfer functionality not implemented yet");
    }
