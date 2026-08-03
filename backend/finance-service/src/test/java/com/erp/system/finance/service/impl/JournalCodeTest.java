package com.erp.system.finance.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalCodeTest {

    @Test
    void parsesAllStandardJournalCodes() {
        assertThat(JournalCode.fromString("VTE").getLabel()).isEqualTo("Journal des ventes");
        assertThat(JournalCode.fromString("ENC").getLabel()).isEqualTo("Journal des encaissements");
        assertThat(JournalCode.fromString("ACH").getLabel()).isEqualTo("Journal des achats");
        assertThat(JournalCode.fromString("DEC").getLabel()).isEqualTo("Journal des décaissements");
        assertThat(JournalCode.fromString("BNQ").getLabel()).isEqualTo("Journal de banque");
        assertThat(JournalCode.fromString("OD").getLabel()).isEqualTo("Journal des opérations diverses");
    }

    @Test
    void rejectsUnknownJournalCode() {
        assertThatThrownBy(() -> JournalCode.fromString("XXX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    void valuesContainsAllSixJournals() {
        assertThat(JournalCode.values()).hasSize(6);
    }
}
