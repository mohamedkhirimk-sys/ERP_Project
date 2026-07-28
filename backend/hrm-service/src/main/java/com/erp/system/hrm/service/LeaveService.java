package com.erp.system.hrm.service;

import com.erp.system.hrm.dto.LeaveRequest;
import com.erp.system.hrm.dto.LeaveResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LeaveService {
    LeaveResponse createLeave(LeaveRequest request);
    Page<LeaveResponse> getLeavesByEmployee(Long employeeId, Pageable pageable);
    Page<LeaveResponse> getAllLeaves(Pageable pageable);
    LeaveResponse updateLeaveStatus(Long id, String status);
}
