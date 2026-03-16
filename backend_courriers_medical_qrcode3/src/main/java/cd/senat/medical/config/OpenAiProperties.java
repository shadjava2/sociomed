// src/main/java/fr/senat/courriersaudiences/config/OpenAiProperties.java
package cd.senat.medical.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
  private String apiKey;
  private String baseUrl;
  private String model;
  private int timeoutMs = 20000;

  // getters/setters
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
  public int getTimeoutMs() { return timeoutMs; }
  public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
