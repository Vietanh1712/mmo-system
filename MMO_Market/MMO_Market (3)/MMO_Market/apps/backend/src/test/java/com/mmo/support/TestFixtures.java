package com.mmo.support;

import com.mmo.shared.model.*;
import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {}

    public static User user(String email, String roleName) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setFullName("Test User " + roleName);
        user.setRole("{\"role\": \"" + roleName + "\"}");
        user.setBalanceVnd(100000L);
        user.setIsLocked(false);
        user.setIsDelete(false);
        user.setIsVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static Product product(User seller, String name, Long priceVnd) {
        Product product = new Product();
        product.setId(101L);
        product.setName(name);
        product.setSeller(seller);
        product.setIsDelete(false);
        product.setCreatedAt(LocalDateTime.now());
        return product;
    }

    public static WalletTransaction walletTransaction(User user, String type, Long amountVnd, String status) {
        WalletTransaction tx = new WalletTransaction();
        tx.setId(201L);
        tx.setUser(user);
        tx.setType(type);
        tx.setAmountVnd(amountVnd);
        tx.setStatus(status);
        tx.setBalanceAfter(user.getBalanceVnd() + amountVnd);
        tx.setReferenceCode("REF-" + System.currentTimeMillis());
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }
}
