package iuh.fit.se;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "iuh.fit.se.repository.httpclient")
public class ChatAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatAiServiceApplication.class, args);
    }

}
