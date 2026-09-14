package com.mustafa.accountswitcher.account;

import com.google.gson.JsonObject;

public class Account {
    private String username;
    private String uuid;
    private String accessToken;
    private String refreshToken;
    private AccountType type;
    private long lastUsed;
    private boolean valid;
    private transient AccountHealth health = AccountHealth.UNKNOWN;

    public Account(String username, String uuid, String accessToken, String refreshToken, AccountType type) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.type = type;
        this.lastUsed = System.currentTimeMillis();
        this.valid = true;
        this.health = AccountHealth.UNKNOWN;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public long getLastUsed() { return lastUsed; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public AccountHealth getHealth() { return health; }
    public void setHealth(AccountHealth health) { this.health = health; }

    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("uuid", uuid);
        if (accessToken != null) json.addProperty("accessToken", accessToken);
        if (refreshToken != null) json.addProperty("refreshToken", refreshToken);
        json.addProperty("type", type != null ? type.name() : AccountType.TOKEN.name());
        json.addProperty("lastUsed", lastUsed);
        json.addProperty("valid", valid);
        return json;
    }

    public static Account fromJson(JsonObject json) {
        String username = json.has("username") ? json.get("username").getAsString() : "Player";
        String uuid = json.has("uuid") ? json.get("uuid").getAsString() : "";
        String accessToken = json.has("accessToken") ? json.get("accessToken").getAsString() : "";
        String refreshToken = json.has("refreshToken") ? json.get("refreshToken").getAsString() : null;
        AccountType type = AccountType.TOKEN;
        if (json.has("type")) {
            try {
                type = AccountType.valueOf(json.get("type").getAsString());
            } catch (Exception ignored) {}
        }
        Account account = new Account(username, uuid, accessToken, refreshToken, type);
        if (json.has("lastUsed")) account.setLastUsed(json.get("lastUsed").getAsLong());
        if (json.has("valid")) account.setValid(json.get("valid").getAsBoolean());
        return account;
    }
}
