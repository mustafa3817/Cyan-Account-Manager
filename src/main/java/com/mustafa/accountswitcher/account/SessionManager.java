package com.mustafa.accountswitcher.account;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mustafa.accountswitcher.auth.AuthResult;
import com.mustafa.accountswitcher.auth.MicrosoftAuth;
import com.mustafa.accountswitcher.auth.TokenAuth;
import com.mustafa.accountswitcher.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SessionManager {
    private static Session originalSession = null;
    private static UserApiService originalUserApiService = null;
    private static ProfileKeys originalProfileKeys = null;
    private static SocialInteractionsManager originalSocialManager = null;
    private static boolean originalCaptured = false;

    public static synchronized void captureOriginalSession() {
        if (!originalCaptured) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSession() != null) {
                try {
                    MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
                    originalSession = client.getSession();
                    originalUserApiService = accessor.accountswitcher$getUserApiService();
                    originalProfileKeys = accessor.accountswitcher$getProfileKeys();
                    originalSocialManager = accessor.accountswitcher$getSocialInteractionsManager();
                    originalCaptured = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static synchronized boolean restoreOriginalSession() {
        captureOriginalSession();
        if (originalSession == null) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;

        Runnable action = () -> {
            try {
                MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
                accessor.accountswitcher$setSession(originalSession);
                if (originalUserApiService != null) accessor.accountswitcher$setUserApiService(originalUserApiService);
                if (originalProfileKeys != null) accessor.accountswitcher$setProfileKeys(originalProfileKeys);
                if (originalSocialManager != null) accessor.accountswitcher$setSocialInteractionsManager(originalSocialManager);

                AccountStorage.setActiveUuid(null);
                AccountStorage.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        if (client.isOnThread()) {
            action.run();
        } else {
            client.execute(action);
        }

        return true;
    }

    public static String getOriginalUsername() {
        captureOriginalSession();
        return originalSession != null ? originalSession.getUsername() : "Orijinal";
    }

    public static boolean isOriginalCaptured() {
        return originalCaptured && originalSession != null;
    }

    public static CompletableFuture<Boolean> login(Account account) {
        captureOriginalSession();
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean tokenValid = false;
                if (account.getAccessToken() != null && !account.getAccessToken().isBlank()) {
                    AuthResult check = TokenAuth.validateAccessToken(account.getAccessToken()).join();
                    if (check.isSuccess()) {
                        tokenValid = true;
                    }
                }

                if (!tokenValid) {
                    if (account.hasRefreshToken()) {
                        AuthResult refreshed = MicrosoftAuth.refreshSession(account.getRefreshToken(), null).join();
                        if (refreshed.isSuccess()) {
                            account.setAccessToken(refreshed.getAccessToken());
                            if (refreshed.getRefreshToken() != null) {
                                account.setRefreshToken(refreshed.getRefreshToken());
                            }
                            account.setUsername(refreshed.getUsername());
                            account.setUuid(refreshed.getUuid());
                            account.setValid(true);
                            AccountStorage.save();
                        } else {
                            account.setValid(false);
                            AccountStorage.save();
                            return false;
                        }
                    } else {
                        account.setValid(false);
                        AccountStorage.save();
                        return false;
                    }
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) return false;

                UUID playerUuid = parseUuidSafe(account.getUuid());

                Session newSession = new Session(
                        account.getUsername(),
                        playerUuid,
                        account.getAccessToken(),
                        Optional.empty(),
                        Optional.empty()
                );

                YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(client.getNetworkProxy());
                UserApiService userApiService = authService.createUserApiService(account.getAccessToken());

                ProfileKeys keyPairManager = ProfileKeys.create(
                        userApiService,
                        newSession,
                        client.runDirectory.toPath()
                );

                SocialInteractionsManager socialManager = new SocialInteractionsManager(client, userApiService);

                client.execute(() -> {
                    try {
                        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
                        accessor.accountswitcher$setSession(newSession);
                        accessor.accountswitcher$setUserApiService(userApiService);
                        accessor.accountswitcher$setProfileKeys(keyPairManager);
                        accessor.accountswitcher$setSocialInteractionsManager(socialManager);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                account.setLastUsed(System.currentTimeMillis());
                account.setValid(true);
                AccountStorage.setActiveUuid(account.getUuid());
                AccountStorage.save();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    private static UUID parseUuidSafe(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return UUID.randomUUID();
        try {
            if (uuidStr.contains("-")) {
                return UUID.fromString(uuidStr);
            } else if (uuidStr.length() == 32) {
                return UUID.fromString(uuidStr.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (Exception ignored) {}
        return UUID.nameUUIDFromBytes(uuidStr.getBytes());
    }
}
