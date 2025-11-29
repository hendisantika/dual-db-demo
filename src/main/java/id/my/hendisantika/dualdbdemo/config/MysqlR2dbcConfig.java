package id.my.hendisantika.dualdbdemo.config;

import id.my.hendisantika.dualdbdemo.config.properties.DatabaseHost;
import id.my.hendisantika.dualdbdemo.config.properties.MysqlProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

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

@Configuration
@EnableConfigurationProperties(MysqlProperties.class)
@EnableR2dbcRepositories(
        basePackages = "id.my.hendisantika.dualdbdemo.repository.mysql",
        entityOperationsRef = "mysqlEntityOperations"
)
public class MysqlR2dbcConfig {

    private static final Logger log = LoggerFactory.getLogger(MysqlR2dbcConfig.class);

    private final MysqlProperties properties;
    private final AtomicInteger currentHostIndex = new AtomicInteger(0);

    public MysqlR2dbcConfig(MysqlProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @Qualifier("mysqlConnectionFactory")
    public ConnectionFactory mysqlConnectionFactory() {
        List<DatabaseHost> hosts = properties.getHosts();

        if (hosts.isEmpty()) {
            throw new IllegalStateException("No MySQL hosts configured");
        }

        // Create connection pool for primary host
        ConnectionPool primaryPool = createConnectionPool(hosts.get(0));

        if (!properties.getFailover().isEnabled() || hosts.size() == 1) {
            log.info("MySQL failover disabled or single host configured. Using host: {}:{}",
                    hosts.get(0).getHost(), hosts.get(0).getPort());
            return primaryPool;
        }

        log.info("MySQL failover enabled with {} hosts configured", hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            log.info("  Host {}: {}:{}", i, hosts.get(i).getHost(), hosts.get(i).getPort());
        }

        // Return a failover-aware connection factory wrapper
        return new FailoverConnectionFactory(hosts, properties);
    }

    private ConnectionPool createConnectionPool(DatabaseHost host) {
        ConnectionFactory connectionFactory = ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(DRIVER, "mysql")
                        .option(HOST, host.getHost())
                        .option(PORT, host.getPort())
                        .option(USER, properties.getUsername())
                        .option(PASSWORD, properties.getPassword())
                        .option(DATABASE, properties.getDatabase())
                        .build()
        );

        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                .initialSize(properties.getPool().getInitialSize())
                .maxSize(properties.getPool().getMaxSize())
                .maxIdleTime(properties.getPool().getMaxIdleTime())
                .maxLifeTime(properties.getPool().getMaxLifeTime())
                .validationQuery(properties.getPool().getValidationQuery())
                .build();

        return new ConnectionPool(poolConfig);
    }

    @Bean
    @Primary
    @Qualifier("mysqlDatabaseClient")
    public DatabaseClient mysqlDatabaseClient(@Qualifier("mysqlConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .build();
    }

    @Bean
    @Primary
    @Qualifier("mysqlEntityOperations")
    public R2dbcEntityOperations mysqlEntityOperations(@Qualifier("mysqlDatabaseClient") DatabaseClient databaseClient) {
        return new R2dbcEntityTemplate(databaseClient, MySqlDialect.INSTANCE);
    }

    /**
     * Failover-aware ConnectionFactory that tries multiple hosts on connection failure
     */
    private class FailoverConnectionFactory implements ConnectionFactory {

        private final List<DatabaseHost> hosts;
        private final MysqlProperties props;
        private final AtomicInteger activeHostIndex = new AtomicInteger(0);
        private volatile ConnectionPool currentPool;

        FailoverConnectionFactory(List<DatabaseHost> hosts, MysqlProperties props) {
            this.hosts = hosts;
            this.props = props;
            this.currentPool = createConnectionPool(hosts.get(0));
        }

        @Override
        public Mono<? extends io.r2dbc.spi.Connection> create() {
            return Mono.defer(() -> currentPool.create())
                    .retryWhen(Retry.fixedDelay(props.getFailover().getMaxRetries(),
                                    Duration.ofMillis(props.getFailover().getRetryDelay()))
                            .filter(this::isConnectionError)
                            .doBeforeRetry(signal -> {
                                log.warn("MySQL connection failed, attempting failover. Retry: {}", signal.totalRetries() + 1);
                                switchToNextHost();
                            }))
                    .doOnError(e -> log.error("MySQL connection failed after all retries", e));
        }

        private boolean isConnectionError(Throwable throwable) {
            return throwable instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                    || throwable.getCause() instanceof java.net.ConnectException
                    || throwable.getMessage() != null && throwable.getMessage().contains("Connection refused");
        }

        private synchronized void switchToNextHost() {
            int nextIndex = (activeHostIndex.get() + 1) % hosts.size();
            if (nextIndex != activeHostIndex.get()) {
                DatabaseHost nextHost = hosts.get(nextIndex);
                log.info("MySQL switching to host: {}:{}", nextHost.getHost(), nextHost.getPort());

                // Close old pool and create new one
                if (currentPool != null) {
                    currentPool.dispose();
                }
                currentPool = createConnectionPool(nextHost);
                activeHostIndex.set(nextIndex);
            }
        }

        @Override
        public ConnectionFactoryMetadata getMetadata() {
            return currentPool.getMetadata();
        }
    }
}
