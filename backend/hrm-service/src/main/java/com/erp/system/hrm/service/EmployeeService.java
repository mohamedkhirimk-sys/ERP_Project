package com.erp.system.hrm.service;

import com.erp.system.hrm.dto.EmployeeRequest;
import com.erp.system.hrm.dto.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface EmployeeService {
    EmployeeResponse createEmployee(EmployeeRequest request);
    EmployeeResponse getEmployeeById(Long id);
    Page<EmployeeResponse> getAllEmployees(Pageable pageable);
    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);
    void deleteEmployee(Long id);
    EmployeeResponse terminateEmployee(Long id);
}
