package iuh.fit.se.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchByImageResponse {
    private Boolean success;

    @JsonProperty("search_type")
    private String searchType;

    @JsonProperty("total_results")
    private Integer totalResults;

    private List<ResultItem> results;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ResultItem {
        private Map<String, Object> product;

        @JsonProperty("similarity_score")
        private Double similarityScore;

        private Double distance;

        @JsonProperty("matched_image")
        private MatchedImage matchedImage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MatchedImage {
        private String url;
        private Integer position;
    }
}
