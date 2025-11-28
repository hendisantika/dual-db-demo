package id.my.hendisantika.dualdbdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;

@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class})
public class DualDbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DualDbDemoApplication.class, args);
    }
}
