package id.my.hendisantika.dualdbdemo.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;

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
@EnableR2dbcRepositories(
        basePackages = "id.my.hendisantika.dualdbdemo.repository.postgresql",
        entityOperationsRef = "postgresEntityOperations"
)
public class PostgresR2dbcConfig {

    @Value("${spring.r2dbc.postgresql.url}")
    private String url;

    @Value("${spring.r2dbc.postgresql.username}")
    private String username;

    @Value("${spring.r2dbc.postgresql.password}")
    private String password;

    @Value("${spring.r2dbc.postgresql.pool.initial-size:10}")
    private int initialSize;

    @Value("${spring.r2dbc.postgresql.pool.max-size:30}")
    private int maxSize;

    @Value("${spring.r2dbc.postgresql.pool.max-idle-time:30m}")
    private Duration maxIdleTime;

    @Bean
    @Qualifier("postgresConnectionFactory")
    public ConnectionFactory postgresConnectionFactory() {
        // Parse r2dbc:postgresql://localhost:5433/profile_http
        String cleanUrl = url.replace("r2dbc:postgresql://", "");
        String[] hostPortDb = cleanUrl.split("/");
        String[] hostPort = hostPortDb[0].split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
        String database = hostPortDb.length > 1 ? hostPortDb[1] : "test";

        ConnectionFactory connectionFactory = ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(DRIVER, "postgresql")
                        .option(HOST, host)
                        .option(PORT, port)
                        .option(USER, username)
                        .option(PASSWORD, password)
                        .option(DATABASE, database)
                        .build()
        );

        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                .initialSize(initialSize)
                .maxSize(maxSize)
                .maxIdleTime(maxIdleTime)
                .build();

        return new ConnectionPool(poolConfig);
    }

    @Bean
    @Qualifier("postgresDatabaseClient")
    public DatabaseClient postgresDatabaseClient(@Qualifier("postgresConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .build();
    }

    @Bean
    @Qualifier("postgresEntityOperations")
    public R2dbcEntityOperations postgresEntityOperations(@Qualifier("postgresDatabaseClient") DatabaseClient databaseClient) {
        return new R2dbcEntityTemplate(databaseClient, PostgresDialect.INSTANCE);
    }
}
