package com.erp.system.hrm.service.impl;

import com.erp.system.hrm.dto.EmployeeRequest;
import com.erp.system.hrm.dto.EmployeeResponse;
import com.erp.system.hrm.entity.Employee;
import com.erp.system.hrm.repository.EmployeeRepository;
import com.erp.system.hrm.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .position(request.getPosition())
                .salary(request.getSalary())
                .hireDate(request.getHireDate())
                .build();
        return toResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        return toResponse(employee);
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setSalary(request.getSalary());
        employee.setHireDate(request.getHireDate());
        return toResponse(employeeRepository.save(employee));
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found: " + id);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeResponse toResponse(Employee emp) {
        return EmployeeResponse.builder()
                .id(emp.getId())
                .employeeId(emp.getEmployeeId())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .department(emp.getDepartment())
                .position(emp.getPosition())
                .salary(emp.getSalary())
                .hireDate(emp.getHireDate())
                .status(emp.getStatus())
                .createdAt(emp.getCreatedAt())
                .build();
    }
}
