package com.erp.system.hrm.controller;

import com.erp.system.hrm.dto.LeaveRequest;
import com.erp.system.hrm.dto.LeaveResponse;
import com.erp.system.hrm.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<LeaveResponse> createLeave(@Valid @RequestBody LeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.createLeave(request));
    }

    @GetMapping
    public ResponseEntity<List<LeaveResponse>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponse>> getLeavesByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employeeId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeaveResponse> updateLeaveStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(leaveService.updateLeaveStatus(id, status));
    }
}
