package iuh.fit.se.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRequest {
    private String query;

    @JsonProperty("top_k")
    private Integer topK;

    private Map<String, Object> filter;
}
