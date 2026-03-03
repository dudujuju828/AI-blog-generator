package com.maxthomarino.res.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);
    private static final int MAX_CONTENT_LENGTH = 10_000;

    private final RestClient restClient;
    private final Map<String, String> cache = new HashMap<>();

    public ResourceService() {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "BlogGenerator/1.0")
                .defaultHeader("Accept", "text/html,text/plain,*/*")
                .build();
    }

    public List<String> resolveResources(List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>();
        for (String resource : resources) {
            String content = resolveOne(resource.trim());
            if (content != null && !content.isBlank()) {
                resolved.add(content);
            }
        }
        return resolved;
    }

    private String resolveOne(String resource) {
        if (!resource.startsWith("http://") && !resource.startsWith("https://")) {
            return resource;
        }

        if (cache.containsKey(resource)) {
            return cache.get(resource);
        }

        try {
            String html = restClient.get()
                    .uri(resource)
                    .retrieve()
                    .body(String.class);

            if (html == null) {
                return null;
            }

            String text = stripHtml(html);
            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "\n[...truncated]";
            }

            String result = "Source: " + resource + "\n" + text;
            cache.put(resource, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch resource {}: {}", resource, e.getMessage());
            return null;
        }
    }

    private static String stripHtml(String html) {
        return html
                .replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ")
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
    }
}
