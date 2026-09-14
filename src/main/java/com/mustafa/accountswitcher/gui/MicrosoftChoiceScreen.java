package com.mustafa.accountswitcher.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class MicrosoftChoiceScreen extends Screen {
    private final Screen parent;

    public MicrosoftChoiceScreen(Screen parent) {
        super(Text.of("Microsoft Giriş Yöntemi Seçin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("1. Tarayıcı Otomatik Giriş (Localhost / IAS Tarzı)"),
                button -> {
                    if (this.client != null) this.client.setScreen(new LocalhostLoginScreen(this.parent));
                }
        ).dimensions(centerX - 150, centerY - 35, 300, 24).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("2. Doğrudan Kod Linki (microsoft.com/link?otc=...)"),
                button -> {
                    if (this.client != null) this.client.setScreen(new MicrosoftLoginScreen(this.parent));
                }
        ).dimensions(centerX - 150, centerY + 0, 300, 24).build());

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .dimensions(centerX - 50, centerY + 45, 100, 20)
                .build());
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

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 75, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Hangi yöntemle oturum açmak istersiniz?"), centerX, centerY - 55, 0xFFAAAAAA);
    }
}
