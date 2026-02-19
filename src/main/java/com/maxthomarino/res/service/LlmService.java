package com.maxthomarino.res.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxthomarino.res.model.BlogPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public LlmService(@Value("${openai.api-key}") String apiKey,
                       @Value("${openai.model:gpt-4o}") String model,
                       ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public BlogPost generatePost(String topic) {
        String systemPrompt = """
                You are a technical blog writer. Given a topic, write a blog post and return ONLY valid JSON with this structure:
                {
                  "title": "Post Title",
                  "description": "A short 1-2 sentence description",
                  "tags": ["Tag1", "Tag2", "Tag3"],
                  "content": "The full blog post content in markdown format. Use ## for subheadings. Do NOT include the title as an h1."
                }
                Do not wrap the JSON in markdown code fences. Return raw JSON only.""";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Write a blog post about: " + topic)
                ),
                "temperature", 0.7
        );

        String responseBody = restClient.post()
                .uri("/v1/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode blog = objectMapper.readTree(content);

            String title = blog.path("title").asText();
            String slug = title.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            String date = LocalDate.now().toString();

            List<String> tags = new java.util.ArrayList<>();
            blog.path("tags").forEach(tag -> tags.add(tag.asText()));

            return new BlogPost(
                    title,
                    date,
                    blog.path("description").asText(),
                    tags,
                    blog.path("content").asText(),
                    slug
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
}
