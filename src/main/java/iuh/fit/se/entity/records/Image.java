package iuh.fit.se.entity.records;

import lombok.Builder;

@Builder
public record Image(String url,Integer position) {
}
