package id.my.hendisantika.dualdbdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {"org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration"})
public class DualDbDemoApplication {

    static void main(String[] args) {
        SpringApplication.run(DualDbDemoApplication.class, args);
    }
}
