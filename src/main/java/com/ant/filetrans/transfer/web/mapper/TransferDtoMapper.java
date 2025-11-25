package com.ant.filetrans.transfer.web.mapper;

import com.ant.filetrans.transfer.application.TransferCommand;
import com.ant.filetrans.transfer.infrastructure.batch.Extensions;
import com.ant.filetrans.transfer.web.dto.CreateTransferRequest;
import com.ant.filetrans.transfer.web.dto.TransferResponse;
import org.springframework.batch.core.job.JobExecution;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TransferDtoMapper {

    private TransferDtoMapper() {
    }

    public static TransferCommand toCommand(CreateTransferRequest req) {
        Path target = toPath(req.targetBaseDir());
        Path file = toPath(req.filePath());
        Path source = toPath(req.sourceDir());
        Extensions extensions = Extensions.of(parseExtensions(req.extensions()));

        return new TransferCommand(source, target, file, extensions);
    }

    public static TransferResponse fromJobExecution(JobExecution exec) {
        Instant start = exec.getStartTime() != null
                ? exec.getStartTime().atZone(ZoneId.systemDefault()).toInstant()
                : null;
        Instant end = exec.getEndTime() != null
                ? exec.getEndTime().atZone(ZoneId.systemDefault()).toInstant()
                : null;

        return new TransferResponse(
                exec.getId(),
                exec.getStatus().toString(),
                start,
                end
        );
    }

    private static Path toPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value);
    }

    private static Set<String> parseExtensions(List<String> extensions) {
        if (extensions == null) {
            return Set.of();
        }
        return extensions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(TransferDtoMapper::stripWildcardsAndDots)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String stripWildcardsAndDots(String value) {
        String result = value;
        while (!result.isEmpty() && (result.charAt(0) == '.' || result.charAt(0) == '*')) {
            result = result.substring(1);
        }
        return result;
    }
}
