package id.my.hendisantika.dualdbdemo.service;

import id.my.hendisantika.dualdbdemo.context.RequestContext;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

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

    /**
     * Get all products from both databases using Structured Concurrency (JDK 25+)
     * combined with Scoped Values for request context propagation.
     * <p>
     * Features demonstrated:
     * - StructuredTaskScope: Manages concurrent subtasks with automatic cleanup
     * - ScopedValue: Propagates correlation ID to all forked subtasks
     * - Virtual Threads: Subtasks run on lightweight virtual threads
     */
    public List<ProductResponse> getAllProductsFromBothDatabases() {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        // Use ScopedValue to propagate context to subtasks
        return ScopedValue.where(RequestContext.CORRELATION_ID, correlationId)
                .where(RequestContext.OPERATION, "getAllProductsFromBothDatabases")
                .call(() -> {
                    log.info("[{}] Starting parallel fetch from both databases", RequestContext.getCorrelationId());

                    try (var scope = StructuredTaskScope.open()) {
                        // Fork both database queries - they inherit the ScopedValue context
                        var mysqlTask = scope.fork(() -> {
                            log.debug("[{}] Fetching from MySQL", RequestContext.getCorrelationId());
                            return getAllMysqlProducts();
                        });
                        var postgresTask = scope.fork(() -> {
                            log.debug("[{}] Fetching from PostgreSQL", RequestContext.getCorrelationId());
                            return getAllPostgresProducts();
                        });

                        // Wait for all subtasks to complete (or one to fail)
                        scope.join();

                        // Merge results from both databases
                        log.info("[{}] Successfully fetched products from both databases", RequestContext.getCorrelationId());
                        return Stream.concat(
                                mysqlTask.get().stream(),
                                postgresTask.get().stream()
                        ).toList();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("[{}] Interrupted while fetching products", RequestContext.getCorrelationId(), e);
                        throw new RuntimeException("Interrupted while fetching products", e);
                    } catch (StructuredTaskScope.FailedException e) {
                        log.error("[{}] A subtask failed while fetching products", RequestContext.getCorrelationId(), e.getCause());
                        throw new RuntimeException("Failed to fetch products from both databases", e.getCause());
                    }
                });
    }

    /**
     * Sync product to both databases using Structured Concurrency (JDK 25+)
     * combined with Scoped Values for request context propagation.
     *
     * Features demonstrated:
     * - StructuredTaskScope: Manages concurrent subtasks with fail-fast behavior
     * - ScopedValue: Propagates correlation ID and operation context to subtasks
     * - Virtual Threads: Subtasks run on lightweight virtual threads
     */
    public void syncProductToBothDatabases(ProductRequest request) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        // Use ScopedValue to propagate context to subtasks
        ScopedValue.where(RequestContext.CORRELATION_ID, correlationId)
                .where(RequestContext.OPERATION, "syncProductToBothDatabases")
                .run(() -> {
                    log.info("[{}] Starting parallel sync to both databases for product: {}",
                            RequestContext.getCorrelationId(), request.getName());

                    try (var scope = StructuredTaskScope.open()) {
                        // Fork both insert operations - they inherit the ScopedValue context
                        scope.fork(() -> {
                            log.debug("[{}] Creating product in MySQL", RequestContext.getCorrelationId());
                            return createMysqlProduct(request);
                        });
                        scope.fork(() -> {
                            log.debug("[{}] Creating product in PostgreSQL", RequestContext.getCorrelationId());
                            return createPostgresProduct(request);
                        });

                        // Wait for both to complete (or one to fail)
                        scope.join();

                        log.info("[{}] Successfully synced product to both databases: {}",
                                RequestContext.getCorrelationId(), request.getName());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("[{}] Interrupted while syncing product", RequestContext.getCorrelationId(), e);
                        throw new RuntimeException("Interrupted while syncing product", e);
                    } catch (StructuredTaskScope.FailedException e) {
                        log.error("[{}] A subtask failed while syncing product", RequestContext.getCorrelationId(), e.getCause());
                        throw new RuntimeException("Failed to sync product to both databases", e.getCause());
                    }
                });
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
