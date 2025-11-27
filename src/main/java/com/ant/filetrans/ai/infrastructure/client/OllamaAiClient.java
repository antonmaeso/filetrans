package com.ant.filetrans.ai.infrastructure.client;

import com.ant.filetrans.ai.api.AiMetadata;
import com.ant.filetrans.ai.application.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Profile("ollama")
public class OllamaAiClient implements AiClient {

    private final OllamaChatModel chatModel;

    public OllamaAiClient(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AiMetadata analyze(Path imagePath) {
        log.info("Ollama AI analyzing {}", imagePath);
        String promptText = buildPrompt(imagePath);
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(promptText)));
        String content = extractContent(response);
        List<String> tags = extractTags(content, imagePath);
        double confidence = 0.75;
        log.info("Ollama AI response for {}: {}", imagePath, content);
        return new AiMetadata(content, tags, confidence);
    }

    private String buildPrompt(Path imagePath) {
        return """
                You are an assistant that generates short descriptions and tags for photos.

                Photo file name: %s

                Respond with 1-2 sentences followed by a short comma-separated tag list.
                Example: "A sunset over the mountains. Tags: sunset, mountains, landscape"
                """.formatted(imagePath.getFileName());
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String content = response.getResult().getOutput().getText();
        return content == null ? "" : content.trim();
    }

    private List<String> extractTags(String content, Path imagePath) {
        int idx = content.toLowerCase().indexOf("tags:");
        if (idx >= 0) {
            String tagSection = content.substring(idx + 5).trim();
            return Arrays.stream(tagSection.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isBlank())
                    .toList();
        }
        return List.of("ollama", imagePath.getFileName().toString());
    }
}
