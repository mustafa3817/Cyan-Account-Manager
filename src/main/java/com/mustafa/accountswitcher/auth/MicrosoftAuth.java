package com.mustafa.accountswitcher.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MicrosoftAuth {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public static CompletableFuture<DeviceCodeInfo> requestDeviceCode(String clientId) {
        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                      "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AuthConstants.MS_DEVICE_CODE_URL))
                .header("User-Agent", AuthConstants.USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Device code alınamadı: HTTP " + response.statusCode() + " - " + response.body());
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    return new DeviceCodeInfo(
                            json.get("device_code").getAsString(),
                            json.get("user_code").getAsString(),
                            json.get("verification_uri").getAsString(),
                            json.get("expires_in").getAsInt(),
                            json.has("interval") ? json.get("interval").getAsInt() : 5
                    );
                });
    }

    public static CompletableFuture<AuthResult> pollForAuthorization(
            DeviceCodeInfo codeInfo,
            String clientId,
            Consumer<String> statusCallback
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.currentTimeMillis() + (codeInfo.getExpiresIn() * 1000L);
            int intervalMs = Math.max(codeInfo.getInterval(), 3) * 1000;

            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    return AuthResult.failure("İşlem kullanıcı tarafından iptal edildi.");
                }

                String postData = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                        "&device_code=" + URLEncoder.encode(codeInfo.getDeviceCode(), StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.MS_TOKEN_URL))
                        .header("User-Agent", AuthConstants.USER_AGENT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(postData))
                        .build();

                try {
                    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                    if (response.statusCode() == 200) {
                        String msaAccessToken = json.get("access_token").getAsString();
                        String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;

                        if (statusCallback != null) statusCallback.accept("Xbox Live doğrulaması yapılıyor...");
                        return completeMinecraftAuthChain(msaAccessToken, refreshToken, statusCallback).join();
                    }

                    if (json.has("error")) {
                        String error = json.get("error").getAsString();
                        if ("authorization_pending".equals(error)) {
                            continue;
                        } else if ("authorization_declined".equals(error)) {
                            return AuthResult.failure("Kullanıcı giriş onayını reddetti.");
                        } else if ("expired_token".equals(error)) {
                            return AuthResult.failure("Doğrulama kodunun süresi doldu.");
                        } else {
                            return AuthResult.failure("Microsoft hatası: " + error);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return AuthResult.failure("Giriş zaman aşımına uğradı.");
        });
    }

    public static CompletableFuture<AuthResult> refreshSession(String refreshToken, Consumer<String> statusCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String postData = "client_id=" + URLEncoder.encode(AuthConstants.PRIMARY_CLIENT_ID, StandardCharsets.UTF_8) +
                        "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                        "&grant_type=refresh_token" +
                        "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.MS_TOKEN_URL))
                        .header("User-Agent", AuthConstants.USER_AGENT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(postData))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String newMsaAccessToken = json.get("access_token").getAsString();
                    String newRefreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken;

                    return completeMinecraftAuthChain(newMsaAccessToken, newRefreshToken, statusCallback).join();
                }
            } catch (Exception ignored) {}

            try {
                String postData = "client_id=" + URLEncoder.encode(AuthConstants.PRIMARY_CLIENT_ID, StandardCharsets.UTF_8) +
                        "&grant_type=refresh_token" +
                        "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(AuthConstants.REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.MS_LIVE_TOKEN_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(postData))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String newMsaAccessToken = json.get("access_token").getAsString();
                    String newRefreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken;

                    return completeMinecraftAuthChain(newMsaAccessToken, newRefreshToken, statusCallback).join();
                }
            } catch (Exception ignored) {}

            return AuthResult.failure("Refresh token geçersiz veya yenilenemedi. Lütfen yeniden giriş yapın.");
        });
    }

    public static CompletableFuture<AuthResult> completeMinecraftAuthChain(
            String msaAccessToken,
            String refreshToken,
            Consumer<String> statusCallback
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (statusCallback != null) statusCallback.accept("Xbox Live doğrulanıyor...");
                JsonObject xblReq = new JsonObject();
                JsonObject xblProps = new JsonObject();
                xblProps.addProperty("AuthMethod", "RPS");
                xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
                xblProps.addProperty("RpsTicket", "d=" + msaAccessToken);
                xblReq.add("Properties", xblProps);
                xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
                xblReq.addProperty("TokenType", "JWT");

                HttpRequest xblHttpReq = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.XBOX_AUTH_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(xblReq.toString()))
                        .build();

                HttpResponse<String> xblRes = HTTP_CLIENT.send(xblHttpReq, HttpResponse.BodyHandlers.ofString());
                if (xblRes.statusCode() != 200) {
                    return AuthResult.failure("Xbox Live oturumu açılamadı (HTTP " + xblRes.statusCode() + ")");
                }
                JsonObject xblJson = JsonParser.parseString(xblRes.body()).getAsJsonObject();
                String xblToken = xblJson.get("Token").getAsString();

                if (statusCallback != null) statusCallback.accept("XSTS güvenlik sertifikası alınıyor...");
                JsonObject xstsReq = new JsonObject();
                JsonObject xstsProps = new JsonObject();
                xstsProps.addProperty("SandboxId", "RETAIL");
                JsonArray userTokens = new JsonArray();
                userTokens.add(xblToken);
                xstsProps.add("UserTokens", userTokens);
                xstsReq.add("Properties", xstsProps);
                xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                xstsReq.addProperty("TokenType", "JWT");

                HttpRequest xstsHttpReq = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.XSTS_AUTH_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(xstsReq.toString()))
                        .build();

                HttpResponse<String> xstsRes = HTTP_CLIENT.send(xstsHttpReq, HttpResponse.BodyHandlers.ofString());
                if (xstsRes.statusCode() != 200) {
                    JsonObject errJson = JsonParser.parseString(xstsRes.body()).getAsJsonObject();
                    if (errJson.has("XErr")) {
                        long errCode = errJson.get("XErr").getAsLong();
                        if (errCode == 2148916233L) {
                            return AuthResult.failure("Bu Microsoft hesabına bağlı bir Xbox hesabı bulunamadı. Lütfen önce xbox.com üzerinden Xbox profili oluşturun.");
                        } else if (errCode == 2148916235L) {
                            return AuthResult.failure("Bulunduğunuz ülkede/bölgede Xbox Live hizmeti kullanılamıyor.");
                        } else if (errCode == 2148916236L || errCode == 2148916237L) {
                            return AuthResult.failure("Hesabınız için yetişkin/yaş doğrulaması yapılması gerekiyor. Lütfen Microsoft hesabınızdan doğum tarihinizi doğrulayın.");
                        } else if (errCode == 2148916238L) {
                            return AuthResult.failure("Çocuk hesabı: Hesabın bir yetişkin Microsoft aile grubuna eklenmesi ve ebeveyn onayı verilmesi gereklidir.");
                        }
                    }
                    return AuthResult.failure("XSTS yetkilendirmesi başarısız: HTTP " + xstsRes.statusCode());
                }

                JsonObject xstsJson = JsonParser.parseString(xstsRes.body()).getAsJsonObject();
                String xstsToken = xstsJson.get("Token").getAsString();
                String userHash = xstsJson.getAsJsonObject("DisplayClaims")
                        .getAsJsonArray("xui")
                        .get(0).getAsJsonObject()
                        .get("uhs").getAsString();

                if (statusCallback != null) statusCallback.accept("Minecraft Services oturumu açılıyor...");
                JsonObject mcReq = new JsonObject();
                mcReq.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

                HttpRequest mcHttpReq = HttpRequest.newBuilder()
                        .uri(URI.create(AuthConstants.MC_LOGIN_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mcReq.toString()))
                        .build();

                HttpResponse<String> mcRes = HTTP_CLIENT.send(mcHttpReq, HttpResponse.BodyHandlers.ofString());
                if (mcRes.statusCode() != 200) {
                    return AuthResult.failure("Minecraft kimliği doğrulanamadı (HTTP " + mcRes.statusCode() + ")");
                }

                JsonObject mcJson = JsonParser.parseString(mcRes.body()).getAsJsonObject();
                String mcAccessToken = mcJson.get("access_token").getAsString();

                if (statusCallback != null) statusCallback.accept("Oyuncu profili yükleniyor...");
                AuthResult profileResult = TokenAuth.validateAccessToken(mcAccessToken).join();
                if (!profileResult.isSuccess()) {
                    return profileResult;
                }

                return AuthResult.success(
                        profileResult.getUsername(),
                        profileResult.getUuid(),
                        mcAccessToken,
                        refreshToken
                );
            } catch (Exception e) {
                return AuthResult.failure("Yetkilendirme sırasında hata: " + e.getMessage());
            }
        });
    }
}
