package iuh.fit.se.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import iuh.fit.se.entity.ProductElastic;
import iuh.fit.se.repository.ProductRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    ProductRepository productRepository;
    ElasticsearchClient elasticsearchClient;
    // Full-text search
    public List<ProductElastic> searchProducts(String query) {
        List<ProductElastic> products = productRepository.searchByNameOrDescription(query);
        return products;
    }
    public List<String> suggestProducts(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }

        String queryText = prefix.trim();

        try {
            SearchResponse<Void> resp = elasticsearchClient.search(s -> s
                            .index("products")
                            .suggest(sug -> sug
                                    .text(queryText)
                                    .suggesters("product-suggest", cs -> cs
                                            .completion(c -> c
                                                    .field("nameSuggest")
                                                    .skipDuplicates(true)
                                                    .size(8)
                                            )
                                    )
                            ),
                    Void.class
            );

            Map<String, List<Suggestion<Void>>> suggestMap = resp.suggest();
            if (suggestMap == null || suggestMap.isEmpty()) {
                return List.of();
            }

            return suggestMap.getOrDefault("product-suggest", List.of())
                    .stream()
                    .flatMap(sug -> sug.completion().options().stream())
                    .map(opt -> opt.text())
                    .distinct()
                    .limit(10)
                    .toList();

        } catch (Exception e) {
            log.error("Suggest error with prefix '{}': {}", queryText, e.getMessage(), e);
            return List.of();
        }
    }
}