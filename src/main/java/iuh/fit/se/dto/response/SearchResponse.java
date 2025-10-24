package iuh.fit.se.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchResponse {
    private Boolean success;
    private String query;

    @JsonProperty("total_results")
    private Integer totalResults;

    private List<SearchResultItem> results;

    // Nếu Flask trả lỗi dạng {"error":"..."} mà vẫn 200 thì cũng hứng được
    private String error;
}