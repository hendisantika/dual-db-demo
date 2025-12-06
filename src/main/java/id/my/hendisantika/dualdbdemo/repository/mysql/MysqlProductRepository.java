package id.my.hendisantika.dualdbdemo.repository.mysql;

import id.my.hendisantika.dualdbdemo.entity.mysql.MysqlProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
@Repository
public interface MysqlProductRepository extends JpaRepository<MysqlProduct, Long> {

    List<MysqlProduct> findByNameContainingIgnoreCase(String name);
}
