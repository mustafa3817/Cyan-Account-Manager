package com.mustafa.accountswitcher.auth;

public class DeviceCodeInfo {
    private final String deviceCode;
    private final String userCode;
    private final String verificationUri;
    private final int expiresIn;
    private final int interval;

    public DeviceCodeInfo(String deviceCode, String userCode, String verificationUri, int expiresIn, int interval) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.expiresIn = expiresIn;
        this.interval = interval > 0 ? interval : 5;
    }

    public String getDeviceCode() { return deviceCode; }
    public String getUserCode() { return userCode; }
    public String getVerificationUri() { return verificationUri; }
    public int getExpiresIn() { return expiresIn; }
    public int getInterval() { return interval; }
}
