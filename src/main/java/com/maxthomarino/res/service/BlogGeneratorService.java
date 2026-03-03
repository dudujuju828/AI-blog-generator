package com.maxthomarino.res.service;

import com.maxthomarino.res.dto.GenerateResponse;
import com.maxthomarino.res.model.BlogPost;
import com.maxthomarino.res.model.ImagePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;

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

    public GenerateResponse generate(String topic, int iterations, boolean withImages) {
        BlogPost post = llmService.generatePost(topic, withImages);
        for (int i = 0; i < iterations - 1; i++) {
            String feedback = llmService.reviewPost(post);
            post = llmService.revisePost(post, feedback, withImages);
        }

        String content = post.content();
        if (withImages && !post.images().isEmpty()) {
            content = processImages(post, content);
        }

        BlogPost finalPost = new BlogPost(
                post.title(), post.date(), post.description(), post.tags(),
                content, post.slug(), post.images()
        );

        String commitUrl = gitHubService.commitPost(finalPost);
        return new GenerateResponse(finalPost.slug(), finalPost.title(), commitUrl, "Blog post generated and committed");
    }

    private String processImages(BlogPost post, String content) {
        for (int i = 0; i < post.images().size(); i++) {
            ImagePlacement placement = post.images().get(i);
            try {
                String base64 = imageService.generateImage(placement.prompt());
                if (base64 != null) {
                    byte[] imageData = Base64.getDecoder().decode(base64);
                    String imageName = "image-" + (i + 1);
                    String imagePath = gitHubService.commitImage(post.slug(), imageName, imageData);
                    content = content.replace(placement.placeholder(),
                            "![" + placement.alt() + "](" + imagePath + ")");
                } else {
                    content = content.replace(placement.placeholder(), "");
                }
            } catch (Exception e) {
                log.error("Failed to process image {}: {}", placement.placeholder(), e.getMessage());
                content = content.replace(placement.placeholder(), "");
            }
        }
        return content;
    }
}
