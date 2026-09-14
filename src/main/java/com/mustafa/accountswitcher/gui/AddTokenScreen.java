package com.mustafa.accountswitcher.gui;

import com.mustafa.accountswitcher.account.Account;
import com.mustafa.accountswitcher.account.AccountStorage;
import com.mustafa.accountswitcher.account.AccountType;
import com.mustafa.accountswitcher.account.SessionManager;
import com.mustafa.accountswitcher.auth.MicrosoftAuth;
import com.mustafa.accountswitcher.auth.TokenAuth;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class AddTokenScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget accessTokenField;
    private TextFieldWidget refreshTokenField;
    private ButtonWidget doneButton;
    private String statusMessage = "";
    private boolean isLoading = false;

    public AddTokenScreen(Screen parent) {
        super(Text.translatable("accountswitcher.token.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.accessTokenField = new TextFieldWidget(this.textRenderer, centerX - 150, centerY - 45, 300, 20, Text.of("Access Token"));
        this.accessTokenField.setMaxLength(3000);
        this.accessTokenField.setPlaceholder(Text.of("Örn: eyJhbGciOi... (Minecraft Access Token)"));
        this.addSelectableChild(this.accessTokenField);

        this.refreshTokenField = new TextFieldWidget(this.textRenderer, centerX - 150, centerY + 5, 300, 20, Text.of("Refresh Token"));
        this.refreshTokenField.setMaxLength(3000);
        this.refreshTokenField.setPlaceholder(Text.of("Örn: M.C500_... (Microsoft Refresh Token - Opsiyonel)"));
        this.addSelectableChild(this.refreshTokenField);

        this.doneButton = ButtonWidget.builder(Text.translatable("accountswitcher.login"), button -> submitTokens())
                .dimensions(centerX - 154, centerY + 45, 150, 20)
                .build();
        this.addDrawableChild(this.doneButton);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .dimensions(centerX + 4, centerY + 45, 150, 20)
                .build());

        setInitialFocus(this.accessTokenField);
    }

    private void submitTokens() {
        String accessToken = this.accessTokenField.getText().trim();
        String refreshToken = this.refreshTokenField.getText().trim();

        if (accessToken.isEmpty() && refreshToken.isEmpty()) {
            this.statusMessage = "Lütfen en az bir Access Token veya Refresh Token girin!";
            return;
        }

        this.isLoading = true;
        this.doneButton.active = false;

        if (accessToken.isEmpty() && !refreshToken.isEmpty()) {
            this.statusMessage = "Refresh Token ile oturum açılıyor...";
            MicrosoftAuth.refreshSession(refreshToken, status -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) client.execute(() -> this.statusMessage = status);
            }).thenAccept(result -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (result.isSuccess()) {
                    Account account = new Account(
                            result.getUsername(),
                            result.getUuid(),
                            result.getAccessToken(),
                            result.getRefreshToken() != null ? result.getRefreshToken() : refreshToken,
                            AccountType.MICROSOFT
                    );
                    AccountStorage.addAccount(account);
                    SessionManager.login(account);
                    if (client != null) client.execute(() -> client.setScreen(new AccountManagerScreen(null)));
                } else {
                    if (client != null) {
                        client.execute(() -> {
                            this.isLoading = false;
                            this.doneButton.active = true;
                            this.statusMessage = "Hata: " + result.getErrorMessage();
                        });
                    }
                }
            });
            return;
        }

        this.statusMessage = "Access Token doğrulanıyor...";
        TokenAuth.validateAccessToken(accessToken).thenAccept(result -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (result.isSuccess()) {
                String finalRefreshToken = refreshToken.isEmpty() ? null : refreshToken;
                AccountType type = finalRefreshToken != null ? AccountType.MICROSOFT : AccountType.TOKEN;

                Account account = new Account(
                        result.getUsername(),
                        result.getUuid(),
                        result.getAccessToken(),
                        finalRefreshToken,
                        type
                );
                AccountStorage.addAccount(account);
                SessionManager.login(account);
                if (client != null) client.execute(() -> client.setScreen(new AccountManagerScreen(null)));
            } else if (!refreshToken.isEmpty()) {
                if (client != null) client.execute(() -> this.statusMessage = "Access Token geçersiz. Refresh Token deneniyor...");
                MicrosoftAuth.refreshSession(refreshToken, status -> {
                    if (client != null) client.execute(() -> this.statusMessage = status);
                }).thenAccept(refreshed -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (refreshed.isSuccess()) {
                        Account account = new Account(
                                refreshed.getUsername(),
                                refreshed.getUuid(),
                                refreshed.getAccessToken(),
                                refreshed.getRefreshToken() != null ? refreshed.getRefreshToken() : refreshToken,
                                AccountType.MICROSOFT
                        );
                        AccountStorage.addAccount(account);
                        SessionManager.login(account);
                        if (mc != null) mc.execute(() -> mc.setScreen(new AccountManagerScreen(null)));
                    } else {
                        if (mc != null) {
                            mc.execute(() -> {
                                this.isLoading = false;
                                this.doneButton.active = true;
                                this.statusMessage = "Hata: " + refreshed.getErrorMessage();
                            });
                        }
                    }
                });
            } else {
                if (client != null) {
                    client.execute(() -> {
                        this.isLoading = false;
                        this.doneButton.active = true;
                        this.statusMessage = "Hata: " + result.getErrorMessage();
                    });
                }
            }
        });
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 80, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, Text.of("Minecraft Access Token:"), centerX - 150, centerY - 58, 0xFFDDDDDD);
        this.accessTokenField.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(this.textRenderer, Text.of("Microsoft Refresh Token (Opsiyonel):"), centerX - 150, centerY - 8, 0xFFDDDDDD);
        this.refreshTokenField.render(context, mouseX, mouseY, delta);

        if (!this.statusMessage.isEmpty()) {
            int color = this.statusMessage.startsWith("Hata") ? 0xFFFF5555 : 0xFFFFFF55;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(this.statusMessage), centerX, centerY + 75, color);
        }
    }
}
