package com.mustafa.accountswitcher.account;

public enum AccountType {
    MICROSOFT("Microsoft"),
    TOKEN("Access Token"),
    OFFLINE("Offline");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
