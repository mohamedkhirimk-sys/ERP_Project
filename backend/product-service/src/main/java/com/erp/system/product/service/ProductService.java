package com.erp.system.product.service;

import com.erp.system.product.dto.ProductRequest;
import com.erp.system.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {
    Product createProduct(ProductRequest request);
    Page<Product> getAllProducts(Pageable pageable);
    Product getProductById(Long id);
    Product getProductBySku(String sku);
    Product updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
}