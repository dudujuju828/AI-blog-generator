package com.maxthomarino.res.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxthomarino.res.model.BlogPost;
import com.maxthomarino.res.model.ImagePlacement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
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

    private String chatCompletion(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
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
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private static String imageInstruction(int imageCount) {
        return """

                Additionally, identify up to %d places in the post where a simple diagram or illustration would help \
                the reader understand the concept. For each, embed a placeholder on its own line in the content \
                using the format {{IMAGE_1}}, {{IMAGE_2}}, etc.

                Include an "images" array in your JSON with objects for each placeholder:
                {
                  "placeholder": "{{IMAGE_1}}",
                  "prompt": "A description of the diagram to generate",
                  "alt": "Alt text for the image"
                }

                If no diagrams are appropriate, return an empty "images" array.""".formatted(imageCount);
    }

    public BlogPost generatePost(String topic, boolean withImages, int imageCount, List<String> resolvedResources) {
        String systemPrompt = """
                You are a technical blog writer. Given a topic, write a blog post and return ONLY valid JSON with this structure:
                {
                  "title": "Post Title",
                  "description": "A short 1-2 sentence description",
                  "tags": ["Tag1", "Tag2", "Tag3"],
                  "content": "The full blog post content in markdown format. Use ## for subheadings. Do NOT include the title as an h1."
                }
                Do not wrap the JSON in markdown code fences. Return raw JSON only.
                The output will be rendered as MDX with KaTeX support. You may use LaTeX math notation \
                (\\( \\) for inline, \\[ \\] for display) for formulas.""";

        if (withImages) {
            systemPrompt += imageInstruction(imageCount);
        }

        systemPrompt += resourceBlock(resolvedResources);

        String content = chatCompletion(systemPrompt, "Write a blog post about: " + topic);

        try {
            JsonNode blog = objectMapper.readTree(content);

            String title = blog.path("title").asText();
            String slug = title.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            String date = LocalDate.now().toString();

            List<String> tags = new ArrayList<>();
            blog.path("tags").forEach(tag -> tags.add(tag.asText()));

            List<ImagePlacement> images = parseImages(blog);

            return new BlogPost(
                    title,
                    date,
                    blog.path("description").asText(),
                    tags,
                    blog.path("content").asText(),
                    slug,
                    images
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    public String reviewPost(BlogPost post) {
        String systemPrompt = """
                You are a senior technical editor. Review the following blog post and provide actionable feedback. \
                Cover structure, technical depth, clarity, engagement, and flow. \
                Return ONLY a concise bullet-point list of specific improvements. Do not rewrite the post.""";

        String userMessage = "Title: " + post.title() + "\n\n" + post.content();
        return chatCompletion(systemPrompt, userMessage);
    }

    public BlogPost revisePost(BlogPost post, String feedback, boolean withImages, int imageCount,
                               List<String> resolvedResources) {
        String systemPrompt = """
                You are a technical blog writer. You will receive a blog post and editorial feedback. \
                Revise the post to address the feedback while preserving the original topic and intent. \
                Return ONLY valid JSON with this structure:
                {
                  "title": "Post Title",
                  "description": "A short 1-2 sentence description",
                  "tags": ["Tag1", "Tag2", "Tag3"],
                  "content": "The full blog post content in markdown format. Use ## for subheadings. Do NOT include the title as an h1."
                }
                Do not wrap the JSON in markdown code fences. Return raw JSON only.
                The output will be rendered as MDX with KaTeX support. You may use LaTeX math notation \
                (\\( \\) for inline, \\[ \\] for display) for formulas.""";

        if (withImages) {
            systemPrompt += """

                    The post may contain image placeholders like {{IMAGE_1}}, {{IMAGE_2}}, etc. \
                    Preserve these placeholders in the content at appropriate locations. \
                    Also preserve the "images" array from the original post in your JSON output.""" + imageInstruction(imageCount);
        }

        systemPrompt += resourceBlock(resolvedResources);

        String userMessage = "## Current Post\nTitle: " + post.title() + "\n\n" + post.content()
                + "\n\n## Feedback\n" + feedback;

        String content = chatCompletion(systemPrompt, userMessage);

        try {
            JsonNode blog = objectMapper.readTree(content);

            List<String> tags = new ArrayList<>();
            blog.path("tags").forEach(tag -> tags.add(tag.asText()));

            List<ImagePlacement> images = withImages ? parseImages(blog) : List.of();

            return new BlogPost(
                    blog.path("title").asText(),
                    post.date(),
                    blog.path("description").asText(),
                    tags,
                    blog.path("content").asText(),
                    post.slug(),
                    images
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private static String resourceBlock(List<String> resolvedResources) {
        if (resolvedResources == null || resolvedResources.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nYou may reference the following resources if relevant (do not fabricate citations):");
        for (String resource : resolvedResources) {
            sb.append("\n---\n").append(resource);
        }
        sb.append("\n---");
        return sb.toString();
    }

    private List<ImagePlacement> parseImages(JsonNode blog) {
        List<ImagePlacement> images = new ArrayList<>();
        JsonNode imagesNode = blog.path("images");
        if (imagesNode.isArray()) {
            for (JsonNode img : imagesNode) {
                images.add(new ImagePlacement(
                        img.path("placeholder").asText(),
                        img.path("prompt").asText(),
                        img.path("alt").asText()
                ));
            }
        }
        return images;
    }
}
