// src/main/java/fr/senat/courriersaudiences/config/OpenAiConfig.java
package cd.senat.medical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

  @Bean
  public WebClient openAiWebClient(
      @Value("${openai.api-key}") String apiKey,
      @Value("${openai.base-url}") String baseUrl,
      @Value("${openai.timeout-ms:60000}") long timeoutMs
  ) {
    // Buffer augmenté pour réponses volumineuses
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
        .build();

    return WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .exchangeStrategies(strategies)
        .build();
  }
}
