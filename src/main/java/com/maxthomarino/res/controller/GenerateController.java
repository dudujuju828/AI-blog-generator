package com.maxthomarino.res.controller;

import com.maxthomarino.res.dto.GenerateRequest;
import com.maxthomarino.res.dto.GenerateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GenerateController {

    @PostMapping("/generate")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        return new GenerateResponse(
                "test-slug",
                "Test Title",
                "https://github.com/maxthomarino/portfolio_new/commit/abc123",
                "Blog post generated for topic: " + request.topic()
        );
    }
}
