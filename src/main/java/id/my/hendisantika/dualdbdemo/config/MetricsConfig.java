package id.my.hendisantika.dualdbdemo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * Project : dual-db-demo
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 27/01/26
 * Time: 10.00
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig implements WebMvcConfigurer {

    private final MeterRegistry meterRegistry;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiMetricsInterceptor(meterRegistry));
    }

    /**
     * Interceptor to count API hits and track response times
     */
    public static class ApiMetricsInterceptor implements HandlerInterceptor {
        private final MeterRegistry meterRegistry;

        public ApiMetricsInterceptor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute("startTime", System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                     Object handler, Exception ex) {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();

            // Calculate request duration
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                // Record request duration
                meterRegistry.timer("api.request.duration",
                        Arrays.asList(
                                Tag.of("uri", uri),
                                Tag.of("method", method),
                                Tag.of("status", String.valueOf(status))
                        )
                ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            // Count API hits
            meterRegistry.counter("api.request.count",
                    Arrays.asList(
                            Tag.of("uri", uri),
                            Tag.of("method", method),
                            Tag.of("status", String.valueOf(status))
                    )
            ).increment();

            // Count specific endpoints for easier tracking
            if (uri.startsWith("/api/products")) {
                meterRegistry.counter("api.products.hits",
                        Arrays.asList(
                                Tag.of("endpoint", uri),
                                Tag.of("method", method),
                                Tag.of("status", String.valueOf(status))
                        )
                ).increment();
            }

            log.debug("API Hit - URI: {}, Method: {}, Status: {}", uri, method, status);
        }
    }
}
