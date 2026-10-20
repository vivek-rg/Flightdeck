package io.blackbox.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BlackboxBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlackboxBackendApplication.class, args);
    }

    @Bean
    public RestTemplate backendRestTemplate() {
        return new RestTemplate();
    }
}
