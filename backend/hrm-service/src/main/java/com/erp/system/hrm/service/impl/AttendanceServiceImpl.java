package com.erp.system.hrm.service.impl;

import com.erp.system.hrm.dto.AttendanceRequest;
import com.erp.system.hrm.dto.AttendanceResponse;
import com.erp.system.hrm.entity.Attendance;
import com.erp.system.hrm.entity.Employee;
import com.erp.system.hrm.repository.AttendanceRepository;
import com.erp.system.hrm.repository.EmployeeRepository;
import com.erp.system.hrm.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public AttendanceResponse createAttendance(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + request.getEmployeeId()));
        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(request.getDate())
                .clockIn(request.getClockIn())
                .clockOut(request.getClockOut())
                .status(request.getStatus() != null ? request.getStatus() : "PRESENT")
                .build();
        return toResponse(attendanceRepository.save(attendance));
    }

    @Override
    public List<AttendanceResponse> getAttendanceByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<AttendanceResponse> getAllAttendance() {
        return attendanceRepository.findAll().stream().map(this::toResponse).toList();
    }

    private AttendanceResponse toResponse(Attendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName())
                .date(a.getDate())
                .clockIn(a.getClockIn())
                .clockOut(a.getClockOut())
                .status(a.getStatus())
                .build();
    }
}
