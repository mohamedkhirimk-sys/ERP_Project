package com.erp.system.product.service;

import com.erp.system.product.dto.ProductRequest;
import com.erp.system.product.entity.Product;
import java.util.List;

public interface ProductService {
    Product createProduct(ProductRequest request);
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
}