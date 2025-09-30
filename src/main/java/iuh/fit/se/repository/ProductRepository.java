package iuh.fit.se.repository;

import iuh.fit.se.entity.ProductElastic;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductElastic, String> {
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"originalName\", \"description\"], \"fuzziness\": \"AUTO\"}}")
    List<ProductElastic> searchByNameOrDescription(String query);}