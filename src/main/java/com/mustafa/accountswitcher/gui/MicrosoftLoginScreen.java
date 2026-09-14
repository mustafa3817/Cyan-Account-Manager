package com.mustafa.accountswitcher.gui;

import com.mustafa.accountswitcher.account.Account;
import com.mustafa.accountswitcher.account.AccountStorage;
import com.mustafa.accountswitcher.account.AccountType;
import com.mustafa.accountswitcher.account.SessionManager;
import com.mustafa.accountswitcher.auth.AuthConstants;
import com.mustafa.accountswitcher.auth.DeviceCodeInfo;
import com.mustafa.accountswitcher.auth.MicrosoftAuth;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class MicrosoftLoginScreen extends Screen {
    private final Screen parent;
    private DeviceCodeInfo codeInfo;
    private String directUrl = null;
    private String statusMessage = "Microsoft giriş kodu talep ediliyor...";
    private ButtonWidget openBrowserButton;
    private CompletableFuture<?> activeTask;

    public MicrosoftLoginScreen(Screen parent) {
        super(Text.translatable("accountswitcher.ms.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.openBrowserButton = ButtonWidget.builder(Text.translatable("accountswitcher.ms.copy_and_open"), button -> {
            openDirectLink();
        }).dimensions(centerX - 110, centerY + 30, 220, 20).build();
        this.openBrowserButton.active = (directUrl != null);
        this.addDrawableChild(this.openBrowserButton);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .dimensions(centerX - 50, centerY + 60, 100, 20)
                .build());

        if (codeInfo == null && activeTask == null) {
            startDeviceAuthFlow();
        }
    }

    private void openDirectLink() {
        if (directUrl != null && this.client != null) {
            this.client.keyboard.setClipboard(directUrl);
            try {
                Util.getOperatingSystem().open(URI.create(directUrl));
            } catch (Exception ignored) {}
        }
    }

    private void startDeviceAuthFlow() {
        this.statusMessage = "Microsoft sunucusundan kod talep ediliyor...";
        activeTask = MicrosoftAuth.requestDeviceCode(AuthConstants.PRIMARY_CLIENT_ID)
                .thenAccept(info -> {
                    this.codeInfo = info;
                    this.directUrl = "https://www.microsoft.com/link?otc=" + info.getUserCode();

                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.statusMessage = "Tarayıcı açılıyor, lütfen oturumu onaylayın...";
                            if (this.openBrowserButton != null) {
                                this.openBrowserButton.active = true;
                            }
                            openDirectLink();
                        });
                    }

                    MicrosoftAuth.pollForAuthorization(info, AuthConstants.PRIMARY_CLIENT_ID, status -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null) {
                            client.execute(() -> this.statusMessage = status);
                        }
                    }).thenAccept(result -> {
                        if (result.isSuccess()) {
                            Account account = new Account(
                                    result.getUsername(),
                                    result.getUuid(),
                                    result.getAccessToken(),
                                    result.getRefreshToken(),
                                    AccountType.MICROSOFT
                            );
                            AccountStorage.addAccount(account);
                            SessionManager.login(account);
                        }
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null) {
                            client.execute(() -> {
                                if (result.isSuccess()) {
                                    client.setScreen(new AccountManagerScreen(null));
                                } else {
                                    this.statusMessage = "Giriş Başarısız: " + result.getErrorMessage();
                                }
                            });
                        }
                    });
                }).exceptionally(throwable -> {
                    if (this.client != null) {
                        this.client.execute(() -> this.statusMessage = "Hata: " + throwable.getMessage());
                    }
                    return null;
                });
    }

    @Override
    public void close() {
        if (activeTask != null) {
            activeTask.cancel(true);
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

        int cardWidth = 340;
        int cardHeight = 85;
        int cardX = centerX - cardWidth / 2;
        int cardY = centerY - 65;
        context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0x88000000);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 85, 0xFFFFFFFF);

        if (codeInfo != null && directUrl != null) {
            String codeText = "ONAY KODU: " + codeInfo.getUserCode();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(codeText), centerX, centerY - 55, 0xFF55FF55);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(directUrl), centerX, centerY - 38, 0xFF55FFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of("(Link otomatik açıldı ve panoya kopyalandı)"), centerX, centerY - 20, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(this.statusMessage), centerX, centerY - 3, 0xFFFFFF55);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(this.statusMessage), centerX, centerY - 30, 0xFFFFFF55);
        }
    }
}
