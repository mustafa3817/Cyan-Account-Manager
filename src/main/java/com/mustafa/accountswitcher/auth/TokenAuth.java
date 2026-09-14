package com.mustafa.accountswitcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TokenAuth {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public static CompletableFuture<AuthResult> validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return CompletableFuture.completedFuture(AuthResult.failure("Access token boş olamaz!"));
        }

        return CompletableFuture.supplyAsync(() -> performValidate(accessToken.trim()));
    }

    private static AuthResult performValidate(String token) {
        int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.MC_PROFILE_URL))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String username = json.get("name").getAsString();
                    String rawUuid = json.get("id").getAsString();
                    String formattedUuid = formatUuid(rawUuid);
                    return AuthResult.success(username, formattedUuid, token, null);
                } else if (statusCode == 401) {
                    return AuthResult.failure("Token geçersiz veya süresi dolmuş (401)");
                } else if (statusCode == 404) {
                    return AuthResult.failure("Bu Microsoft hesabına bağlı bir Minecraft oyunu bulunamadı! (Oyun satın alınmamış olabilir)");
                } else if (statusCode == 429) {
                    return AuthResult.failure("İstek limiti aşıldı (Rate limit).");
                } else if (statusCode >= 500 && attempt < maxAttempts) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    continue;
                } else {
                    return AuthResult.failure("Profil sorgusu başarısız (HTTP " + statusCode + ")");
                }
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    continue;
                }
                return AuthResult.failure("Ağ hatası: " + e.getMessage());
            }
        }
        return AuthResult.failure("Profil sorgulanamadı.");
    }

    public static String formatUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.length() != 32) return rawUuid;
        return String.format("%s-%s-%s-%s-%s",
                rawUuid.substring(0, 8),
                rawUuid.substring(8, 12),
                rawUuid.substring(12, 16),
                rawUuid.substring(16, 20),
                rawUuid.substring(20, 32)
        );
    }
}
