package iuh.fit.se.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.entity.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicySimpleTool {

    private final PolicySimpleService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @Tool(
            name = "policy_getByCode",
            description = "Lấy chi tiết một chính sách cụ thể theo mã code. " +
                    "Ví dụ: PROHIBITED_ITEMS (hàng cấm), SELLER_TOS (điều khoản người bán), BUYER_REFUND (hoàn tiền)"
    )
    public String getByCode(
            @ToolParam(description = "Mã chính sách (VD: PROHIBITED_ITEMS, SELLER_TOS)") String code
    ) {
        log.info("📜 TOOL CALLED: policy_getByCode(code='{}')", code);

        try {
            Optional<Policy> policyOpt = service.getByCode(code);

            if (policyOpt.isEmpty()) {
                log.warn("⚠️ Policy not found: {}", code);
                return mapper.writeValueAsString(Map.of(
                        "type", "policy",
                        "error", "not_found",
                        "message", "Không tìm thấy chính sách với mã: " + code
                ));
            }

            Policy policy = policyOpt.get();
            log.info("✅ Policy found: {} ({})", policy.getTitle(), policy.getCode());

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "type", "policy",
                    "policy", oneAsMap(policy)
            ));

        } catch (Exception e) {
            log.error("❌ Error in policy_getByCode: {}", e.getMessage(), e);
            return "{\"type\":\"policy\",\"error\":\"internal_error\"}";
        }
    }

    @Tool(
            name = "policy_search",
            description = "Tìm kiếm chính sách theo từ khóa tiếng Việt. " +
                    "Sử dụng khi người dùng hỏi về quy định, điều khoản, hoàn tiền, vi phạm, hàng cấm, etc."
    )
    public String search(
            @ToolParam(description = "Từ khóa tìm kiếm (VD: hoàn tiền, hàng cấm, vi phạm)") String q,
            @ToolParam(description = "Số lượng kết quả tối đa (mặc định: 5)") Integer limit
    ) {
        int lim = (limit != null && limit > 0) ? limit : 5;
        log.info("📜 TOOL CALLED: policy_search(q='{}', limit={})", q, lim);

        try {
            // Sử dụng fulltext search nếu đã cấu hình
            var list = service.search(q, lim, true);

            log.info("✅ Found {} policies matching '{}'", list.size(), q);

            var items = list.stream().map(this::oneAsMap).toList();

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "type", "policy_list",
                    "query", q,
                    "count", items.size(),
                    "items", items
            ));

        } catch (Exception e) {
            log.error("❌ Error in policy_search: {}", e.getMessage(), e);
            return "{\"type\":\"policy_list\",\"items\":[]}";
        }
    }

    @Tool(
            name = "policy_listNewest",
            description = "Liệt kê các chính sách mới nhất của hệ thống. " +
                    "Sử dụng khi người dùng muốn xem tất cả chính sách hoặc chính sách gần đây."
    )
    public String listNewest(
            @ToolParam(description = "Số lượng chính sách (mặc định: 5)") Integer limit
    ) {
        int lim = (limit != null && limit > 0) ? limit : 5;
        log.info("📜 TOOL CALLED: policy_listNewest(limit={})", lim);

        try {
            var list = service.listAllNewestFirst(lim);

            log.info("✅ Listing {} newest policies", list.size());

            var items = list.stream().map(this::oneAsMap).toList();

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "type", "policy_list",
                    "count", items.size(),
                    "items", items
            ));

        } catch (Exception e) {
            log.error("❌ Error in policy_listNewest: {}", e.getMessage(), e);
            return "{\"type\":\"policy_list\",\"items\":[]}";
        }
    }

    /* ------------ Helper Methods ------------ */

    private Map<String, Object> oneAsMap(Policy p) {
        return Map.of(
                "id", p.getId(),
                "code", p.getCode(),
                "title", p.getTitle(),
                "version", p.getVersion(),
                "effectiveDate", p.getEffectiveDate().toString(),
                "contentMarkdown", p.getContentMarkdown()
        );
    }
}
