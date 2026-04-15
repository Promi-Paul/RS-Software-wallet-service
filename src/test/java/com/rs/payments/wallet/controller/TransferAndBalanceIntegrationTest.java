package com.rs.payments.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Transfer and Balance Inquiry features.
 * Requires a running database (Testcontainers or H2 in-memory).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransferAndBalanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        User userA = new User();
        userA.setUsername("alice");
        userA.setEmail("alice@example.com");
        userA = userRepository.save(userA);

        User userB = new User();
        userB.setUsername("bob");
        userB.setEmail("bob@example.com");
        userB = userRepository.save(userB);

        walletA = new Wallet();
        walletA.setUser(userA);
        walletA.setBalance(new BigDecimal("500.00"));
        walletA = walletRepository.save(walletA);

        walletB = new Wallet();
        walletB.setUser(userB);
        walletB.setBalance(new BigDecimal("100.00"));
        walletB = walletRepository.save(walletB);
    }

    // ── Transfer tests ──────────────────────────────────────────────────────────

    @Test
    void transfer_shouldReturn200AndTransferDetails_onSuccess() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("150.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromWalletId", is(walletA.getId().toString())))
                .andExpect(jsonPath("$.toWalletId", is(walletB.getId().toString())))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void transfer_shouldUpdateBothBalances() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("200.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Wallet updatedA = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet updatedB = walletRepository.findById(walletB.getId()).orElseThrow();

        assertThat(updatedA.getBalance()).isEqualByComparingTo("300.00");
        assertThat(updatedB.getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void transfer_shouldCreateTwoTransactionRecords() throws Exception {
        long countBefore = transactionRepository.count();

        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(transactionRepository.count()).isEqualTo(countBefore + 2);
    }

    @Test
    void transfer_shouldReturn400_whenInsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("9999.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_shouldNotChangeBalances_whenInsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("9999.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Wallet unchangedA = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet unchangedB = walletRepository.findById(walletB.getId()).orElseThrow();

        assertThat(unchangedA.getBalance()).isEqualByComparingTo("500.00");
        assertThat(unchangedB.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_shouldReturn404_whenFromWalletNotFound() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(UUID.randomUUID());
        request.setToWalletId(walletB.getId());
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_shouldReturn404_whenToWalletNotFound() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(UUID.randomUUID());
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_shouldReturn400_whenAmountIsZero() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletA.getId());
        request.setToWalletId(walletB.getId());
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_shouldReturn400_whenMissingFields() throws Exception {
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Balance inquiry tests ────────────────────────────────────────────────────

    @Test
    void getBalance_shouldReturn200AndBalance() throws Exception {
        mockMvc.perform(get("/wallets/{id}/balance", walletA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is(walletA.getId().toString())))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    void getBalance_shouldReturn404_whenWalletNotFound() throws Exception {
        mockMvc.perform(get("/wallets/{id}/balance", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_shouldReflectBalanceAfterDeposit() throws Exception {
        // Deposit 100 first
        mockMvc.perform(post("/wallets/{id}/deposit", walletA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wallets/{id}/balance", walletA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(600.00)));
    }
}
