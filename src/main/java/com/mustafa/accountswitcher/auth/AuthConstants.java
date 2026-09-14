package com.mustafa.accountswitcher.auth;

public final class AuthConstants {
    private AuthConstants() {}

    public static final String PRIMARY_CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    public static final String BACKUP_CLIENT_ID = "00000000402b5328";

    public static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    public static final String SCOPES = "XboxLive.signin XboxLive.offline_access";

    public static final String MS_DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    public static final String MS_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    public static final String MS_LIVE_TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    public static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    public static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public static final String USER_AGENT = "AccountSwitcher/1.0 (Minecraft Fabric 1.21.11)";
}
