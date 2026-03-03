package com.maxthomarino.res.controller;

import com.maxthomarino.res.dto.GenerateRequest;
import com.maxthomarino.res.dto.GenerateResponse;
import com.maxthomarino.res.service.BlogGeneratorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GenerateController {

    private final BlogGeneratorService blogGeneratorService;

    public GenerateController(BlogGeneratorService blogGeneratorService) {
        this.blogGeneratorService = blogGeneratorService;
    }

    @PostMapping("/generate")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        int iterations = request.iterations() != null ? request.iterations() : 1;
        boolean images = request.images() != null && request.images();
        int imageCount = request.imageCount() != null ? request.imageCount() : 5;
        boolean audio = request.audio() != null && request.audio();
        return blogGeneratorService.generate(request.topic(), iterations, images, imageCount, audio);
    }
}
