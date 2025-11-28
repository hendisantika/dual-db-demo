package id.my.hendisantika.dualdbdemo.repository.postgresql;

import id.my.hendisantika.dualdbdemo.entity.postgresql.PostgresProduct;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

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
@Repository
public interface PostgresProductRepository extends R2dbcRepository<PostgresProduct, Long> {

    Flux<PostgresProduct> findByNameContainingIgnoreCase(String name);
}
