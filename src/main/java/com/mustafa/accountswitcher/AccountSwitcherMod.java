package com.mustafa.accountswitcher;

import com.mustafa.accountswitcher.account.AccountStorage;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountSwitcherMod implements ClientModInitializer {
    public static final String MOD_ID = "accountswitcher";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AccountSwitcher] Fabric 1.21.11 Hesap Değiştirici başlatılıyor...");
        AccountStorage.load();
        LOGGER.info("[AccountSwitcher] {} kayıtlı hesap yüklendi.", AccountStorage.getAccounts().size());
    }
}
