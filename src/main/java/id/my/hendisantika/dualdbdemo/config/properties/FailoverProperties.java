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
public class FailoverProperties {
    private boolean enabled = false;
    private int maxRetries = 3;
    private long retryDelay = 1000;
}
