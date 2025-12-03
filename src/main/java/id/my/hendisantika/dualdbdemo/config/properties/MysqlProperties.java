package id.my.hendisantika.dualdbdemo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
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
@Data
@ConfigurationProperties(prefix = "spring.r2dbc.mysql")
public class MysqlProperties {
    private List<DatabaseHost> hosts = new ArrayList<>();
    private String database;
    private String username;
    private String password;
    private PoolProperties pool = new PoolProperties();
    private FailoverProperties failover = new FailoverProperties();
}
