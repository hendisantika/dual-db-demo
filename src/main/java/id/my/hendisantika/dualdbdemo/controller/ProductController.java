package id.my.hendisantika.dualdbdemo.controller;

import id.my.hendisantika.dualdbdemo.dto.ProductRequest;
import id.my.hendisantika.dualdbdemo.dto.ProductResponse;
import id.my.hendisantika.dualdbdemo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/all")
    public List<ProductResponse> getAllProductsFromBothDatabases() {
        return productService.getAllProductsFromBothDatabases();
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.CREATED)
    public void syncProductToBothDatabases(@RequestBody ProductRequest request) {
        productService.syncProductToBothDatabases(request);
    }
}
