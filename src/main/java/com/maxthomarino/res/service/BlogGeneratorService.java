package com.maxthomarino.res.service;

import com.maxthomarino.res.dto.GenerateResponse;
import com.maxthomarino.res.model.BlogPost;
import com.maxthomarino.res.model.ImagePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BlogGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(BlogGeneratorService.class);

    private final LlmService llmService;
    private final GitHubService gitHubService;
    private final ImageService imageService;

    public BlogGeneratorService(LlmService llmService, GitHubService gitHubService, ImageService imageService) {
        this.llmService = llmService;
        this.gitHubService = gitHubService;
        this.imageService = imageService;
    }

    public GenerateResponse generate(String topic, int iterations, boolean withImages, int imageCount) {
        BlogPost post = llmService.generatePost(topic, withImages, imageCount);
        for (int i = 0; i < iterations - 1; i++) {
            String feedback = llmService.reviewPost(post);
            post = llmService.revisePost(post, feedback, withImages, imageCount);
        }

        String content = post.content();
        Map<String, byte[]> imageFiles = new LinkedHashMap<>();

        if (withImages && !post.images().isEmpty()) {
            content = processImages(post, content, imageFiles);
        }

        BlogPost finalPost = new BlogPost(
                post.title(), post.date(), post.description(), post.tags(),
                content, post.slug(), post.images()
        );

        String commitUrl = gitHubService.commitAll(finalPost, imageFiles);
        return new GenerateResponse(finalPost.slug(), finalPost.title(), commitUrl, "Blog post generated and committed");
    }

    private String processImages(BlogPost post, String content, Map<String, byte[]> imageFiles) {
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
                    imageFiles.put(fileName, imageData);
                    content = content.replace(placement.placeholder(),
                            "![" + placement.alt() + "](/blog-images/" + fileName + ")");
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
}
