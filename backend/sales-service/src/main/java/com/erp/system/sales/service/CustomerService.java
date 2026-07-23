package com.erp.system.sales.service;

import com.erp.system.sales.dto.CustomerRequest;
import com.erp.system.sales.entity.Customer;
import java.util.List;

public interface CustomerService {
    Customer createCustomer(CustomerRequest request);
    Customer getCustomerById(Long id);
    List<Customer> getAllCustomers();
    Customer updateCustomer(Long id, CustomerRequest request);
    void deleteCustomer(Long id);
}
