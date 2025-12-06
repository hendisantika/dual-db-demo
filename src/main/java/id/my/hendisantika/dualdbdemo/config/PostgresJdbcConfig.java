package id.my.hendisantika.dualdbdemo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import id.my.hendisantika.dualdbdemo.config.properties.DatabaseHost;
import id.my.hendisantika.dualdbdemo.config.properties.PostgresProperties;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
@EnableConfigurationProperties(PostgresProperties.class)
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "id.my.hendisantika.dualdbdemo.repository.postgresql",
        entityManagerFactoryRef = "postgresEntityManagerFactory",
        transactionManagerRef = "postgresTransactionManager"
)
public class PostgresJdbcConfig {

    private static final Logger log = LoggerFactory.getLogger(PostgresJdbcConfig.class);

    private final PostgresProperties properties;

    public PostgresJdbcConfig(PostgresProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Qualifier("postgresDataSource")
    public DataSource postgresDataSource() {
        List<DatabaseHost> hosts = properties.getHosts();

        if (hosts.isEmpty()) {
            throw new IllegalStateException("No PostgreSQL hosts configured");
        }

        if (!properties.getFailover().isEnabled() || hosts.size() == 1) {
            log.info("PostgreSQL failover disabled or single host configured. Using host: {}:{}",
                    hosts.get(0).getHost(), hosts.get(0).getPort());
            return createHikariDataSource(hosts.get(0));
        }

        log.info("PostgreSQL failover enabled with {} hosts configured", hosts.size());
        for (int i = 0; i < hosts.size(); i++) {
            log.info("  Host {}: {}:{}", i, hosts.get(i).getHost(), hosts.get(i).getPort());
        }

        return new FailoverDataSource(hosts, properties);
    }

    private HikariDataSource createHikariDataSource(DatabaseHost host) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                host.getHost(), host.getPort(), properties.getDatabase()));
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName("org.postgresql.Driver");

        config.setMinimumIdle(properties.getPool().getMinimumIdle());
        config.setMaximumPoolSize(properties.getPool().getMaximumPoolSize());
        config.setIdleTimeout(properties.getPool().getIdleTimeout());
        config.setMaxLifetime(properties.getPool().getMaxLifetime());
        config.setConnectionTimeout(properties.getPool().getConnectionTimeout());
        config.setValidationTimeout(properties.getPool().getValidationTimeout());
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("PostgreSQL-HikariPool-" + host.getHost() + ":" + host.getPort());

        return new HikariDataSource(config);
    }

    @Bean
    @Qualifier("postgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(
            @Qualifier("postgresDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("id.my.hendisantika.dualdbdemo.entity.postgresql");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(true);
        vendorAdapter.setGenerateDdl(false);
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.format_sql", true);
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        em.setJpaPropertyMap(jpaProperties);

        return em;
    }

    @Bean
    @Qualifier("postgresTransactionManager")
    public PlatformTransactionManager postgresTransactionManager(
            @Qualifier("postgresEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * Failover-aware DataSource that switches between hosts on connection failure
     */
    private class FailoverDataSource implements DataSource {

        private final List<DatabaseHost> hosts;
        private final PostgresProperties props;
        private final AtomicInteger activeHostIndex = new AtomicInteger(0);
        private final ScheduledExecutorService healthChecker;
        private volatile HikariDataSource currentDataSource;

        FailoverDataSource(List<DatabaseHost> hosts, PostgresProperties props) {
            this.hosts = hosts;
            this.props = props;
            this.currentDataSource = createHikariDataSource(hosts.get(0));

            // Start health check scheduler
            this.healthChecker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PostgreSQL-HealthChecker");
                t.setDaemon(true);
                return t;
            });

            healthChecker.scheduleWithFixedDelay(
                    this::checkPrimaryHealth,
                    props.getFailover().getHealthCheckInterval(),
                    props.getFailover().getHealthCheckInterval(),
                    TimeUnit.MILLISECONDS
            );
        }

        private void checkPrimaryHealth() {
            // If we're not on primary, check if primary is back
            if (activeHostIndex.get() != 0) {
                DatabaseHost primaryHost = hosts.get(0);
                try (Connection conn = createHikariDataSource(primaryHost).getConnection()) {
                    if (conn.isValid(5)) {
                        log.info("PostgreSQL primary host {}:{} is back online, switching back",
                                primaryHost.getHost(), primaryHost.getPort());
                        switchToHost(0);
                    }
                } catch (SQLException e) {
                    log.debug("PostgreSQL primary host still unavailable: {}", e.getMessage());
                }
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            return getConnectionWithFailover();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnectionWithFailover();
        }

        private Connection getConnectionWithFailover() throws SQLException {
            int retries = 0;
            SQLException lastException = null;

            while (retries <= props.getFailover().getMaxRetries()) {
                try {
                    Connection conn = currentDataSource.getConnection();
                    if (conn.isValid(5)) {
                        return conn;
                    }
                } catch (SQLException e) {
                    lastException = e;
                    log.warn("PostgreSQL connection failed (attempt {}): {}", retries + 1, e.getMessage());

                    if (isConnectionError(e)) {
                        switchToNextHost();
                        try {
                            Thread.sleep(props.getFailover().getRetryDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new SQLException("Interrupted during failover retry", ie);
                        }
                    }
                }
                retries++;
            }

            log.error("PostgreSQL connection failed after all retries");
            throw lastException != null ? lastException :
                    new SQLException("Failed to obtain PostgreSQL connection after all retries");
        }

        private boolean isConnectionError(SQLException e) {
            String message = e.getMessage();
            return message != null && (
                    message.contains("Connection refused") ||
                            message.contains("Unable to connect") ||
                            message.contains("Connection timed out") ||
                            message.contains("FATAL") ||
                            e.getSQLState() != null && e.getSQLState().startsWith("08")
            );
        }

        private synchronized void switchToNextHost() {
            int nextIndex = (activeHostIndex.get() + 1) % hosts.size();
            switchToHost(nextIndex);
        }

        private synchronized void switchToHost(int index) {
            if (index != activeHostIndex.get()) {
                DatabaseHost nextHost = hosts.get(index);
                log.info("PostgreSQL switching to host: {}:{}", nextHost.getHost(), nextHost.getPort());

                HikariDataSource oldDataSource = currentDataSource;
                currentDataSource = createHikariDataSource(nextHost);
                activeHostIndex.set(index);

                // Close old datasource in background
                if (oldDataSource != null) {
                    try {
                        oldDataSource.close();
                    } catch (Exception e) {
                        log.warn("Error closing old PostgreSQL datasource: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return currentDataSource.getLogWriter();
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            currentDataSource.setLogWriter(out);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return currentDataSource.getLoginTimeout();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            currentDataSource.setLoginTimeout(seconds);
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return currentDataSource.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return currentDataSource.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return currentDataSource.isWrapperFor(iface);
        }
    }
}
