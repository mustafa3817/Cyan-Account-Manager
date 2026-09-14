package com.mustafa.accountswitcher.mixin;

import com.mustafa.accountswitcher.account.SessionManager;
import com.mustafa.accountswitcher.gui.AccountManagerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    private Screen parent;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void accountswitcher$addAccountButton(CallbackInfo ci) {
        int xAccounts = this.width / 2 + 158;
        int y = 10;

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("§bHesaplar"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new AccountManagerScreen(this));
                    }
                }
        ).dimensions(xAccounts, y, 65, 20).build());

        int xExit = xAccounts + 68;
        String origUser = SessionManager.getOriginalUsername();
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("§cÇık"),
                button -> {
                    SessionManager.restoreOriginalSession();
                    if (this.client != null) {
                        this.client.setScreen(new MultiplayerScreen(this.parent));
                    }
                }
        ).dimensions(xExit, y, 40, 20)
         .tooltip(Tooltip.of(Text.of("Orijinal başlatıcı hesabına geri dön (" + origUser + ")")))
         .build());
    }
}
