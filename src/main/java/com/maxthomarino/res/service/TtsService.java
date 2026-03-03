package com.maxthomarino.res.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String voiceId;
    private final String model;

    public TtsService(@Value("${elevenlabs.api-key:}") String apiKey,
                      @Value("${elevenlabs.voice-id:JBFqnCBsd6RMkjVDRZzb}") String voiceId,
                      @Value("${elevenlabs.model:eleven_flash_v2_5}") String model) {
        this.apiKey = apiKey;
        this.voiceId = voiceId;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.elevenlabs.io")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public byte[] generateAudio(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ELEVENLABS_API_KEY not set — skipping audio generation");
            return null;
        }

        try {
            String cleanText = stripMarkdown(text);

            return restClient.post()
                    .uri("/v1/text-to-speech/{voiceId}?output_format=mp3_44100_128", voiceId)
                    .header("xi-api-key", apiKey)
                    .body(Map.of("text", cleanText, "model_id", model))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("ElevenLabs TTS generation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    String stripMarkdown(String content) {
        String text = content;

        // Remove HTML tags (e.g. <audio> element)
        text = text.replaceAll("<[^>]+>", "");

        // Remove image references entirely: ![alt](url)
        text = text.replaceAll("!\\[[^]]*]\\([^)]*\\)", "");

        // Convert links to just their text: [text](url) -> text
        text = text.replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1");

        // Remove code fences
        text = text.replaceAll("```[\\s\\S]*?```", "");

        // Remove inline code backticks
        text = text.replaceAll("`([^`]*)`", "$1");

        // Remove heading markers (keep the text)
        text = text.replaceAll("(?m)^#{1,6}\\s+", "");

        // Remove bold/italic markers
        text = text.replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1");
        text = text.replaceAll("_{1,3}([^_]+)_{1,3}", "$1");

        // Remove horizontal rules
        text = text.replaceAll("(?m)^[-*_]{3,}\\s*$", "");

        // Remove blockquote markers
        text = text.replaceAll("(?m)^>\\s?", "");

        // Collapse multiple newlines
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text.trim();
    }
}
