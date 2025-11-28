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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

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
    public Mono<ProductResponse> createMysqlProduct(ProductRequest request) {
        MysqlProduct product = MysqlProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mysqlProductRepository.save(product)
                .map(this::toMysqlResponse)
                .doOnSuccess(p -> log.info("Created MySQL product: {}", p.getId()));
    }

    public Flux<ProductResponse> getAllMysqlProducts() {
        return mysqlProductRepository.findAll()
                .map(this::toMysqlResponse);
    }

    public Mono<ProductResponse> getMysqlProductById(Long id) {
        return mysqlProductRepository.findById(id)
                .map(this::toMysqlResponse);
    }

    public Mono<ProductResponse> updateMysqlProduct(Long id, ProductRequest request) {
        return mysqlProductRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setPrice(request.getPrice());
                    existing.setQuantity(request.getQuantity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return mysqlProductRepository.save(existing);
                })
                .map(this::toMysqlResponse)
                .doOnSuccess(p -> log.info("Updated MySQL product: {}", id));
    }

    public Mono<Void> deleteMysqlProduct(Long id) {
        return mysqlProductRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Deleted MySQL product: {}", id));
    }

    public Flux<ProductResponse> searchMysqlProducts(String name) {
        return mysqlProductRepository.findByNameContainingIgnoreCase(name)
                .map(this::toMysqlResponse);
    }

    // PostgreSQL CRUD Operations
    public Mono<ProductResponse> createPostgresProduct(ProductRequest request) {
        PostgresProduct product = PostgresProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return postgresProductRepository.save(product)
                .map(this::toPostgresResponse)
                .doOnSuccess(p -> log.info("Created PostgreSQL product: {}", p.getId()));
    }

    public Flux<ProductResponse> getAllPostgresProducts() {
        return postgresProductRepository.findAll()
                .map(this::toPostgresResponse);
    }

    public Mono<ProductResponse> getPostgresProductById(Long id) {
        return postgresProductRepository.findById(id)
                .map(this::toPostgresResponse);
    }

    public Mono<ProductResponse> updatePostgresProduct(Long id, ProductRequest request) {
        return postgresProductRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setPrice(request.getPrice());
                    existing.setQuantity(request.getQuantity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return postgresProductRepository.save(existing);
                })
                .map(this::toPostgresResponse)
                .doOnSuccess(p -> log.info("Updated PostgreSQL product: {}", id));
    }

    public Mono<Void> deletePostgresProduct(Long id) {
        return postgresProductRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Deleted PostgreSQL product: {}", id));
    }

    public Flux<ProductResponse> searchPostgresProducts(String name) {
        return postgresProductRepository.findByNameContainingIgnoreCase(name)
                .map(this::toPostgresResponse);
    }

    // Get all products from both databases
    public Flux<ProductResponse> getAllProductsFromBothDatabases() {
        Flux<ProductResponse> mysqlProducts = getAllMysqlProducts();
        Flux<ProductResponse> postgresProducts = getAllPostgresProducts();
        return Flux.merge(mysqlProducts, postgresProducts);
    }

    // Sync product to both databases
    public Mono<Void> syncProductToBothDatabases(ProductRequest request) {
        return Mono.when(
                createMysqlProduct(request),
                createPostgresProduct(request)
        ).doOnSuccess(v -> log.info("Synced product to both databases: {}", request.getName()));
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
