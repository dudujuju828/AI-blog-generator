package com.maxthomarino.res.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxthomarino.res.model.BlogPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GitHubService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String repo;

    public GitHubService(@Value("${github.token}") String token,
                         @Value("${github.repo}") String repo,
                         ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String commitPost(BlogPost post) {
        String mdxContent = buildMdx(post);
        String encoded = Base64.getEncoder().encodeToString(mdxContent.getBytes());
        String path = "src/content/blog/" + post.slug() + ".mdx";

        Map<String, Object> requestBody = Map.of(
                "message", "Add blog post: " + post.title(),
                "content", encoded
        );

        String responseBody = restClient.put()
                .uri("/repos/{repo}/contents/{path}", repo, path)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("commit").path("html_url").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GitHub response", e);
        }
    }

    private String buildMdx(BlogPost post) {
        String tagsFormatted = post.tags().stream()
                .map(tag -> "\"" + tag + "\"")
                .collect(Collectors.joining(", "));

        return """
                ---
                title: "%s"
                date: "%s"
                description: "%s"
                tags: [%s]
                ---

                %s
                """.formatted(post.title(), post.date(), post.description(), tagsFormatted, post.content());
    }
}
