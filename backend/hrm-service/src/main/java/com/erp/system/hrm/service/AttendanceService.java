package com.erp.system.hrm.service;

import com.erp.system.hrm.dto.AttendanceRequest;
import com.erp.system.hrm.dto.AttendanceResponse;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse createAttendance(AttendanceRequest request);
    List<AttendanceResponse> getAttendanceByEmployee(Long employeeId);
    List<AttendanceResponse> getAllAttendance();
}
