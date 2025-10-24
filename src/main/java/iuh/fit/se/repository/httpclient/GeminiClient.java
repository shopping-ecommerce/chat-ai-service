// src/main/java/iuh/fit/se/repository/httpclient/GeminiClient.java
package iuh.fit.se.repository.httpclient;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import iuh.fit.se.dto.request.SearchRequest;
import iuh.fit.se.dto.response.SearchByImageResponse;
import iuh.fit.se.dto.response.SearchResponse;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "gemini-service",configuration = {GeminiClient.FormConfig.class}
)
public interface GeminiClient {

    // Full path đúng với Flask: /gemini/search/search
    @PostMapping(value = "/search/search", consumes = "application/json")
    SearchResponse semanticSearch(@RequestBody SearchRequest request);

    @PostMapping(value = "/index/search-by-image-multi",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    SearchByImageResponse searchByImageUpload(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "top_k", required = false) Integer topK,
            @RequestPart(value = "candidate_k", required = false) Integer candidateK,
            @RequestPart(value = "per_product_rerank", required = false) Integer perProductRerank,
            @RequestPart(value = "min_similarity", required = false) Double minSimilarity
    );
    @Configuration
    class FormConfig {
        @Bean
        public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
            return new SpringFormEncoder(new SpringEncoder(messageConverters));
        }
    }
}
