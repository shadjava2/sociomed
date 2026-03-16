// src/main/java/fr/senat/courriersaudiences/service/ChatGptService.java
package cd.senat.medical.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import cd.senat.medical.config.OpenAiProperties;

@Service
public class ChatGptService {

  private final WebClient webClient;
  private final OpenAiProperties props;

  public ChatGptService(OpenAiProperties props, WebClient.Builder builder) {
    this.props = props;
    this.webClient = builder
      .baseUrl(props.getBaseUrl())
      .defaultHeader("Authorization", "Bearer " + props.getApiKey())
      .build();
  }

  public String ask(String prompt, Map<String, Object> context) {
    // Messages au format Chat Completions
    List<Map<String, Object>> messages = new ArrayList<>();

    String system = """
      Tu es l'assistant IA du Secrétariat Général. 
      Tu connais la gestion des courriers et audiences (priorités, états, stats).
      Réponds en français, de façon claire et actionnable.
      """;

    // Ajoute un contexte textuel compact si fourni (ex: chiffres résumés)
    String contextText = "";
    if (context != null && !context.isEmpty()) {
      contextText = "Contexte: " + context.toString();
    }

    messages.add(Map.of("role", "system", "content", system));
    if (!contextText.isBlank()) {
      messages.add(Map.of("role", "system", "content", contextText));
    }
    messages.add(Map.of("role", "user", "content", prompt));

    Map<String, Object> body = new HashMap<>();
    body.put("model", props.getModel());
    body.put("messages", messages);
    body.put("temperature", 0.2);

    try {
      Map<String, Object> response = webClient.post()
        .uri("/chat/completions") // -> base-url + /chat/completions
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class)
        .timeout(Duration.ofMillis(props.getTimeoutMs()))
        .block();

      // Extraction du texte
      if (response == null) return "Aucune réponse du modèle.";
      List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
      if (choices == null || choices.isEmpty()) return "Aucune réponse générée.";
      Map<String, Object> first = choices.get(0);
      Map<String, Object> msg = (Map<String, Object>) first.get("message");
      return (String) msg.getOrDefault("content", "Réponse vide.");
    } catch (WebClientResponseException e) {
      return "Erreur OpenAI (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
    } catch (Exception e) {
      return "Erreur d'appel OpenAI: " + e.getMessage();
    }
  }
}
