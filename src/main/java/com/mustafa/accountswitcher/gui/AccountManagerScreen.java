package com.mustafa.accountswitcher.gui;

import com.mustafa.accountswitcher.account.Account;
import com.mustafa.accountswitcher.account.AccountHealthChecker;
import com.mustafa.accountswitcher.account.AccountStorage;
import com.mustafa.accountswitcher.account.SessionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class AccountManagerScreen extends Screen {
    private final Screen parent;
    private AccountListWidget listWidget;
    private ButtonWidget loginButton;
    private ButtonWidget deleteButton;
    private ButtonWidget moveUpButton;
    private ButtonWidget moveDownButton;
    private String statusMessage = "";

    public AccountManagerScreen(Screen parent) {
        super(Text.translatable("accountswitcher.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int topY = 30;
        int listHeight = this.height - topY - 72;
        this.listWidget = new AccountListWidget(this, this.client, this.width, listHeight, topY, 28);
        this.addSelectableChild(this.listWidget);

        int y1 = this.height - 62;
        int y2 = this.height - 36;
        int centerX = this.width / 2;

        int startX1 = centerX - 192;
        this.loginButton = ButtonWidget.builder(Text.translatable("accountswitcher.login"), button -> doLogin())
                .dimensions(startX1, y1, 75, 20)
                .build();
        this.addDrawableChild(this.loginButton);

        this.moveUpButton = ButtonWidget.builder(Text.of("▲"), button -> doMoveUp())
                .dimensions(startX1 + 80, y1, 22, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.of("Seçili hesabı yukarı taşı")))
                .build();
        this.addDrawableChild(this.moveUpButton);

        this.moveDownButton = ButtonWidget.builder(Text.of("▼"), button -> doMoveDown())
                .dimensions(startX1 + 105, y1, 22, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.of("Seçili hesabı aşağı taşı")))
                .build();
        this.addDrawableChild(this.moveDownButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.add.microsoft"), button -> {
            if (this.client != null) this.client.setScreen(new MicrosoftChoiceScreen(this));
        }).dimensions(startX1 + 132, y1, 85, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.add.token"), button -> {
            if (this.client != null) this.client.setScreen(new AddTokenScreen(this));
        }).dimensions(startX1 + 222, y1, 85, 20).build());

        this.deleteButton = ButtonWidget.builder(Text.translatable("accountswitcher.delete"), button -> doDelete())
                .dimensions(startX1 + 312, y1, 70, 20)
                .build();
        this.addDrawableChild(this.deleteButton);

        int startX2 = centerX - 187;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.import"), button -> doImport())
                .dimensions(startX2, y2, 85, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.export"), button -> doExport())
                .dimensions(startX2 + 90, y2, 85, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.validate_all"), button -> doValidateAll())
                .dimensions(startX2 + 180, y2, 115, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("accountswitcher.done"), button -> close())
                .dimensions(startX2 + 300, y2, 75, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("§9Discord"),
                button -> {
                    try {
                        net.minecraft.util.Util.getOperatingSystem().open(java.net.URI.create("https://discord.com/invite/YzujvvZQjE"));
                    } catch (Exception ignored) {}
                }
        ).dimensions(10, 6, 75, 20)
         .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.of("CyanAFK Discord Sunucusuna Katıl")))
         .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("Orijinal Hesaba Dön"),
                button -> {
                    SessionManager.restoreOriginalSession();
                    this.statusMessage = "Orijinal hesaba dönüldü: " + SessionManager.getOriginalUsername();
                    this.listWidget.refreshList();
                    updateButtonState();
                }
        ).dimensions(this.width - 145, 6, 135, 20)
         .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.of("Launcher'ın orijinal hesabına (" + SessionManager.getOriginalUsername() + ") geçiş yap")))
         .build());

        updateButtonState();

        doValidateAll();
    }

    public void doLogin() {
        Account selected = this.listWidget.getSelectedAccount();
        if (selected == null) return;

        this.statusMessage = "Giriş yapılıyor...";
        this.loginButton.active = false;

        SessionManager.login(selected).thenAccept(success -> {
            if (this.client != null) {
                this.client.execute(() -> {
                    this.loginButton.active = true;
                    if (success) {
                        this.statusMessage = "Giriş başarılı: " + selected.getUsername();
                        this.listWidget.refreshList();
                    } else {
                        this.statusMessage = "Giriş başarısız! Token veya Refresh Token geçersiz.";
                    }
                });
            }
        });
    }

    private void doDelete() {
        Account selected = this.listWidget.getSelectedAccount();
        if (selected != null) {
            AccountStorage.removeAccount(selected);
            this.listWidget.refreshList();
            updateButtonState();
            this.statusMessage = "Hesap silindi.";
        }
    }

    private void doMoveUp() {
        Account selected = this.listWidget.getSelectedAccount();
        if (selected != null && AccountStorage.moveUp(selected)) {
            this.listWidget.refreshList();
            this.listWidget.selectAccount(selected);
            updateButtonState();
        }
    }

    private void doMoveDown() {
        Account selected = this.listWidget.getSelectedAccount();
        if (selected != null && AccountStorage.moveDown(selected)) {
            this.listWidget.refreshList();
            this.listWidget.selectAccount(selected);
            updateButtonState();
        }
    }

    private void doExport() {
        String json = AccountStorage.exportToJsonString();
        if (this.client != null) {
            this.client.keyboard.setClipboard(json);
            this.statusMessage = "Tüm hesaplar panoya kopyalandı! (JSON)";
        }
    }

    private void doImport() {
        if (this.client == null) return;
        String clipboardText = this.client.keyboard.getClipboard();
        int count = 0;
        if (clipboardText != null && !clipboardText.isBlank()) {
            count = AccountStorage.importFromJsonString(clipboardText);
        }
        if (count == 0) {
            count = AccountStorage.importFromOpsecFile();
        }

        if (count > 0) {
            this.listWidget.refreshList();
            this.statusMessage = count + " hesap başarıyla içe aktarıldı!";
            doValidateAll();
        } else {
            this.statusMessage = "Pano boş veya geçerli hesap verisi bulunamadı!";
        }
    }

    private void doValidateAll() {
        this.statusMessage = "Hesap durumları kontrol ediliyor...";
        AccountHealthChecker.checkAll(AccountStorage.getAccounts(), () -> {
            if (this.client != null) {
                this.client.execute(() -> this.listWidget.refreshList());
            }
        }).thenRun(() -> {
            if (this.client != null) {
                this.client.execute(() -> {
                    this.listWidget.refreshList();
                    this.statusMessage = "Hesap kontrolleri tamamlandı.";
                });
            }
        });
    }

    private void updateButtonState() {
        boolean hasSelection = this.listWidget != null && this.listWidget.getSelectedAccount() != null;
        if (this.loginButton != null) this.loginButton.active = hasSelection;
        if (this.deleteButton != null) this.deleteButton.active = hasSelection;

        if (this.moveUpButton != null) {
            int index = hasSelection ? AccountStorage.getAccounts().indexOf(this.listWidget.getSelectedAccount()) : -1;
            this.moveUpButton.active = hasSelection && index > 0;
        }
        if (this.moveDownButton != null) {
            int index = hasSelection ? AccountStorage.getAccounts().indexOf(this.listWidget.getSelectedAccount()) : -1;
            this.moveDownButton.active = hasSelection && index >= 0 && index < AccountStorage.getAccounts().size() - 1;
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateButtonState();
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
        this.listWidget.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFF00E5FF);

        if (AccountStorage.getAccounts().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of("§7Henüz eklenmiş hesap yok."), this.width / 2, this.height / 2 - 20, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of("§bCyan Acc Manager §8ile 'MS Girişi' veya 'Token Ekle' butonunu kullanın."), this.width / 2, this.height / 2 - 5, 0xFF888888);
        }

        if (!this.statusMessage.isEmpty()) {
            int color = this.statusMessage.contains("başarılı") || this.statusMessage.contains("tamamlandı") || this.statusMessage.contains("dönüldü") ? 0xFF00E5FF : 0xFFFFFF55;
            if (this.statusMessage.contains("başarısız") || this.statusMessage.contains("Geçersiz") || this.statusMessage.contains("boş") || this.statusMessage.contains("Hata")) {
                color = 0xFFFF5555;
            }
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(this.statusMessage), this.width / 2, this.height - 78, color);
        }
    }
}
