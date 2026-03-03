package com.maxthomarino.res.dto;

public record GenerateRequest(String topic, Integer iterations, Boolean images, Integer imageCount, Boolean audio) {
}
