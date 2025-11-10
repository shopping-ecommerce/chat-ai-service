package iuh.fit.se.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initDatabase() {
        try {
            log.info("🔧 Initializing database indexes...");

            // 1. Kiểm tra xem FULLTEXT INDEX đã tồn tại chưa
            String checkIndexSql = """
                SELECT COUNT(*) 
                FROM information_schema.statistics 
                WHERE table_schema = DATABASE() 
                  AND table_name = 'policies' 
                  AND index_name = 'ft_policy_search'
                """;

            Integer indexCount = jdbcTemplate.queryForObject(checkIndexSql, Integer.class);

            if (indexCount == null || indexCount == 0) {
                log.info("📌 Creating FULLTEXT INDEX for policies...");
                jdbcTemplate.execute(
                        "ALTER TABLE policies ADD FULLTEXT INDEX ft_policy_search (title, content_markdown)"
                );
                log.info("✅ FULLTEXT INDEX created successfully");
            } else {
                log.info("✅ FULLTEXT INDEX already exists");
            }

            // 2. Modify SPRING_AI_CHAT_MEMORY column (luôn chạy, idempotent)
            log.info("📌 Modifying SPRING_AI_CHAT_MEMORY column...");
            jdbcTemplate.execute(
                    "ALTER TABLE SPRING_AI_CHAT_MEMORY MODIFY COLUMN content TEXT NULL"
            );
            log.info("✅ Column modified successfully");

        } catch (Exception e) {
            log.error("❌ Error initializing database: {}", e.getMessage(), e);
            // Không throw exception để app vẫn khởi động được
        }
    }
}