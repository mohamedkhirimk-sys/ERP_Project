package com.erp.system.finance.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JournalCode {
    VTE("Journal des ventes"),
    ENC("Journal des encaissements"),
    ACH("Journal des achats"),
    DEC("Journal des décaissements"),
    BNQ("Journal de banque"),
    OD("Journal des opérations diverses");

    private final String label;

    public static JournalCode fromString(String raw) {
        try {
            return valueOf(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown journal code: " + raw);
        }
    }
}
