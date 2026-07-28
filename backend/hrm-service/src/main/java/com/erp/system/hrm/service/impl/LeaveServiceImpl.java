package com.erp.system.hrm.service.impl;

import com.erp.system.hrm.dto.LeaveRequest;
import com.erp.system.hrm.dto.LeaveResponse;
import com.erp.system.hrm.entity.Employee;
import com.erp.system.hrm.entity.Leave;
import com.erp.system.hrm.repository.EmployeeRepository;
import com.erp.system.hrm.repository.LeaveRepository;
import com.erp.system.hrm.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public LeaveResponse createLeave(LeaveRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + request.getEmployeeId()));
        Leave leave = Leave.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();
        return toResponse(leaveRepository.save(leave));
    }

    @Override
    public Page<LeaveResponse> getLeavesByEmployee(Long employeeId, Pageable pageable) {
        return leaveRepository.findByEmployeeId(employeeId, pageable).map(this::toResponse);
    }

    @Override
    public Page<LeaveResponse> getAllLeaves(Pageable pageable) {
        return leaveRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public LeaveResponse updateLeaveStatus(Long id, String status) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found: " + id));
        leave.setStatus(status);
        return toResponse(leaveRepository.save(leave));
    }

    private LeaveResponse toResponse(Leave leave) {
        return LeaveResponse.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .createdAt(leave.getCreatedAt())
                .build();
    }
}
