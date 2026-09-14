package com.mustafa.accountswitcher.gui;

import com.mojang.authlib.GameProfile;
import com.mustafa.accountswitcher.account.Account;
import com.mustafa.accountswitcher.account.AccountStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountListWidget.AccountEntry> {
    private final AccountManagerScreen parent;
    private long lastClickTime = 0L;
    private Account lastClickedAccount = null;

    public AccountListWidget(AccountManagerScreen parent, MinecraftClient client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
        this.parent = parent;
        refreshList();
    }

    public void refreshList() {
        Account previouslySelected = getSelectedAccount();
        this.clearEntries();
        List<Account> accounts = AccountStorage.getAccounts();
        String activeUuid = AccountStorage.getActiveUuid();

        for (Account account : accounts) {
            boolean isActive = account.getUuid() != null && account.getUuid().equalsIgnoreCase(activeUuid);
            AccountEntry entry = new AccountEntry(account, isActive);
            this.addEntry(entry);
            if (previouslySelected != null && account.getUuid() != null && account.getUuid().equalsIgnoreCase(previouslySelected.getUuid())) {
                this.setSelected(entry);
            }
        }
    }

    public Account getSelectedAccount() {
        AccountEntry entry = this.getSelectedOrNull();
        return entry != null ? entry.account : null;
    }

    public void selectAccount(Account target) {
        if (target == null) return;
        for (AccountEntry entry : this.children()) {
            if (entry.account == target || (entry.account.getUuid() != null && entry.account.getUuid().equalsIgnoreCase(target.getUuid()))) {
                this.setSelected(entry);
                break;
            }
        }
    }

    private static UUID parseUuidSafe(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return UUID.randomUUID();
        try {
            if (uuidStr.contains("-")) {
                return UUID.fromString(uuidStr);
            } else if (uuidStr.length() == 32) {
                return UUID.fromString(uuidStr.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (Exception ignored) {}
        return UUID.nameUUIDFromBytes(uuidStr.getBytes());
    }

    @Override
    public int getRowWidth() {
        return Math.min(360, this.width - 40);
    }

    @Override
    public boolean mouseClicked(Click click, boolean hovered) {
        boolean handled = super.mouseClicked(click, hovered);
        if (click.button() == 0) {
            Account selected = getSelectedAccount();
            if (selected != null) {
                long currentTime = Util.getMeasuringTimeMs();
                if (selected == lastClickedAccount && (currentTime - lastClickTime < 350L)) {
                    parent.doLogin();
                    return true;
                }
                lastClickedAccount = selected;
                lastClickTime = currentTime;
            }
        }
        return handled;
    }

    public class AccountEntry extends AlwaysSelectedEntryListWidget.Entry<AccountEntry> {
        public final Account account;
        private final boolean isActive;
        private final Supplier<SkinTextures> skinSupplier;

        public AccountEntry(Account account, boolean isActive) {
            this.account = account;
            this.isActive = isActive;
            UUID uuid = parseUuidSafe(account.getUuid());
            GameProfile profile = new GameProfile(uuid, account.getUsername() != null ? account.getUsername() : "Player");
            this.skinSupplier = client.getSkinProvider().supplySkinTextures(profile, true);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int entryX = this.getX();
            int entryY = this.getY();
            int entryWidth = this.getWidth();

            if (skinSupplier != null) {
                try {
                    SkinTextures skin = skinSupplier.get();
                    if (skin != null) {
                        PlayerSkinDrawer.draw(context, skin, entryX + 4, entryY + 4, 20);
                    }
                } catch (Exception ignored) {}
            }

            int textOffsetX = entryX + 28;
            String name = account.getUsername();
            String type = "[" + account.getType().getDisplayName() + "]";
            String activeStatus = isActive ? " §b(AKTİF)" : "";
            String healthBadge = account.getHealth() != null ? " " + account.getHealth().getLabel() : "";

            int nameColor = isActive ? 0xFF00E5FF : 0xFFFFFFFF;
            context.drawTextWithShadow(client.textRenderer, Text.of(name + activeStatus + healthBadge), textOffsetX, entryY + 3, nameColor);

            String uuidDisplay = "UUID: " + (account.getUuid() != null ? account.getUuid() : "Bilinmiyor");
            context.drawTextWithShadow(client.textRenderer, Text.of(uuidDisplay), textOffsetX, entryY + 15, 0xFFAAAAAA);

            int typeX = entryX + entryWidth - client.textRenderer.getWidth(type) - 8;
            context.drawTextWithShadow(client.textRenderer, Text.of(type), typeX, entryY + 8, 0xFF22D3EE);
        }

        @Override
        public boolean mouseClicked(Click click, boolean hovered) {
            return true;
        }

        @Override
        public Text getNarration() {
            return Text.of(account.getUsername());
        }
    }
}
