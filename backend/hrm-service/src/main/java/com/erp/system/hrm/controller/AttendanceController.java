package com.erp.system.hrm.controller;

import com.erp.system.hrm.dto.AttendanceRequest;
import com.erp.system.hrm.dto.AttendanceResponse;
import com.erp.system.hrm.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> createAttendance(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.createAttendance(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AttendanceResponse>> getAllAttendance(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAllAttendance(pageable));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceByEmployee(@PathVariable Long employeeId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(employeeId, pageable));
    }
}
