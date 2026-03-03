package com.maxthomarino.res.service;

import com.maxthomarino.res.dto.GenerateResponse;
import com.maxthomarino.res.model.BlogPost;
import com.maxthomarino.res.model.ImagePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BlogGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(BlogGeneratorService.class);

    private final LlmService llmService;
    private final GitHubService gitHubService;
    private final ImageService imageService;
    private final TtsService ttsService;
    private final ResourceService resourceService;

    public BlogGeneratorService(LlmService llmService, GitHubService gitHubService,
                                ImageService imageService, TtsService ttsService,
                                ResourceService resourceService) {
        this.llmService = llmService;
        this.gitHubService = gitHubService;
        this.imageService = imageService;
        this.ttsService = ttsService;
        this.resourceService = resourceService;
    }

    public GenerateResponse generate(String topic, int iterations, boolean withImages, int imageCount,
                                      boolean withAudio, List<String> resources) {
        List<String> resolvedResources = resourceService.resolveResources(resources);

        BlogPost post = llmService.generatePost(topic, withImages, imageCount, resolvedResources);
        for (int i = 0; i < iterations - 1; i++) {
            String feedback = llmService.reviewPost(post);
            post = llmService.revisePost(post, feedback, withImages, imageCount, resolvedResources);
        }

        String content = post.content();
        Map<String, byte[]> extraFiles = new LinkedHashMap<>();

        if (withImages && !post.images().isEmpty()) {
            content = processImages(post, content, extraFiles);
        }

        if (withAudio) {
            try {
                byte[] audioBytes = ttsService.generateAudio(content);
                if (audioBytes != null) {
                    extraFiles.put("public/blog-audio/" + post.slug() + ".mp3", audioBytes);
                    content = "<BlogAudioPlayer src=\"/blog-audio/" + post.slug() + ".mp3\" />\n\n" + content;
                }
            } catch (Exception e) {
                log.error("Audio generation failed, publishing without audio: {}", e.getMessage());
            }
        }

        content = escapeMdxSpecialChars(content);

        BlogPost finalPost = new BlogPost(
                post.title(), post.date(), post.description(), post.tags(),
                content, post.slug(), post.images()
        );

        String commitUrl = gitHubService.commitAll(finalPost, extraFiles);
        return new GenerateResponse(finalPost.slug(), finalPost.title(), commitUrl, "Blog post generated and committed");
    }

    private String processImages(BlogPost post, String content, Map<String, byte[]> extraFiles) {
        for (int i = 0; i < post.images().size(); i++) {
            ImagePlacement placement = post.images().get(i);
            try {
                if (i > 0) {
                    Thread.sleep(10_000);
                }
                String base64 = imageService.generateImage(placement.prompt());
                if (base64 != null) {
                    byte[] imageData = Base64.getDecoder().decode(base64);
                    String fileName = post.slug() + "-image-" + (i + 1) + ".png";
                    extraFiles.put("public/blog-images/" + fileName, imageData);
                    String safeAlt = placement.alt().replace("<", "&lt;").replace(">", "&gt;");
                    content = content.replace(placement.placeholder(),
                            "![" + safeAlt + "](/blog-images/" + fileName + ")");
                } else {
                    content = content.replace(placement.placeholder(), "");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Image processing interrupted at {}", placement.placeholder());
                content = content.replace(placement.placeholder(), "");
                break;
            } catch (Exception e) {
                log.error("Failed to process image {}: {}", placement.placeholder(), e.getMessage());
                content = content.replace(placement.placeholder(), "");
            }
        }
        return content;
    }

    /**
     * Escapes MDX-sensitive characters in prose while preserving code fences,
     * inline code, math blocks, and intentional HTML/JSX tags.
     */
    static String escapeMdxSpecialChars(String content) {
        Pattern protectedRegion = Pattern.compile(
                "```[\\s\\S]*?```"            // code fences
                + "|\\$\\$[\\s\\S]*?\\$\\$"   // display math $$...$$
                + "|\\$[^$\\n]+\\$"            // inline math $...$
                + "|`[^`]+`"                   // inline code
                + "|!\\[[^]]*\\]\\([^)]*\\)"   // markdown images ![...](...)
                + "|</?[A-Z]\\w*[^>]*/?>\\s*"  // JSX components
                + "|</?(?:audio|source|img|br|hr|div|span|p|a|em|strong|ul|ol|li|table|tr|td|th|thead|tbody|blockquote|pre|code|h[1-6])[^>]*/?>" // known HTML tags
        );

        StringBuilder result = new StringBuilder();
        Matcher matcher = protectedRegion.matcher(content);
        int lastEnd = 0;

        while (matcher.find()) {
            String prose = content.substring(lastEnd, matcher.start());
            result.append(escapeProseForMdx(prose));
            // Append protected region unchanged
            result.append(matcher.group());
            lastEnd = matcher.end();
        }
        result.append(escapeProseForMdx(content.substring(lastEnd)));

        return result.toString();
    }

    private static String escapeProseForMdx(String prose) {
        // Escape all bare < that MDX would try to parse as JSX/HTML (e.g. <Derived>, <500, <T>)
        // Protected regions (known HTML, JSX, images, math) are already excluded by the caller.
        String escaped = prose.replace("<", "&lt;");
        // Escape curly braces that MDX would parse as JSX expressions
        escaped = escaped.replace("{", "\\{").replace("}", "\\}");
        return escaped;
    }
}
