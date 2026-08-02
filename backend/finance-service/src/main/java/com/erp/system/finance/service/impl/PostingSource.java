package com.erp.system.finance.service.impl;

public enum PostingSource {
    ORDER_CONFIRMED,
    PAYMENT_COMPLETED,
    GOODS_RECEIVED;

    public static PostingSource fromString(String raw) {
        try {
            return valueOf(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown posting source: " + raw);
        }
    }
}
