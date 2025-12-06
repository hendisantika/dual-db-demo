package id.my.hendisantika.dualdbdemo.config.properties;

import lombok.Data;

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
@Data
public class HikariPoolProperties {
    private int minimumIdle = 5;
    private int maximumPoolSize = 20;
    private long idleTimeout = 30000;
    private long maxLifetime = 1800000;
    private long connectionTimeout = 30000;
    private long validationTimeout = 5000;
}
