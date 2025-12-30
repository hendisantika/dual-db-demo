package id.my.hendisantika.dualdbdemo;

import id.my.hendisantika.dualdbdemo.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class DualDbDemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
