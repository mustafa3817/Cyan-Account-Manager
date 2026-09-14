package com.mustafa.accountswitcher.account;

import com.mustafa.accountswitcher.auth.TokenAuth;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class AccountHealthChecker {
    private static final Semaphore RATE_LIMIT_SEMAPHORE = new Semaphore(2);
    private static final ExecutorService WORKER_POOL = Executors.newFixedThreadPool(3);

    public static CompletableFuture<AccountHealth> checkAccount(Account account) {
        if (account.getAccessToken() == null || account.getAccessToken().isBlank()) {
            AccountHealth health = account.hasRefreshToken() ? AccountHealth.REFRESHABLE : AccountHealth.EXPIRED;
            account.setHealth(health);
            return CompletableFuture.completedFuture(health);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                RATE_LIMIT_SEMAPHORE.acquire();
                Thread.sleep(120);

                return TokenAuth.validateAccessToken(account.getAccessToken()).thenApply(result -> {
                    AccountHealth health;
                    if (result.isSuccess()) {
                        health = AccountHealth.VALID;
                        account.setUsername(result.getUsername());
                    } else if (account.hasRefreshToken()) {
                        health = AccountHealth.REFRESHABLE;
                    } else {
                        health = AccountHealth.EXPIRED;
                    }
                    account.setHealth(health);
                    return health;
                }).join();
            } catch (Exception e) {
                return AccountHealth.UNKNOWN;
            } finally {
                RATE_LIMIT_SEMAPHORE.release();
            }
        }, WORKER_POOL);
    }

    public static CompletableFuture<Void> checkAll(List<Account> accounts, Runnable onUpdate) {
        if (accounts.isEmpty()) return CompletableFuture.completedFuture(null);

        CompletableFuture<?>[] futures = accounts.stream()
                .map(acc -> checkAccount(acc).thenRun(() -> {
                    if (onUpdate != null) onUpdate.run();
                }))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }
}
