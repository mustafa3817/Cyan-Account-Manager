package com.mustafa.accountswitcher.mixin;

import com.mustafa.accountswitcher.account.SessionManager;
import com.mustafa.accountswitcher.gui.AccountManagerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void accountswitcher$addAccountButton(CallbackInfo ci) {
        int xAccounts = this.width / 2 + 104;
        int y = this.height / 4 + 48;
        
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("§bHesaplar"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new AccountManagerScreen(this));
                    }
                }
        ).dimensions(xAccounts, y, 70, 20).build());

        int xExit = xAccounts + 73;
        String origUser = SessionManager.getOriginalUsername();
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("§cÇık"),
                button -> {
                    SessionManager.restoreOriginalSession();
                    if (this.client != null) {
                        this.client.setScreen(new TitleScreen());
                    }
                }
        ).dimensions(xExit, y, 35, 20)
         .tooltip(Tooltip.of(Text.of("Orijinal başlatıcı hesabına geri dön (" + origUser + ")")))
         .build());
    }
}
