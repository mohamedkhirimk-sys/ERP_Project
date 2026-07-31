package com.erp.system.product.service.impl;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.product.dto.ProductRequest;
import com.erp.system.product.entity.PriceHistory;
import com.erp.system.product.entity.Product;
import com.erp.system.product.repository.ProductRepository;
import com.erp.system.product.service.ProductService;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Product with SKU " + request.getSku() + " already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .build();

        return productRepository.save(product);
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", sku));
    }

    @Override
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = getProductById(id);

        if (request.getPrice().compareTo(product.getPrice()) != 0) {
            PriceHistory history = PriceHistory.builder()
                    .product(product)
                    .oldPrice(product.getPrice())
                    .newPrice(request.getPrice())
                    .build();
            product.getPriceHistory().add(history);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : product.getStockQuantity());

        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}