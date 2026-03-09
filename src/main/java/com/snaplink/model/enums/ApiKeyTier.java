package com.snaplink.model.enums;

public enum ApiKeyTier {
    FREE(100),
    PRO(1000),
    ENTERPRISE(10000);

    private final int defaultRateLimit;

    ApiKeyTier(int defaultRateLimit) {
        this.defaultRateLimit = defaultRateLimit;
    }

    public int getDefaultRateLimit() {
        return defaultRateLimit;
    }
}
