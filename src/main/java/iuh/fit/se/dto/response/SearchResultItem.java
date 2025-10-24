package iuh.fit.se.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchResultItem {
    // Flask trả "product": {...} – để linh hoạt, map thẳng thành Map
    private Map<String, Object> product;

    @JsonProperty("similarity_score")
    private Double similarityScore;

    @JsonProperty("matched_text")
    private String matchedText;
}