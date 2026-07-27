package com.erp.reporting.service;

import com.erp.reporting.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final RestTemplate rest;

    public ReportService(RestTemplate rest) {
        this.rest = rest;
    }

    // ──────────────────────────────────────
    //  Sales Report
    // ──────────────────────────────────────

    public SalesReport getSalesReport() {
        List<Map<String, Object>> orders = fetchList("http://order-service/api/orders");
        List<Map<String, Object>> invoices = fetchList("http://sales-service/api/invoices");

        BigDecimal totalRevenue = orders.stream()
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase((String) i.get("status")))
                .count();
        long pendingCount = invoices.stream()
                .filter(i -> "PENDING".equalsIgnoreCase((String) i.get("status")))
                .count();

        List<SalesReport.OrderSummary> topOrders = orders.stream()
                .map(o -> SalesReport.OrderSummary.builder()
                        .orderNumber((String) o.get("orderNumber"))
                        .customerName((String) o.get("customerName"))
                        .totalAmount(new BigDecimal(o.get("totalAmount").toString()))
                        .status((String) o.get("status"))
                        .build())
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(10)
                .toList();

        long orderCount = orders.size();
        BigDecimal avgOrder = orderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SalesReport.Summary summary = SalesReport.Summary.builder()
                .totalOrders(orderCount)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrder)
                .paidInvoices(paidCount)
                .pendingInvoices(pendingCount)
                .build();

        List<SalesReport.DailyRevenue> daily = orders.stream()
                .collect(Collectors.groupingBy(o -> {
                    Object dt = o.getOrDefault("orderedAt", o.get("createdAt"));
                    if (dt == null) return LocalDate.now();
                    return LocalDate.parse(dt.toString().substring(0, 10));
                }))
                .entrySet().stream()
                .map(e -> SalesReport.DailyRevenue.builder()
                        .date(e.getKey())
                        .orderCount(e.getValue().size())
                        .revenue(e.getValue().stream()
                                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted(Comparator.comparing(SalesReport.DailyRevenue::getDate))
                .toList();

        return SalesReport.builder().summary(summary).topOrders(topOrders).dailyRevenue(daily).build();
    }

    // ──────────────────────────────────────
    //  Inventory Report
    // ──────────────────────────────────────

    public InventoryReport getInventoryReport() {
        List<Map<String, Object>> products = fetchList("http://product-service/api/products");
        List<Map<String, Object>> stocks = fetchList("http://inventory-service/api/inventory/stocks");

        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(
                        p -> (String) p.get("sku"),
                        p -> (String) p.get("name"),
                        (a, b) -> a));

        List<InventoryReport.StockItem> items = stocks.stream()
                .map(s -> InventoryReport.StockItem.builder()
                        .sku((String) s.get("productSku"))
                        .productName(productNames.getOrDefault((String) s.get("productSku"), "—"))
                        .quantity(((Number) s.get("quantity")).intValue())
                        .warehouseLocation((String) s.getOrDefault("warehouseLocation", ""))
                        .build())
                .sorted(Comparator.comparing(InventoryReport.StockItem::getQuantity))
                .toList();

        long lowStock = items.stream().filter(i -> i.getQuantity() > 0 && i.getQuantity() <= 10).count();
        long outOfStock = items.stream().filter(i -> i.getQuantity() == 0).count();

        InventoryReport.Summary summary = InventoryReport.Summary.builder()
                .totalProducts(products.size())
                .totalStockItems(stocks.size())
                .lowStockCount(lowStock)
                .outOfStockCount(outOfStock)
                .build();

        List<InventoryReport.StockItem> lowItems = items.stream()
                .filter(i -> i.getQuantity() <= 10)
                .sorted(Comparator.comparing(InventoryReport.StockItem::getQuantity))
                .toList();

        return InventoryReport.builder().summary(summary).stockItems(items).lowStockItems(lowItems).build();
    }

    // ──────────────────────────────────────
    //  Financial Report
    // ──────────────────────────────────────

    public FinancialReport getFinancialReport() {
        List<Map<String, Object>> accounts = fetchList("http://finance-service/api/accounts");
        List<Map<String, Object>> entries = fetchList("http://finance-service/api/journal-entries");

        BigDecimal totalDebits = entries.stream()
                .map(e -> new BigDecimal(e.get("debit").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream()
                .map(e -> new BigDecimal(e.get("credit").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinancialReport.Summary summary = FinancialReport.Summary.builder()
                .totalAccounts(accounts.size())
                .totalJournalEntries(entries.size())
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .build();

        List<FinancialReport.AccountBalance> trialBalance = accounts.stream().map(a -> {
            Long accountId = ((Number) a.get("id")).longValue();
            BigDecimal debits = entries.stream()
                    .filter(e -> ((Number) e.get("accountId")).longValue() == accountId)
                    .map(e -> new BigDecimal(e.get("debit").toString()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = entries.stream()
                    .filter(e -> ((Number) e.get("accountId")).longValue() == accountId)
                    .map(e -> new BigDecimal(e.get("credit").toString()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return FinancialReport.AccountBalance.builder()
                    .accountCode((String) a.get("accountCode"))
                    .accountName((String) a.get("accountName"))
                    .accountType((String) a.get("accountType"))
                    .balance(new BigDecimal(a.get("balance").toString()))
                    .totalDebits(debits)
                    .totalCredits(credits)
                    .build();
        }).sorted(Comparator.comparing(FinancialReport.AccountBalance::getAccountCode)).toList();

        List<FinancialReport.JournalSummary> recent = entries.stream()
                .map(e -> FinancialReport.JournalSummary.builder()
                        .entryNumber((String) e.get("entryNumber"))
                        .description((String) e.getOrDefault("description", ""))
                        .debit(new BigDecimal(e.get("debit").toString()))
                        .credit(new BigDecimal(e.get("credit").toString()))
                        .entryDate(LocalDate.parse(e.get("entryDate").toString().substring(0, 10)))
                        .build())
                .sorted((a, b) -> b.getEntryDate().compareTo(a.getEntryDate()))
                .limit(20)
                .toList();

        return FinancialReport.builder().summary(summary).trialBalance(trialBalance).recentEntries(recent).build();
    }

    // ──────────────────────────────────────
    //  HR Report
    // ──────────────────────────────────────

    public HrReport getHrReport() {
        List<Map<String, Object>> employees = fetchList("http://hrm-service/api/employees");
        List<Map<String, Object>> leaves = fetchList("http://hrm-service/api/leaves");
        List<Map<String, Object>> payrolls = fetchList("http://finance-service/api/payroll");

        long active = employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase((String) e.get("status"))).count();
        long terminated = employees.stream().filter(e -> "TERMINATED".equalsIgnoreCase((String) e.get("status"))).count();
        long pendingLeaves = leaves.stream().filter(l -> "PENDING".equalsIgnoreCase((String) l.get("status"))).count();

        HrReport.Summary summary = HrReport.Summary.builder()
                .totalEmployees(employees.size())
                .activeEmployees(active)
                .terminatedEmployees(terminated)
                .pendingLeaves(pendingLeaves)
                .build();

        List<HrReport.DepartmentSummary> byDept = employees.stream()
                .collect(Collectors.groupingBy(e -> {
                    String dept = (String) e.get("department");
                    return dept != null && !dept.isBlank() ? dept : "Unassigned";
                }))
                .entrySet().stream()
                .map(e -> {
                    BigDecimal total = e.getValue().stream()
                            .map(emp -> {
                                Object sal = emp.get("salary");
                                if (sal == null) return BigDecimal.ZERO;
                                return new BigDecimal(sal.toString());
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long count = e.getValue().size();
                    return HrReport.DepartmentSummary.builder()
                            .department(e.getKey())
                            .employeeCount(count)
                            .totalSalary(total)
                            .avgSalary(count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .build();
                })
                .sorted((a, b) -> b.getEmployeeCount() > a.getEmployeeCount() ? 1 : -1)
                .toList();

        List<HrReport.PayrollSummary> recentPayroll = payrolls.stream()
                .map(p -> HrReport.PayrollSummary.builder()
                        .employeeName((String) p.get("employeeName"))
                        .grossSalary(new BigDecimal(p.get("grossSalary").toString()))
                        .deductions(new BigDecimal(p.getOrDefault("deductions", "0").toString()))
                        .netSalary(new BigDecimal(p.get("netSalary").toString()))
                        .status((String) p.get("status"))
                        .period(p.get("payPeriodStart") + " — " + p.get("payPeriodEnd"))
                        .build())
                .sorted((a, b) -> b.getGrossSalary().compareTo(a.getGrossSalary()))
                .limit(20)
                .toList();

        return HrReport.builder().summary(summary).byDepartment(byDept).recentPayroll(recentPayroll).build();
    }

    // ──────────────────────────────────────
    //  Procurement Report
    // ──────────────────────────────────────

    public ProcurementReport getProcurementReport() {
        List<Map<String, Object>> vendors = fetchList("http://procurement-service/api/vendors");
        List<Map<String, Object>> pos = fetchList("http://procurement-service/api/purchase-orders");

        long pendingPOs = pos.stream().filter(p -> "PENDING".equalsIgnoreCase((String) p.get("status"))).count();
        long receivedPOs = pos.stream().filter(p -> "RECEIVED".equalsIgnoreCase((String) p.get("status"))).count();
        BigDecimal totalPoAmount = pos.stream()
                .map(p -> new BigDecimal(p.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProcurementReport.Summary summary = ProcurementReport.Summary.builder()
                .totalVendors(vendors.size())
                .totalPurchaseOrders(pos.size())
                .pendingPOs(pendingPOs)
                .receivedPOs(receivedPOs)
                .totalPoAmount(totalPoAmount)
                .build();

        List<ProcurementReport.PurchaseOrderSummary> pending = pos.stream()
                .filter(p -> "PENDING".equalsIgnoreCase((String) p.get("status")))
                .map(p -> ProcurementReport.PurchaseOrderSummary.builder()
                        .poNumber((String) p.get("poNumber"))
                        .vendorName((String) p.get("vendorName"))
                        .totalAmount(new BigDecimal(p.get("totalAmount").toString()))
                        .status((String) p.get("status"))
                        .orderedAt(LocalDate.parse(p.get("orderedAt").toString().substring(0, 10)))
                        .build())
                .sorted((a, b) -> a.getOrderedAt().compareTo(b.getOrderedAt()))
                .toList();

        Map<String, List<Map<String, Object>>> byVendor = pos.stream()
                .collect(Collectors.groupingBy(p -> (String) p.get("vendorName")));
        List<ProcurementReport.VendorActivity> topVendors = byVendor.entrySet().stream()
                .map(e -> ProcurementReport.VendorActivity.builder()
                        .vendorName(e.getKey())
                        .poCount(e.getValue().size())
                        .totalAmount(e.getValue().stream()
                                .map(p -> new BigDecimal(p.get("totalAmount").toString()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(10)
                .toList();

        return ProcurementReport.builder().summary(summary).pendingOrders(pending).topVendors(topVendors).build();
    }

    // ──────────────────────────────────────
    //  Dashboard Summary (all reports condensed)
    // ──────────────────────────────────────

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> dash = new LinkedHashMap<>();

        List<Map<String, Object>> orders = fetchList("http://order-service/api/orders");
        List<Map<String, Object>> invoices = fetchList("http://sales-service/api/invoices");
        List<Map<String, Object>> stocks = fetchList("http://inventory-service/api/inventory/stocks");
        List<Map<String, Object>> employees = fetchList("http://hrm-service/api/employees");
        List<Map<String, Object>> payrols = fetchList("http://finance-service/api/payroll");
        List<Map<String, Object>> pos = fetchList("http://procurement-service/api/purchase-orders");

        dash.put("totalOrders", orders.size());
        dash.put("totalInvoices", invoices.size());
        dash.put("paidInvoices", invoices.stream().filter(i -> "PAID".equalsIgnoreCase((String) i.get("status"))).count());
        dash.put("pendingInvoices", invoices.stream().filter(i -> "PENDING".equalsIgnoreCase((String) i.get("status"))).count());

        dash.put("lowStockItems", stocks.stream().filter(s -> ((Number) s.get("quantity")).intValue() <= 10).count());
        dash.put("outOfStockItems", stocks.stream().filter(s -> ((Number) s.get("quantity")).intValue() == 0).count());

        dash.put("activeEmployees", employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase((String) e.get("status"))).count());
        dash.put("pendingPayroll", payrols.stream().filter(p -> "PENDING".equalsIgnoreCase((String) p.get("status"))).count());
        dash.put("pendingPOs", pos.stream().filter(p -> "PENDING".equalsIgnoreCase((String) p.get("status"))).count());

        BigDecimal totalRevenue = orders.stream()
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dash.put("totalRevenue", totalRevenue);

        return dash;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchList(String url) {
        try {
            return rest.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
        } catch (Exception e) {
            return List.of();
        }
    }
}
