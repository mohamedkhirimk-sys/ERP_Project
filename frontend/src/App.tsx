import { BrowserRouter, Routes, Route } from 'react-router-dom'
import ProtectedRoute from '@/components/ProtectedRoute'
import Layout from '@/components/Layout'
import LoginPage from '@/features/auth/LoginPage'
import DashboardPage from '@/features/dashboard/DashboardPage'
import ProductListPage from '@/features/products/ProductListPage'
import ProductFormPage from '@/features/products/ProductFormPage'
import StockListPage from '@/features/inventory/StockListPage'
import OrderListPage from '@/features/orders/OrderListPage'
import CustomerListPage from '@/features/customers/CustomerListPage'
import InvoiceListPage from '@/features/invoices/InvoiceListPage'
import VendorListPage from '@/features/vendors/VendorListPage'
import PurchaseOrderListPage from '@/features/purchase-orders/PurchaseOrderListPage'
import EmployeeListPage from '@/features/employees/EmployeeListPage'
import AccountListPage from '@/features/accounts/AccountListPage'
import CreateOrderPage from '@/features/orders/CreateOrderPage'
import CreateGrnPage from '@/features/goods-received/CreateGrnPage'
import CreatePurchaseOrderPage from '@/features/purchase-orders/CreatePurchaseOrderPage'
import CreateInvoicePage from '@/features/invoices/CreateInvoicePage'
import PayrollListPage from '@/features/payroll/PayrollListPage'
import JournalEntryListPage from '@/features/journal/JournalEntryListPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/products" element={<ProductListPage />} />
            <Route path="/products/new" element={<ProductFormPage />} />
            <Route path="/products/:id/edit" element={<ProductFormPage />} />
            <Route path="/inventory" element={<StockListPage />} />
            <Route path="/orders" element={<OrderListPage />} />
            <Route path="/orders/new" element={<CreateOrderPage />} />
            <Route path="/goods-received/new" element={<CreateGrnPage />} />
            <Route path="/customers" element={<CustomerListPage />} />
            <Route path="/invoices" element={<InvoiceListPage />} />
            <Route path="/invoices/new" element={<CreateInvoicePage />} />
            <Route path="/vendors" element={<VendorListPage />} />
            <Route path="/purchase-orders" element={<PurchaseOrderListPage />} />
            <Route path="/purchase-orders/new" element={<CreatePurchaseOrderPage />} />
            <Route path="/employees" element={<EmployeeListPage />} />
            <Route path="/accounts" element={<AccountListPage />} />
            <Route path="/journal" element={<JournalEntryListPage />} />
            <Route path="/payroll" element={<PayrollListPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
