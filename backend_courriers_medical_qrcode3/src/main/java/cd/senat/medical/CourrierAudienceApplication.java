package cd.senat.medical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import cd.senat.medical.config.OpenAiProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenAiProperties.class)
public class CourrierAudienceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourrierAudienceApplication.class, args);
    }

}