package com.mustafa.accountswitcher.auth;

public class AuthResult {
    private final boolean success;
    private final String username;
    private final String uuid;
    private final String accessToken;
    private final String refreshToken;
    private final String errorMessage;

    private AuthResult(boolean success, String username, String uuid, String accessToken, String refreshToken, String errorMessage) {
        this.success = success;
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.errorMessage = errorMessage;
    }

    public static AuthResult success(String username, String uuid, String accessToken, String refreshToken) {
        return new AuthResult(true, username, uuid, accessToken, refreshToken, null);
    }

    public static AuthResult failure(String errorMessage) {
        return new AuthResult(false, null, null, null, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getUsername() { return username; }
    public String getUuid() { return uuid; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getErrorMessage() { return errorMessage; }
}
