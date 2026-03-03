package com.maxthomarino.res.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final String STYLE_PREFIX =
            "Create a simple, clean, minimal diagram. Use flat colors, clean lines, and a white background. " +
            "Suitable for a technical blog post. ";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public ImageService(@Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.model:gemini-2.5-flash-image}") String model,
                        ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Generates an image from the given prompt via Gemini API.
     * Returns base64-encoded PNG data, or null on any failure.
     */
    public String generateImage(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not set — skipping image generation");
            return null;
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", STYLE_PREFIX + prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "responseModalities", List.of("TEXT", "IMAGE")
                    )
            );

            String responseBody = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

            for (JsonNode part : parts) {
                JsonNode inlineData = part.path("inlineData");
                if (!inlineData.isMissingNode() && inlineData.has("data")) {
                    return inlineData.path("data").asText();
                }
            }

            log.warn("No image data found in Gemini response for prompt: {}", prompt);
            return null;
        } catch (Exception e) {
            log.error("Gemini image generation failed for prompt: {}", prompt, e);
            return null;
        }
    }
}
