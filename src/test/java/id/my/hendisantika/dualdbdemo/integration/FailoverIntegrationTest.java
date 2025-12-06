package id.my.hendisantika.dualdbdemo.integration;

import id.my.hendisantika.dualdbdemo.dto.ProductRequest;
import id.my.hendisantika.dualdbdemo.dto.ProductResponse;
import id.my.hendisantika.dualdbdemo.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the dual database demo application.
 * These tests require both MySQL and PostgreSQL databases to be running.
 * <p>
 * To test failover scenarios:
 * 1. Start all database containers: docker compose up -d
 * 2. Run these tests
 * 3. To test MySQL failover: docker compose stop mysql-primary
 * 4. Run the tests again - they should succeed using mysql-secondary
 * 5. To test PostgreSQL failover: docker compose stop postgres-primary
 * 6. Run the tests again - they should succeed using postgres-secondary
 */
@SpringBootTest
@ActiveProfiles("test")
class FailoverIntegrationTest {

    @Autowired
    private ProductService productService;

    private ProductRequest testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new ProductRequest();
        testProduct.setName("Test Product " + System.currentTimeMillis());
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setQuantity(10);
    }

    @Test
    @DisplayName("Should create product in MySQL database")
    void shouldCreateMysqlProduct() {
        ProductResponse response = productService.createMysqlProduct(testProduct);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testProduct.getName(), response.getName());
        assertEquals(testProduct.getDescription(), response.getDescription());
        assertEquals(0, testProduct.getPrice().compareTo(response.getPrice()));
        assertEquals(testProduct.getQuantity(), response.getQuantity());
        assertEquals("MySQL", response.getSource());
    }

    @Test
    @DisplayName("Should create product in PostgreSQL database")
    void shouldCreatePostgresProduct() {
        ProductResponse response = productService.createPostgresProduct(testProduct);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testProduct.getName(), response.getName());
        assertEquals(testProduct.getDescription(), response.getDescription());
        assertEquals(0, testProduct.getPrice().compareTo(response.getPrice()));
        assertEquals(testProduct.getQuantity(), response.getQuantity());
        assertEquals("PostgreSQL", response.getSource());
    }

    @Test
    @DisplayName("Should get all products from MySQL database")
    void shouldGetAllMysqlProducts() {
        // Create a product first
        productService.createMysqlProduct(testProduct);

        List<ProductResponse> products = productService.getAllMysqlProducts();

        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertTrue(products.stream().allMatch(p -> "MySQL".equals(p.getSource())));
    }

    @Test
    @DisplayName("Should get all products from PostgreSQL database")
    void shouldGetAllPostgresProducts() {
        // Create a product first
        productService.createPostgresProduct(testProduct);

        List<ProductResponse> products = productService.getAllPostgresProducts();

        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertTrue(products.stream().allMatch(p -> "PostgreSQL".equals(p.getSource())));
    }

    @Test
    @DisplayName("Should get products from both databases")
    void shouldGetAllProductsFromBothDatabases() {
        // Create products in both databases
        productService.createMysqlProduct(testProduct);
        productService.createPostgresProduct(testProduct);

        List<ProductResponse> products = productService.getAllProductsFromBothDatabases();

        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertTrue(products.stream().anyMatch(p -> "MySQL".equals(p.getSource())));
        assertTrue(products.stream().anyMatch(p -> "PostgreSQL".equals(p.getSource())));
    }

    @Test
    @DisplayName("Should sync product to both databases")
    void shouldSyncProductToBothDatabases() {
        String uniqueName = "Sync Test " + System.currentTimeMillis();
        testProduct.setName(uniqueName);

        productService.syncProductToBothDatabases(testProduct);

        // Verify product exists in both databases
        List<ProductResponse> mysqlProducts = productService.searchMysqlProducts(uniqueName);
        List<ProductResponse> postgresProducts = productService.searchPostgresProducts(uniqueName);

        assertFalse(mysqlProducts.isEmpty(), "Product should exist in MySQL");
        assertFalse(postgresProducts.isEmpty(), "Product should exist in PostgreSQL");
    }

    @Test
    @DisplayName("Should search products by name in MySQL")
    void shouldSearchMysqlProductsByName() {
        String uniqueName = "SearchTest MySQL " + System.currentTimeMillis();
        testProduct.setName(uniqueName);
        productService.createMysqlProduct(testProduct);

        List<ProductResponse> results = productService.searchMysqlProducts("SearchTest MySQL");

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("SearchTest MySQL")));
    }

    @Test
    @DisplayName("Should search products by name in PostgreSQL")
    void shouldSearchPostgresProductsByName() {
        String uniqueName = "SearchTest PostgreSQL " + System.currentTimeMillis();
        testProduct.setName(uniqueName);
        productService.createPostgresProduct(testProduct);

        List<ProductResponse> results = productService.searchPostgresProducts("SearchTest PostgreSQL");

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("SearchTest PostgreSQL")));
    }

    @Test
    @DisplayName("Should update product in MySQL")
    void shouldUpdateMysqlProduct() {
        ProductResponse created = productService.createMysqlProduct(testProduct);

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated Description");
        updateRequest.setPrice(new BigDecimal("199.99"));
        updateRequest.setQuantity(20);

        var updated = productService.updateMysqlProduct(created.getId(), updateRequest);

        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
        assertEquals("Updated Description", updated.get().getDescription());
        assertEquals(0, new BigDecimal("199.99").compareTo(updated.get().getPrice()));
        assertEquals(20, updated.get().getQuantity());
    }

    @Test
    @DisplayName("Should update product in PostgreSQL")
    void shouldUpdatePostgresProduct() {
        ProductResponse created = productService.createPostgresProduct(testProduct);

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated Description");
        updateRequest.setPrice(new BigDecimal("199.99"));
        updateRequest.setQuantity(20);

        var updated = productService.updatePostgresProduct(created.getId(), updateRequest);

        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
        assertEquals("Updated Description", updated.get().getDescription());
        assertEquals(0, new BigDecimal("199.99").compareTo(updated.get().getPrice()));
        assertEquals(20, updated.get().getQuantity());
    }

    @Test
    @DisplayName("Should delete product from MySQL")
    void shouldDeleteMysqlProduct() {
        ProductResponse created = productService.createMysqlProduct(testProduct);

        productService.deleteMysqlProduct(created.getId());

        var result = productService.getMysqlProductById(created.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should delete product from PostgreSQL")
    void shouldDeletePostgresProduct() {
        ProductResponse created = productService.createPostgresProduct(testProduct);

        productService.deletePostgresProduct(created.getId());

        var result = productService.getPostgresProductById(created.getId());
        assertTrue(result.isEmpty());
    }
}
