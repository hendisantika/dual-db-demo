package id.my.hendisantika.dualdbdemo.service;

import id.my.hendisantika.dualdbdemo.dto.ProductRequest;
import id.my.hendisantika.dualdbdemo.dto.ProductResponse;
import id.my.hendisantika.dualdbdemo.entity.mysql.MysqlProduct;
import id.my.hendisantika.dualdbdemo.entity.postgresql.PostgresProduct;
import id.my.hendisantika.dualdbdemo.repository.mysql.MysqlProductRepository;
import id.my.hendisantika.dualdbdemo.repository.postgresql.PostgresProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Created by IntelliJ IDEA.
 * Project : dual-db-demo
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 28/11/25
 * Time: 17.30
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final MysqlProductRepository mysqlProductRepository;
    private final PostgresProductRepository postgresProductRepository;

    // MySQL CRUD Operations
    @Transactional("mysqlTransactionManager")
    public ProductResponse createMysqlProduct(ProductRequest request) {
        MysqlProduct product = MysqlProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MysqlProduct saved = mysqlProductRepository.save(product);
        log.info("Created MySQL product: {}", saved.getId());
        return toMysqlResponse(saved);
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public List<ProductResponse> getAllMysqlProducts() {
        return mysqlProductRepository.findAll()
                .stream()
                .map(this::toMysqlResponse)
                .toList();
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public Optional<ProductResponse> getMysqlProductById(Long id) {
        return mysqlProductRepository.findById(id)
                .map(this::toMysqlResponse);
    }

    @Transactional("mysqlTransactionManager")
    public Optional<ProductResponse> updateMysqlProduct(Long id, ProductRequest request) {
        return mysqlProductRepository.findById(id)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setPrice(request.getPrice());
                    existing.setQuantity(request.getQuantity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    MysqlProduct updated = mysqlProductRepository.save(existing);
                    log.info("Updated MySQL product: {}", id);
                    return toMysqlResponse(updated);
                });
    }

    @Transactional("mysqlTransactionManager")
    public void deleteMysqlProduct(Long id) {
        mysqlProductRepository.deleteById(id);
        log.info("Deleted MySQL product: {}", id);
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public List<ProductResponse> searchMysqlProducts(String name) {
        return mysqlProductRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toMysqlResponse)
                .toList();
    }

    // PostgreSQL CRUD Operations
    @Transactional("postgresTransactionManager")
    public ProductResponse createPostgresProduct(ProductRequest request) {
        PostgresProduct product = PostgresProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PostgresProduct saved = postgresProductRepository.save(product);
        log.info("Created PostgreSQL product: {}", saved.getId());
        return toPostgresResponse(saved);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<ProductResponse> getAllPostgresProducts() {
        return postgresProductRepository.findAll()
                .stream()
                .map(this::toPostgresResponse)
                .toList();
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public Optional<ProductResponse> getPostgresProductById(Long id) {
        return postgresProductRepository.findById(id)
                .map(this::toPostgresResponse);
    }

    @Transactional("postgresTransactionManager")
    public Optional<ProductResponse> updatePostgresProduct(Long id, ProductRequest request) {
        return postgresProductRepository.findById(id)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setPrice(request.getPrice());
                    existing.setQuantity(request.getQuantity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    PostgresProduct updated = postgresProductRepository.save(existing);
                    log.info("Updated PostgreSQL product: {}", id);
                    return toPostgresResponse(updated);
                });
    }

    @Transactional("postgresTransactionManager")
    public void deletePostgresProduct(Long id) {
        postgresProductRepository.deleteById(id);
        log.info("Deleted PostgreSQL product: {}", id);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<ProductResponse> searchPostgresProducts(String name) {
        return postgresProductRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toPostgresResponse)
                .toList();
    }

    // Get all products from both databases
    public List<ProductResponse> getAllProductsFromBothDatabases() {
        // Execute both queries in parallel using CompletableFuture
        CompletableFuture<List<ProductResponse>> mysqlFuture = CompletableFuture.supplyAsync(this::getAllMysqlProducts);
        CompletableFuture<List<ProductResponse>> postgresFuture = CompletableFuture.supplyAsync(this::getAllPostgresProducts);

        // Wait for both to complete and merge results
        List<ProductResponse> allProducts = new ArrayList<>();
        try {
            allProducts.addAll(mysqlFuture.get());
            allProducts.addAll(postgresFuture.get());
        } catch (Exception e) {
            log.error("Error fetching products from both databases", e);
            throw new RuntimeException("Failed to fetch products from both databases", e);
        }

        return allProducts;
    }

    // Sync product to both databases
    public void syncProductToBothDatabases(ProductRequest request) {
        // Execute both inserts in parallel
        CompletableFuture<ProductResponse> mysqlFuture = CompletableFuture.supplyAsync(() -> createMysqlProduct(request));
        CompletableFuture<ProductResponse> postgresFuture = CompletableFuture.supplyAsync(() -> createPostgresProduct(request));

        try {
            CompletableFuture.allOf(mysqlFuture, postgresFuture).get();
            log.info("Synced product to both databases: {}", request.getName());
        } catch (Exception e) {
            log.error("Error syncing product to both databases", e);
            throw new RuntimeException("Failed to sync product to both databases", e);
        }
    }

    private ProductResponse toMysqlResponse(MysqlProduct product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .source("MySQL")
                .build();
    }

    private ProductResponse toPostgresResponse(PostgresProduct product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .source("PostgreSQL")
                .build();
    }
}
