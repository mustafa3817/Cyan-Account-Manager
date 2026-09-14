package com.mustafa.accountswitcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LocalhostAuthServer implements Closeable {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public static final String REDIRECT_PATH = "/in_game_account_switcher_long_enough_uri_to_prevent_accidental_leaks_on_screensharing_even_if_you_have_like_extremely_big_screen_though_it_might_not_mork_but_we_will_try_it_anyway_to_prevent_funny_things_from_happening_or_something";

    private static final java.util.concurrent.ScheduledExecutorService SCHEDULER = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LocalhostAuth-Timeout");
        t.setDaemon(true);
        return t;
    });

    private HttpServer server;
    private int port;
    private String authUrl;
    private final CompletableFuture<AuthResult> resultFuture = new CompletableFuture<>();
    private final Consumer<String> statusCallback;
    private java.util.concurrent.ScheduledFuture<?> timeoutTask;

    public LocalhostAuthServer(Consumer<String> statusCallback) {
        this.statusCallback = statusCallback;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    private static HttpServer createServer() throws IOException {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            if (s.getAddress().getPort() > 0) {
                return s;
            }
            s.stop(0);
        } catch (Exception ignored) {}

        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            int freePort = ss.getLocalPort();
            return HttpServer.create(new InetSocketAddress("127.0.0.1", freePort), 0);
        } catch (Exception ignored) {}

        for (int p = 38490; p <= 38550; p++) {
            try {
                return HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
            } catch (Exception ignored) {}
        }

        throw new IOException("Kullanılabilir boş bir yerel port bulunamadı.");
    }

    public CompletableFuture<AuthResult> start() {
        try {
            this.server = createServer();
            this.port = this.server.getAddress().getPort();

            this.server.createContext(REDIRECT_PATH, this::handleRequest);
            this.server.createContext("/", this::handleRequest);
            this.server.setExecutor(null);
            this.server.start();

            String redirectUri = "http://localhost:" + port + REDIRECT_PATH;
            this.authUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=" + URLEncoder.encode(AuthConstants.PRIMARY_CLIENT_ID, StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8) +
                    "&prompt=select_account";

            this.timeoutTask = SCHEDULER.schedule(() -> {
                if (!resultFuture.isDone()) {
                    resultFuture.complete(AuthResult.failure("Giriş işlemi zaman aşımına uğradı (Port: " + port + ")."));
                    close();
                }
            }, 3, java.util.concurrent.TimeUnit.MINUTES);

            if (statusCallback != null) statusCallback.accept("Tarayıcı açıldı (Port: " + port + "). Lütfen Microsoft hesabınızı onaylayın...");

            Util.getOperatingSystem().open(URI.create(authUrl));

        } catch (Exception e) {
            resultFuture.complete(AuthResult.failure("Localhost sunucusu başlatılamadı (Port: " + port + "): " + e.getMessage()));
            close();
        }

        return resultFuture;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        String query = requestUri.getQuery();
        String code = null;
        String error = null;

        if (query != null) {
            for (String param : query.split("&")) {
                int eq = param.indexOf('=');
                if (eq != -1) {
                    String key = param.substring(0, eq);
                    String val = java.net.URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8);
                    if ("code".equals(key)) code = val;
                    else if ("error".equals(key)) error = val;
                }
            }
        }

        String responseHtml;
        String logoBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAABFUlEQVR4nO3OMQrCQBCF4RVBBSs7G0sv4Dm8hCDYehB7Eey9gFiJp/AGNnaCYGEhuiu8uBkz2Zl1IQj5izWaxPc1TMXVgJ8As/P9aT/Mqt+O/p+oFzFMi4GoXuCGaRqI6EHpME0CKX2ADo+7TXt+2t4e9gxXBim8QYdRLAAVQXI/cMOp8yHZhau1O74Bk9HQnunbLJb2NOa6mGe72YULAJQKgmEkBqBYCB1GagCSQrhhJAJMBz2zPl3s1XccJDSMxAAUgkiHkRrg4hCdw96euv4DgACpDBCqBtSApACXFqEF+OOu3Bc/KUQKoMOo8Ee/ECQE4IZR6U0/DsIBQsNI9JAfhVCAdBipHvYDBADtMIp6KWWVA14YXq8hvmDptgAAAABJRU5ErkJggg==";

        if (code != null) {
            responseHtml = "<!DOCTYPE html><html lang='tr'><head><meta charset='utf-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<title>Cyan Acc Manager - Giriş Başarılı</title>" +
                    "<style>" +
                    "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                    "body { background: #07090e; color: #f3f4f6; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; overflow: hidden; position: relative; }" +
                    ".glow { position: absolute; width: 500px; height: 500px; background: radial-gradient(circle, rgba(6, 182, 212, 0.15) 0%, rgba(6, 182, 212, 0) 70%); top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 0; pointer-events: none; }" +
                    ".card { position: relative; z-index: 1; background: rgba(13, 17, 26, 0.85); border: 1px solid rgba(6, 182, 212, 0.35); box-shadow: 0 0 50px rgba(6, 182, 212, 0.18); backdrop-filter: blur(16px); border-radius: 24px; padding: 48px 40px; text-align: center; max-width: 440px; width: 90%; }" +
                    ".logo-wrap { width: 80px; height: 80px; margin: 0 auto 20px; background: rgba(6, 182, 212, 0.1); border: 2px solid #06b6d4; border-radius: 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 25px rgba(6, 182, 212, 0.4); animation: pulse 3s infinite ease-in-out; }" +
                    ".logo-wrap img { width: 48px; height: 48px; image-rendering: pixelated; }" +
                    "@keyframes pulse { 0%, 100% { transform: scale(1); box-shadow: 0 0 25px rgba(6, 182, 212, 0.4); } 50% { transform: scale(1.04); box-shadow: 0 0 35px rgba(34, 211, 238, 0.6); } }" +
                    ".brand { font-size: 13px; font-weight: 800; letter-spacing: 3px; text-transform: uppercase; color: #22d3ee; margin-bottom: 6px; text-shadow: 0 0 10px rgba(34, 211, 238, 0.5); }" +
                    ".title { font-size: 28px; font-weight: 800; color: #ffffff; margin-bottom: 12px; }" +
                    ".desc { color: #94a3b8; font-size: 15px; line-height: 1.5; margin-bottom: 28px; }" +
                    ".status-badge { display: inline-flex; align-items: center; gap: 8px; background: rgba(34, 197, 94, 0.12); border: 1px solid rgba(34, 197, 94, 0.3); color: #4ade80; padding: 8px 18px; border-radius: 999px; font-size: 14px; font-weight: 600; margin-bottom: 24px; }" +
                    ".progress-bar-bg { width: 100%; height: 6px; background: rgba(255, 255, 255, 0.08); border-radius: 999px; overflow: hidden; margin-top: 10px; }" +
                    ".progress-bar { height: 100%; width: 100%; background: linear-gradient(90deg, #06b6d4, #22d3ee); animation: progress 2.5s linear forwards; transform-origin: left; }" +
                    "@keyframes progress { from { transform: scaleX(1); } to { transform: scaleX(0); } }" +
                    ".btn-group { display: flex; gap: 12px; justify-content: center; margin-top: 24px; flex-wrap: wrap; }" +
                    ".btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; padding: 11px 22px; border-radius: 12px; font-size: 14px; font-weight: 700; text-decoration: none; cursor: pointer; transition: all 0.2s; border: none; font-family: inherit; }" +
                    ".btn-close { background: rgba(6, 182, 212, 0.15); border: 1px solid rgba(6, 182, 212, 0.4); color: #22d3ee; }" +
                    ".btn-close:hover { background: #06b6d4; color: #000; box-shadow: 0 0 20px rgba(6, 182, 212, 0.5); transform: translateY(-1px); }" +
                    ".btn-discord { background: #5865F2; color: #ffffff; box-shadow: 0 0 20px rgba(88, 101, 242, 0.35); }" +
                    ".btn-discord:hover { background: #4752c4; box-shadow: 0 0 25px rgba(88, 101, 242, 0.6); transform: translateY(-1px); }" +
                    ".btn-discord svg { width: 18px; height: 18px; fill: currentColor; }" +
                    "</style></head><body>" +
                    "<div class='glow'></div>" +
                    "<div class='card'>" +
                    "<div class='logo-wrap'><img src='" + logoBase64 + "' alt='CyanAFK'></div>" +
                    "<div class='brand'>Cyan Acc Manager</div>" +
                    "<div class='title'>Giriş Başarılı!</div>" +
                    "<div class='status-badge'>✓ Minecraft Hesabı Bağlandı</div>" +
                    "<div class='desc'>Hesabınız başarıyla entegre edildi. Bu pencereyi kapatıp oyuna dönebilirsiniz.</div>" +
                    "<div class='progress-bar-bg'><div class='progress-bar'></div></div>" +
                    "<div class='btn-group'>" +
                    "<button class='btn btn-close' onclick='closeTab()'>Sekmeyi Kapat</button>" +
                    "<a class='btn btn-discord' href='https://discord.com/invite/YzujvvZQjE' target='_blank'>" +
                    "<svg viewBox='0 0 24 24'><path d='M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.058a.082.082 0 00.031.056 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.291.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.061 0a.074.074 0 01.079.01c.12.098.246.198.373.292a.077.077 0 01-.007.128c-.598.343-1.22.645-1.873.892a.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.332-.947 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.332-.946 2.418-2.157 2.418z'/></svg>" +
                    "Discord" +
                    "</a>" +
                    "</div>" +
                    "</div>" +
                    "<script>" +
                    "function closeTab() {" +
                    "  window.open('', '_self', '');" +
                    "  window.close();" +
                    "  setTimeout(function() {" +
                    "    window.location.href = 'about:blank';" +
                    "  }, 150);" +
                    "}" +
                    "setTimeout(function(){ closeTab(); }, 3500);" +
                    "</script>" +
                    "</body></html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            if (statusCallback != null) statusCallback.accept("Kod alındı! Token'lar çekiliyor ve oyuna bağlanılıyor...");

            String finalCode = code;
            String redirectUri = "http://localhost:" + port + REDIRECT_PATH;
            CompletableFuture.runAsync(() -> exchangeCodeForTokens(finalCode, redirectUri));

        } else {
            responseHtml = "<!DOCTYPE html><html lang='tr'><head><meta charset='utf-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<title>Cyan Acc Manager - Hata</title>" +
                    "<style>" +
                    "body { background: #07090e; color: #f3f4f6; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; }" +
                    ".card { background: rgba(13, 17, 26, 0.85); border: 1px solid rgba(239, 68, 68, 0.35); box-shadow: 0 0 40px rgba(239, 68, 68, 0.15); border-radius: 24px; padding: 40px; text-align: center; max-width: 440px; width: 90%; }" +
                    ".logo-wrap { width: 70px; height: 70px; margin: 0 auto 16px; background: rgba(239, 68, 68, 0.1); border: 2px solid #ef4444; border-radius: 18px; display: flex; align-items: center; justify-content: center; }" +
                    ".logo-wrap img { width: 40px; height: 40px; }" +
                    ".title { font-size: 24px; font-weight: 800; color: #f87171; margin-bottom: 12px; }" +
                    ".desc { color: #94a3b8; font-size: 14px; line-height: 1.5; }" +
                    "</style></head><body>" +
                    "<div class='card'>" +
                    "<div class='logo-wrap'><img src='" + logoBase64 + "' alt='CyanAFK'></div>" +
                    "<div class='title'>Giriş Yapılamadı</div>" +
                    "<div class='desc'>" + (error != null ? error : "Yetkilendirme kodu bulunamadı veya işlem iptal edildi.") + "</div>" +
                    "</div></body></html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            resultFuture.complete(AuthResult.failure("Yetkilendirme reddedildi veya hata oluştu: " + error));
            close();
        }
    }

    private void exchangeCodeForTokens(String code, String redirectUri) {
        try {
            if (statusCallback != null) statusCallback.accept("Microsoft token takası yapılıyor...");

            String postData = "client_id=" + URLEncoder.encode(AuthConstants.PRIMARY_CLIENT_ID, StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AuthConstants.MS_LIVE_TOKEN_URL))
                    .header("User-Agent", AuthConstants.USER_AGENT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(postData))
                    .build();

            System.out.println("[AccountSwitcher] Kod takas istegi gonderiliyor. redirect_uri=" + redirectUri);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[AccountSwitcher] Microsoft live token yaniti: HTTP " + response.statusCode() + " -> " + response.body());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (response.statusCode() == 200) {
                String msaAccessToken = json.get("access_token").getAsString();
                String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;

                AuthResult finalResult = MicrosoftAuth.completeMinecraftAuthChain(msaAccessToken, refreshToken, statusCallback).join();
                System.out.println("[AccountSwitcher] Minecraft auth sonucu: " + finalResult.isSuccess() + " (" + finalResult.getUsername() + ")");
                
                if (finalResult.isSuccess()) {
                    if (statusCallback != null) statusCallback.accept("Giriş Başarılı: " + finalResult.getUsername() + "! Oyuna bağlanılıyor...");
                    com.mustafa.accountswitcher.account.Account account = new com.mustafa.accountswitcher.account.Account(
                            finalResult.getUsername(),
                            finalResult.getUuid(),
                            finalResult.getAccessToken(),
                            finalResult.getRefreshToken(),
                            com.mustafa.accountswitcher.account.AccountType.MICROSOFT
                    );
                    com.mustafa.accountswitcher.account.AccountStorage.addAccount(account);
                    com.mustafa.accountswitcher.account.SessionManager.login(account);
                    System.out.println("[AccountSwitcher] Hesap basariyla kaydedildi ve oturum acildi: " + account.getUsername());

                    net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.execute(() -> {
                            mc.setScreen(new com.mustafa.accountswitcher.gui.AccountManagerScreen(null));
                        });
                    }
                }
                
                resultFuture.complete(finalResult);
            } else {
                String errMsg = json.has("error_description") ? json.get("error_description").getAsString() : "HTTP " + response.statusCode();
                System.err.println("[AccountSwitcher] Microsoft token alinamadi: " + errMsg);
                resultFuture.complete(AuthResult.failure("Microsoft token alınamadı: " + errMsg));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultFuture.complete(AuthResult.failure("Token takası hatası: " + e.getMessage()));
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {}
            server = null;
        }
    }
}
