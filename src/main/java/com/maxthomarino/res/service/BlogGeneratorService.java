package com.maxthomarino.res.service;

import com.maxthomarino.res.dto.GenerateResponse;
import com.maxthomarino.res.model.BlogPost;
import org.springframework.stereotype.Service;

@Service
public class BlogGeneratorService {

    private final LlmService llmService;
    private final GitHubService gitHubService;

    public BlogGeneratorService(LlmService llmService, GitHubService gitHubService) {
        this.llmService = llmService;
        this.gitHubService = gitHubService;
    }

    public GenerateResponse generate(String topic, int iterations) {
        BlogPost post = llmService.generatePost(topic);
        for (int i = 0; i < iterations - 1; i++) {
            String feedback = llmService.reviewPost(post);
            post = llmService.revisePost(post, feedback);
        }
        String commitUrl = gitHubService.commitPost(post);
        return new GenerateResponse(post.slug(), post.title(), commitUrl, "Blog post generated and committed");
    }
}
