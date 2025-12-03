package id.my.hendisantika.dualdbdemo.config.properties;

import lombok.Data;

import java.time.Duration;

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
public class PoolProperties {
    private int initialSize = 5;
    private int maxSize = 20;
    private Duration maxIdleTime = Duration.ofMinutes(30);
    private Duration maxLifeTime = Duration.ofMinutes(60);
    private String validationQuery = "SELECT 1";
}
