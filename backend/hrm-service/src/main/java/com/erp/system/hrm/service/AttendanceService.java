package com.erp.system.hrm.service;

import com.erp.system.hrm.dto.AttendanceRequest;
import com.erp.system.hrm.dto.AttendanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse createAttendance(AttendanceRequest request);
    Page<AttendanceResponse> getAttendanceByEmployee(Long employeeId, Pageable pageable);
    Page<AttendanceResponse> getAllAttendance(Pageable pageable);
}
