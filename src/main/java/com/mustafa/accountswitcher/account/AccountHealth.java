package com.mustafa.accountswitcher.account;

public enum AccountHealth {
    UNKNOWN("§7Bilinmiyor", 0xAAAAAA),
    VALID("§aGeçerli", 0x55FF55),
    REFRESHABLE("§eYenilenebilir", 0xFFFF55),
    EXPIRED("§cGeçersiz", 0xFF5555);

    private final String label;
    private final int color;

    AccountHealth(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public int getColor() { return color; }
}
