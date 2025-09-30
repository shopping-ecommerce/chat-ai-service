package iuh.fit.se.entity.records;

import lombok.Builder;

import java.time.Instant;

@Builder
public record Report(
        String userId,
        String description,
        Instant createdAt
) {
}
