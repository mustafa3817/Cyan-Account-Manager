package com.mustafa.accountswitcher.gui;

import com.mustafa.accountswitcher.account.Account;
import com.mustafa.accountswitcher.account.AccountStorage;
import com.mustafa.accountswitcher.account.AccountType;
import com.mustafa.accountswitcher.account.SessionManager;
import com.mustafa.accountswitcher.auth.LocalhostAuthServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

public class LocalhostLoginScreen extends Screen {
    private final Screen parent;
    private LocalhostAuthServer authServer;
    private String statusMessage = "Yerel sunucu başlatılıyor...";
    private boolean isFinished = false;
    private ButtonWidget copyLinkButton;
    private int tickCount = 0;

    public LocalhostLoginScreen(Screen parent) {
        super(Text.of("Microsoft Tarayıcı Girişi (Localhost)"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.copyLinkButton = ButtonWidget.builder(Text.of("Linki Kopyala / Tarayıcıda Aç"), button -> {
            if (authServer != null && authServer.getAuthUrl() != null && this.client != null) {
                this.client.keyboard.setClipboard(authServer.getAuthUrl());
                try {
                    Util.getOperatingSystem().open(URI.create(authServer.getAuthUrl()));
                } catch (Exception ignored) {}
            }
        }).dimensions(centerX - 110, centerY + 30, 220, 20).build();
        this.copyLinkButton.active = false;
        this.addDrawableChild(this.copyLinkButton);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .dimensions(centerX - 50, centerY + 58, 100, 20)
                .build());

        if (this.authServer == null && !isFinished) {
            startServer();
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
        if (authServer != null && authServer.getAuthUrl() != null && copyLinkButton != null && !copyLinkButton.active) {
            copyLinkButton.active = true;
        }
    }

    private void startServer() {
        this.authServer = new LocalhostAuthServer(status -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> this.statusMessage = status);
            }
        });

        this.authServer.start().thenAccept(result -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    this.isFinished = true;
                    if (result.isSuccess()) {
                        this.statusMessage = "Giriş Başarılı: " + result.getUsername() + " (Oyuna bağlanılıyor...)";
                        Account account = new Account(
                                result.getUsername(),
                                result.getUuid(),
                                result.getAccessToken(),
                                result.getRefreshToken(),
                                AccountType.MICROSOFT
                        );
                        AccountStorage.addAccount(account);
                        SessionManager.login(account);
                        close();
                    } else {
                        this.statusMessage = "Giriş Başarısız: " + result.getErrorMessage();
                    }
                });
            }
        }).exceptionally(throwable -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    this.isFinished = true;
                    this.statusMessage = "Hata: " + throwable.getMessage();
                });
            }
            return null;
        });
    }

    @Override
    public void close() {
        if (authServer != null) {
            authServer.close();
            authServer = null;
        }
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardWidth = 320;
        int cardHeight = 72;
        int cardX = centerX - cardWidth / 2;
        int cardY = centerY - 55;
        context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0x88000000);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 80, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Tarayıcınızda açılan sekmeden hesabınızı seçin."), centerX, centerY - 45, 0xFFCCCCCC);

        String dots = switch ((tickCount / 12) % 4) {
            case 1 -> ".";
            case 2 -> "..";
            case 3 -> "...";
            default -> "";
        };

        int statusColor = this.statusMessage.contains("Başarısız") || this.statusMessage.contains("Hata") ? 0xFFFF5555 : 0xFF55FF55;
        String displayStatus = this.statusMessage;
        if (!isFinished && !displayStatus.contains("Hata") && !displayStatus.contains("Başarısız")) {
            displayStatus += dots;
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.of(displayStatus), centerX, centerY - 20, statusColor);
    }
}
