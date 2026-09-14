package com.mustafa.accountswitcher.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("session")
    void accountswitcher$setSession(Session session);

    @Accessor("userApiService")
    void accountswitcher$setUserApiService(UserApiService userApiService);

    @Accessor("userApiService")
    UserApiService accountswitcher$getUserApiService();

    @Accessor("profileKeys")
    void accountswitcher$setProfileKeys(ProfileKeys profileKeys);

    @Accessor("profileKeys")
    ProfileKeys accountswitcher$getProfileKeys();

    @Accessor("socialInteractionsManager")
    void accountswitcher$setSocialInteractionsManager(SocialInteractionsManager socialInteractionsManager);

    @Accessor("socialInteractionsManager")
    SocialInteractionsManager accountswitcher$getSocialInteractionsManager();
}
