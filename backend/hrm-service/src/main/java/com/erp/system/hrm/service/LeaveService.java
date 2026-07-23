package com.erp.system.hrm.service;

import com.erp.system.hrm.dto.LeaveRequest;
import com.erp.system.hrm.dto.LeaveResponse;
import java.util.List;

public interface LeaveService {
    LeaveResponse createLeave(LeaveRequest request);
    List<LeaveResponse> getLeavesByEmployee(Long employeeId);
    List<LeaveResponse> getAllLeaves();
    LeaveResponse updateLeaveStatus(Long id, String status);
}
