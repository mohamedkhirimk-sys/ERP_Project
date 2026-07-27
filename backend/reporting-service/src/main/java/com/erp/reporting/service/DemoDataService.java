package com.erp.reporting.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class DemoDataService {

    private final RestTemplate rest;

    public DemoDataService(RestTemplate rest) {
        this.rest = rest;
    }

    public Map<String, Object> seedAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", seedAccounts());
        result.put("products", seedProducts());
        result.put("stock", seedStock());
        result.put("customers", seedCustomers());
        result.put("vendors", seedVendors());
        result.put("employees", seedEmployees());
        result.put("orders", seedOrders());
        result.put("invoices", seedInvoices());
        result.put("purchaseOrders", seedPurchaseOrders());
        result.put("journalEntries", seedJournalEntries());
        result.put("payroll", seedPayroll());
        return result;
    }

    private String seedAccounts() {
        List<Map<String, Object>> accounts = List.of(
            Map.of("accountCode", "1000", "accountName", "Cash & Bank", "accountType", "ASSET", "description", "Primary operating account", "balance", 50000),
            Map.of("accountCode", "1100", "accountName", "Accounts Receivable", "accountType", "ASSET", "description", "Money owed by customers", "balance", 15000),
            Map.of("accountCode", "1200", "accountName", "Inventory", "accountType", "ASSET", "description", "Goods available for sale", "balance", 25000),
            Map.of("accountCode", "2000", "accountName", "Accounts Payable", "accountType", "LIABILITY", "description", "Money owed to vendors", "balance", 12000),
            Map.of("accountCode", "3000", "accountName", "Owner's Equity", "accountType", "EQUITY", "description", "Owner's capital", "balance", 50000),
            Map.of("accountCode", "4000", "accountName", "Sales Revenue", "accountType", "REVENUE", "description", "Revenue from sales", "balance", 0),
            Map.of("accountCode", "5000", "accountName", "Salaries Expense", "accountType", "EXPENSE", "description", "Employee salaries", "balance", 0),
            Map.of("accountCode", "5100", "accountName", "Rent Expense", "accountType", "EXPENSE", "description", "Office rent", "balance", 0),
            Map.of("accountCode", "5200", "accountName", "Utilities Expense", "accountType", "EXPENSE", "description", "Electricity, water, internet", "balance", 0)
        );
        int count = 0;
        for (var a : accounts) {
            try { rest.postForObject("http://finance-service/api/accounts", a, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " accounts created";
    }

    private String seedProducts() {
        List<Map<String, Object>> products = List.of(
            Map.of("name", "Ergonomic Office Chair", "sku", "CHAIR-001", "price", 299.99, "description", "Adjustable lumbar support chair"),
            Map.of("name", "Standing Desk", "sku", "DESK-001", "price", 599.99, "description", "Electric height-adjustable desk"),
            Map.of("name", "Monitor 27-inch", "sku", "MON-001", "price", 349.99, "description", "4K UHD IPS Monitor"),
            Map.of("name", "Wireless Keyboard", "sku", "KB-001", "price", 89.99, "description", "Mechanical wireless keyboard"),
            Map.of("name", "Wireless Mouse", "sku", "MS-001", "price", 49.99, "description", "Ergonomic wireless mouse"),
            Map.of("name", "USB-C Hub", "sku", "HUB-001", "price", 39.99, "description", "7-in-1 USB-C hub"),
            Map.of("name", "Laptop Stand", "sku", "LAP-001", "price", 29.99, "description", "Adjustable aluminum laptop stand"),
            Map.of("name", "Webcam HD", "sku", "CAM-001", "price", 79.99, "description", "1080p webcam with microphone")
        );
        int count = 0;
        for (var p : products) {
            try { rest.postForObject("http://product-service/api/products", p, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " products created";
    }

    private String seedStock() {
        Object[][] stock = {
            {"CHAIR-001", 25, "Warehouse A"},
            {"DESK-001", 10, "Warehouse A"},
            {"MON-001", 15, "Warehouse B"},
            {"KB-001", 3, "Warehouse A"},
            {"MS-001", 50, "Warehouse B"},
            {"HUB-001", 100, "Warehouse A"},
            {"LAP-001", 8, "Warehouse B"},
            {"CAM-001", 0, "Warehouse A"},
        };
        int count = 0;
        for (var s : stock) {
            try {
                rest.postForObject("http://inventory-service/api/inventory/stock",
                    Map.of("productSku", s[0], "quantity", s[1], "warehouseLocation", s[2]), Map.class);
                count++;
            } catch (Exception ignored) {}
        }
        return count + " stock records created";
    }

    private String seedCustomers() {
        List<Map<String, Object>> customers = List.of(
            Map.of("name", "Acme Corporation", "email", "info@acme.com", "phone", "+1 555-0100", "address", "100 Innovation Drive, San Francisco, CA"),
            Map.of("name", "Global Trade Inc.", "email", "orders@globaltrade.com", "phone", "+1 555-0200", "address", "200 Commerce Blvd, New York, NY"),
            Map.of("name", "TechStart Solutions", "email", "hello@techstart.io", "phone", "+1 555-0300", "address", "300 Startup Way, Austin, TX"),
            Map.of("name", "Retail Plus Co.", "email", "buy@retailplus.com", "phone", "+1 555-0400", "address", "400 Market Street, Chicago, IL"),
            Map.of("name", "GreenField Organics", "email", "info@greenfield.com", "phone", "+1 555-0500", "address", "500 Farm Road, Portland, OR")
        );
        int count = 0;
        for (var c : customers) {
            try { rest.postForObject("http://sales-service/api/customers", c, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " customers created";
    }

    private String seedVendors() {
        List<Map<String, Object>> vendors = List.of(
            Map.of("name", "Office Supplies Inc.", "email", "orders@officesupplies.com", "phone", "+1 555-0600", "address", "600 Industrial Ave, Dallas, TX"),
            Map.of("name", "Tech Distributors LLC", "email", "sales@techdist.com", "phone", "+1 555-0700", "address", "700 Tech Park, Seattle, WA"),
            Map.of("name", "Furniture World", "email", "info@furnitureworld.com", "phone", "+1 555-0800", "address", "800 Design Row, Los Angeles, CA"),
            Map.of("name", "Packaging Pro", "email", "orders@packagingpro.com", "phone", "+1 555-0900", "address", "900 Logistics Lane, Memphis, TN")
        );
        int count = 0;
        for (var v : vendors) {
            try { rest.postForObject("http://procurement-service/api/vendors", v, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " vendors created";
    }

    private String seedEmployees() {
        List<Map<String, Object>> employees = List.of(
            Map.of("employeeId", "EMP-001", "firstName", "John", "lastName", "Smith", "email", "john.smith@erp.com", "phone", "+1 555-1001", "department", "Engineering", "position", "Software Engineer", "salary", 85000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-002", "firstName", "Jane", "lastName", "Doe", "email", "jane.doe@erp.com", "phone", "+1 555-1002", "department", "Marketing", "position", "Marketing Manager", "salary", 75000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-003", "firstName", "Bob", "lastName", "Johnson", "email", "bob.johnson@erp.com", "phone", "+1 555-1003", "department", "Sales", "position", "Sales Representative", "salary", 65000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-004", "firstName", "Alice", "lastName", "Williams", "email", "alice.williams@erp.com", "phone", "+1 555-1004", "department", "Finance", "position", "Accountant", "salary", 70000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-005", "firstName", "Charlie", "lastName", "Brown", "email", "charlie.brown@erp.com", "phone", "+1 555-1005", "department", "Engineering", "position", "DevOps Engineer", "salary", 90000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-006", "firstName", "Diana", "lastName", "Prince", "email", "diana.prince@erp.com", "phone", "+1 555-1006", "department", "HR", "position", "HR Manager", "salary", 72000, "status", "ACTIVE"),
            Map.of("employeeId", "EMP-007", "firstName", "Eve", "lastName", "Davis", "email", "eve.davis@erp.com", "phone", "+1 555-1007", "department", "Sales", "position", "Sales Manager", "salary", 82000, "status", "TERMINATED")
        );
        int count = 0;
        for (var e : employees) {
            try { rest.postForObject("http://hrm-service/api/employees", e, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " employees created";
    }

    private String seedOrders() {
        var today = LocalDate.now();
        List<Map<String, Object>> orders = List.of(
            Map.of("customerName", "Acme Corporation", "totalAmount", 2999.90, "status", "DELIVERED", "paymentMethod", "CREDIT_CARD",
                   "items", List.of(Map.of("productSku", "CHAIR-001", "quantity", 5), Map.of("productSku", "DESK-001", "quantity", 2))),
            Map.of("customerName", "TechStart Solutions", "totalAmount", 1049.97, "status", "SHIPPED", "paymentMethod", "BANK_TRANSFER",
                   "items", List.of(Map.of("productSku", "MON-001", "quantity", 3))),
            Map.of("customerName", "Global Trade Inc.", "totalAmount", 699.85, "status", "PENDING", "paymentMethod", "CREDIT_CARD",
                   "items", List.of(Map.of("productSku", "KB-001", "quantity", 5), Map.of("productSku", "MS-001", "quantity", 5))),
            Map.of("customerName", "Retail Plus Co.", "totalAmount", 479.85, "status", "CONFIRMED", "paymentMethod", "CASH",
                   "items", List.of(Map.of("productSku", "HUB-001", "quantity", 6), Map.of("productSku", "LAP-001", "quantity", 8))),
            Map.of("customerName", "Acme Corporation", "totalAmount", 1599.95, "status", "DELIVERED", "paymentMethod", "CREDIT_CARD",
                   "items", List.of(Map.of("productSku", "DESK-001", "quantity", 1), Map.of("productSku", "MON-001", "quantity", 2), Map.of("productSku", "CAM-001", "quantity", 5)))
        );
        int count = 0;
        for (var o : orders) {
            try { rest.postForObject("http://order-service/api/orders", o, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " orders created";
    }

    private String seedInvoices() {
        var today = LocalDate.now();
        List<Map<String, Object>> invoices = List.of(
            Map.of("customerId", 1, "totalAmount", 2999.90, "status", "PAID", "dueDate", today.plusDays(30).toString(),
                   "items", List.of(Map.of("productSku", "CHAIR-001", "quantity", 5))),
            Map.of("customerId", 3, "totalAmount", 1049.97, "status", "PENDING", "dueDate", today.plusDays(15).toString(),
                   "items", List.of(Map.of("productSku", "MON-001", "quantity", 3))),
            Map.of("customerId", 2, "totalAmount", 699.85, "status", "PENDING", "dueDate", today.plusDays(45).toString(),
                   "items", List.of(Map.of("productSku", "KB-001", "quantity", 5))),
            Map.of("customerId", 4, "totalAmount", 479.85, "status", "PAID", "dueDate", today.plusDays(30).toString(),
                   "items", List.of(Map.of("productSku", "HUB-001", "quantity", 6)))
        );
        int count = 0;
        for (var i : invoices) {
            try { rest.postForObject("http://sales-service/api/invoices", i, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " invoices created";
    }

    private String seedPurchaseOrders() {
        List<Map<String, Object>> pos = List.of(
            Map.of("vendorId", 1, "totalAmount", 2500.00, "status", "RECEIVED"),
            Map.of("vendorId", 2, "totalAmount", 4500.00, "status", "APPROVED"),
            Map.of("vendorId", 3, "totalAmount", 3200.00, "status", "PENDING"),
            Map.of("vendorId", 1, "totalAmount", 1800.00, "status", "RECEIVED"),
            Map.of("vendorId", 2, "totalAmount", 6000.00, "status", "PENDING")
        );
        int count = 0;
        for (var po : pos) {
            try { rest.postForObject("http://procurement-service/api/purchase-orders", po, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " purchase orders created";
    }

    private String seedJournalEntries() {
        var today = LocalDate.now();
        // accountId mapping: Cash=1000, A/R=1100, Inventory=1200, A/P=2000, Equity=3000, Revenue=4000, Salaries=5000, Rent=5100, Utilities=5200
        var entries = List.of(
            Map.of("entryDate", today.minusDays(5).toString(), "description", "Cash sale — Acme Corporation", "accountId", 1, "debit", 2999.90, "credit", 0),
            Map.of("entryDate", today.minusDays(5).toString(), "description", "Cash sale — Acme Corporation", "accountId", 6, "debit", 0, "credit", 2999.90),
            Map.of("entryDate", today.minusDays(3).toString(), "description", "Payroll for July 2026", "accountId", 7, "debit", 45700.00, "credit", 0),
            Map.of("entryDate", today.minusDays(3).toString(), "description", "Payroll for July 2026", "accountId", 1, "debit", 0, "credit", 45700.00),
            Map.of("entryDate", today.minusDays(2).toString(), "description", "Office rent payment", "accountId", 8, "debit", 5000.00, "credit", 0),
            Map.of("entryDate", today.minusDays(2).toString(), "description", "Office rent payment", "accountId", 1, "debit", 0, "credit", 5000.00),
            Map.of("entryDate", today.minusDays(1).toString(), "description", "Purchase inventory on credit", "accountId", 3, "debit", 2500.00, "credit", 0),
            Map.of("entryDate", today.minusDays(1).toString(), "description", "Purchase inventory on credit", "accountId", 4, "debit", 0, "credit", 2500.00)
        );
        int count = 0;
        for (var e : entries) {
            try { rest.postForObject("http://finance-service/api/journal-entries", e, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " journal entries created";
    }

    private String seedPayroll() {
        List<Map<String, Object>> payroll = List.of(
            Map.of("employeeId", "EMP-001", "employeeName", "John Smith", "grossSalary", 7083.33, "deductions", 1416.67, "netSalary", 5666.66, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PAID"),
            Map.of("employeeId", "EMP-002", "employeeName", "Jane Doe", "grossSalary", 6250.00, "deductions", 1250.00, "netSalary", 5000.00, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PAID"),
            Map.of("employeeId", "EMP-003", "employeeName", "Bob Johnson", "grossSalary", 5416.67, "deductions", 1083.33, "netSalary", 4333.34, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PAID"),
            Map.of("employeeId", "EMP-004", "employeeName", "Alice Williams", "grossSalary", 5833.33, "deductions", 1166.67, "netSalary", 4666.66, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PENDING"),
            Map.of("employeeId", "EMP-005", "employeeName", "Charlie Brown", "grossSalary", 7500.00, "deductions", 1500.00, "netSalary", 6000.00, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PENDING"),
            Map.of("employeeId", "EMP-006", "employeeName", "Diana Prince", "grossSalary", 6000.00, "deductions", 1200.00, "netSalary", 4800.00, "payPeriodStart", "2026-07-01", "payPeriodEnd", "2026-07-31", "status", "PENDING")
        );
        int count = 0;
        for (var p : payroll) {
            try { rest.postForObject("http://finance-service/api/payroll", p, Map.class); count++; } catch (Exception ignored) {}
        }
        return count + " payroll records created";
    }
}
