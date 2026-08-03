package com.erp.system.finance.service;

import com.erp.system.finance.dto.JournalDetailResponse;
import com.erp.system.finance.dto.JournalSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface JournalService {

    List<JournalSummaryResponse> summary();

    JournalDetailResponse detail(String code, LocalDate from, LocalDate to);
}
