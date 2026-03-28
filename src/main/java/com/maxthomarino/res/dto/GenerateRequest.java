package com.maxthomarino.res.dto;

import java.util.List;

public record GenerateRequest(String topic, Integer iterations, Boolean images, Integer imageCount, Boolean audio,
                               List<String> resources) {
}
