package iuh.fit.se.repository;

import iuh.fit.se.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    /**
     * Tìm chính sách theo code
     */
    Optional<Policy> findByCode(String code);

    /**
     * FULLTEXT search - yêu cầu đã tạo FULLTEXT INDEX
     * Chạy SQL sau để tạo index:
     * ALTER TABLE policies ADD FULLTEXT INDEX ft_policy_search (title, content_markdown);
     */
    @Query(value = """
        SELECT * FROM policies
        WHERE MATCH (title, content_markdown) AGAINST (:q IN NATURAL LANGUAGE MODE)
        ORDER BY effective_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Policy> searchFulltext(@Param("q") String q, @Param("limit") int limit);

    /**
     * Fallback LIKE search - luôn hoạt động, không cần index
     */
    @Query(value = """
        SELECT * FROM policies
        WHERE LOWER(title) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(content_markdown) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY effective_date DESC
        """, nativeQuery = true)
    List<Policy> searchLike(@Param("q") String q);

    /**
     * Lấy tất cả chính sách, sắp xếp theo ngày hiệu lực giảm dần
     */
    @Query(value = "SELECT * FROM policies ORDER BY effective_date DESC", nativeQuery = true)
    List<Policy> findAllOrderByEffectiveDateDesc();
}
