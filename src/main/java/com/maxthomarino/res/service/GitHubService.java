package com.maxthomarino.res.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxthomarino.res.model.BlogPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
        return commitAll(post, Map.of());
    }

    public String commitAll(BlogPost post, Map<String, byte[]> images) {
        try {
            String branch = "master";
            String headSha = getHeadSha(branch);
            String baseTreeSha = getCommitTreeSha(headSha);

            List<Map<String, String>> treeEntries = new ArrayList<>();

            // Add MDX file
            String mdxContent = buildMdx(post);
            String mdxBlobSha = createBlob(mdxContent, "utf-8");
            treeEntries.add(Map.of(
                    "path", "src/content/blog/" + post.slug() + ".mdx",
                    "mode", "100644",
                    "type", "blob",
                    "sha", mdxBlobSha
            ));

            // Add image files
            for (var entry : images.entrySet()) {
                String base64 = Base64.getEncoder().encodeToString(entry.getValue());
                String imageBlobSha = createBlob(base64, "base64");
                treeEntries.add(Map.of(
                        "path", "public/blog-images/" + entry.getKey(),
                        "mode", "100644",
                        "type", "blob",
                        "sha", imageBlobSha
                ));
            }

            String newTreeSha = createTree(baseTreeSha, treeEntries);
            String message = "Add blog post: " + post.title();
            String newCommitSha = createCommit(message, newTreeSha, headSha);
            updateRef(branch, newCommitSha);

            return "https://github.com/" + repo + "/commit/" + newCommitSha;
        } catch (Exception e) {
            throw new RuntimeException("Failed to commit to GitHub: " + e.getMessage(), e);
        }
    }

    private String getHeadSha(String branch) throws Exception {
        String body = restClient.get()
                .uri("/repos/" + repo + "/git/ref/heads/" + branch)
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body).path("object").path("sha").asText();
    }

    private String getCommitTreeSha(String commitSha) throws Exception {
        String body = restClient.get()
                .uri("/repos/" + repo + "/git/commits/" + commitSha)
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body).path("tree").path("sha").asText();
    }

    private String createBlob(String content, String encoding) throws Exception {
        String body = restClient.post()
                .uri("/repos/" + repo + "/git/blobs")
                .body(Map.of("content", content, "encoding", encoding))
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body).path("sha").asText();
    }

    private String createTree(String baseTreeSha, List<Map<String, String>> entries) throws Exception {
        String body = restClient.post()
                .uri("/repos/" + repo + "/git/trees")
                .body(Map.of("base_tree", baseTreeSha, "tree", entries))
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body).path("sha").asText();
    }

    private String createCommit(String message, String treeSha, String parentSha) throws Exception {
        String body = restClient.post()
                .uri("/repos/" + repo + "/git/commits")
                .body(Map.of("message", message, "tree", treeSha, "parents", List.of(parentSha)))
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body).path("sha").asText();
    }

    private void updateRef(String branch, String commitSha) {
        restClient.patch()
                .uri("/repos/" + repo + "/git/refs/heads/" + branch)
                .body(Map.of("sha", commitSha))
                .retrieve()
                .body(String.class);
    }

    String buildMdx(BlogPost post) {
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
