package iuh.fit.se.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.dto.ChatRequest;
import iuh.fit.se.dto.ProductSearchPayload;
import iuh.fit.se.repository.httpclient.GeminiClient;
import iuh.fit.se.dto.request.SearchRequest;
import iuh.fit.se.dto.response.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Slf4j
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Bạn là Shopping AI Assistant - trợ lý mua sắm thông minh cho sàn thương mại điện tử.
            
            ## Khả năng của bạn:
            1. **Tìm kiếm sản phẩm**: 
               - Sử dụng searchProducts() cho truy vấn văn bản
               - Sử dụng searchProductsByImage() khi người dùng gửi ảnh
            
            2. **Thông tin chính sách**: 
               - Sử dụng policy_getByCode() khi biết mã chính sách (VD: PROHIBITED_ITEMS, SELLER_TOS)
               - Sử dụng policy_search() để tìm chính sách theo từ khóa
               - Sử dụng policy_listNewest() để liệt kê chính sách mới nhất
            
            3. **Trò chuyện tự nhiên**: Trả lời các câu hỏi thường không cần dùng công cụ
            
            ## Nguyên tắc hoạt động:
            - LUÔN trả lời bằng tiếng Việt khi người dùng nói tiếng Việt
            - LUÔN dùng công cụ tìm kiếm khi người dùng hỏi về sản phẩm, giá cả, tồn kho
            - LUÔN tra cứu chính sách khi người dùng hỏi về quy định, hoàn tiền, vi phạm, điều khoản
            - Trả lời ngắn gọn, thân thiện, có thể hài hước nhẹ nhàng
            - Nếu không biết, hãy thừa nhận thẳng thắn
            
            ## Ví dụ sử dụng công cụ:
            - "Tìm áo hoodie màu đen" → searchProducts(query="áo hoodie màu đen", limit=4)
            - "Chính sách hoàn tiền như thế nào?" → policy_search(q="hoàn tiền", limit=5)
            - "Cho tôi xem code PROHIBITED_ITEMS" → policy_getByCode(code="PROHIBITED_ITEMS")
            - "Có sản phẩm nào giống cái này không?" (kèm ảnh) → searchProductsByImage()
            - "Hàng cấm là gì?" → policy_search(q="hàng cấm", limit=3)
            
            ## ⚠️ QUAN TRỌNG - Định dạng trả về khi dùng searchProducts hoặc searchProductsByImage:
            
            **Khi tool trả về kết quả sản phẩm:**
            1. Tool sẽ cho bạn JSON với cấu trúc:
               {
                 "type": "product_list",
                 "message": "Tìm thấy X sản phẩm...",
                 "items": [
                   {
                     "id": "...",
                     "name": "...",
                     "price": 129000.0,
                     "discount": 0.0,
                     "description": "...",
                     "url": "/products/...",
                     "imageUrl": "https://..."
                   }
                 ]
               }
            
            2. Bạn PHẢI trả về JSON NGUYÊN VẸN này cho user
            3. KHÔNG được:
               - Tóm tắt hay viết lại nội dung
               - Chuyển sang markdown list
               - Thay đổi cấu trúc JSON
               - Thêm/bớt field nào
            
            4. CHỈ được phép:
               - Thêm 1-2 câu nhận xét ngắn TRƯỚC JSON (không bắt buộc)
               - Giữ NGUYÊN TOÀN BỘ JSON từ tool
            
            **Ví dụ response đúng:**
            ```
            Mình tìm thấy sản phẩm phù hợp với bạn rồi đây:
            
            {
              "type": "product_list",
              "message": "Tìm thấy 3 sản phẩm cho: \"áo hoodie\"",
              "items": [
                {
                  "id": "68ff149c6a32474c840bb4a8",
                  "name": "Áo hoodie basic",
                  "price": 299000.0,
                  "discount": 10.0,
                  "description": "Áo hoodie cotton mềm mại",
                  "url": "/products/68ff149c6a32474c840bb4a8",
                  "imageUrl": "https://example.com/image.jpg"
                }
              ]
            }
            ```
            
            **Ví dụ response SAI (TUYỆT ĐỐI KHÔNG làm):**
            ❌ "Mình tìm được 3 sản phẩm:
                • Áo hoodie basic - 299,000đ
                • ..."
            ❌ Tóm tắt thành text
            ❌ Thay đổi bất kỳ field nào trong JSON
            
            ## Định dạng trả về cho chính sách:
            - Khi trả về chính sách: tóm tắt nội dung chính + trích dẫn chi tiết nếu cần
            - Luôn thân thiện và hữu ích
            """;

    private final ChatClient chatClient;
    private final SearchProductsTool searchProductsTool;
    private final PolicySimpleTool policyTool;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       JdbcChatMemoryRepository jdbcChatMemoryRepository,
                       SearchProductsTool searchProductsTool,
                       PolicySimpleTool policyTool) {
        this.searchProductsTool = searchProductsTool;
        this.policyTool = policyTool;

        log.info("🔧 Initializing ChatClient with tools: {}, {}",
                searchProductsTool.getClass().getSimpleName(),
                policyTool.getClass().getSimpleName());

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(10)
                .build();

        // ✅ QUAN TRỌNG: Đăng ký tools với ChatClient
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(searchProductsTool, policyTool) // ✅ Đăng ký cả 2 tools
                .build();

        log.info("✅ ChatClient initialized successfully with {} tools", 2);
    }
    private static String extractFirstJsonObject(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        boolean inStr = false;
        char prev = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"' && prev != '\\') {
                inStr = !inStr;
            }
            if (!inStr) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
            prev = c;
        }
        return null;
    }
    /**
     * Chat với văn bản - để LLM tự quyết định dùng tool nào
     */
    public String chat(ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        log.info("💬 Chat request: conversationId={}, message='{}'", conversationId, request.message());

        Prompt prompt = new Prompt(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(request.message())
        );

        try {
            String raw = chatClient.prompt(prompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            log.info("✅ Chat response generated successfully");

            if (raw != null && raw.contains("\"type\"") && raw.contains("product_list")) {
                String json = extractFirstJsonObject(raw);
                if (json != null) {
                    return json;
                }
                // fallback: nếu không cắt được thì trả stub JSON để FE không lỗi
                return "{\"type\":\"product_list\",\"message\":\"Lỗi định dạng kết quả\",\"items\":[]}";
            }

            // Không phải product_list (vd: trả lời chính sách / small talk)
            return raw;

        } catch (Exception e) {
            log.error("❌ Error calling Chat API: {}", e.getMessage(), e);
            return "{\"type\":\"product_list\",\"message\":\"Lỗi xử lý\",\"items\":[]}";
        }
    }


    /**
     * Chat với hình ảnh - ưu tiên tìm kiếm sản phẩm tương tự
     */
    public String chatWithImage(MultipartFile file, String message, String conversationId) {
        String cid = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        log.info("🖼️ Chat with image: conversationId={}, message='{}', fileSize={}",
                cid, message, file.getSize());

        // ✅ Ưu tiên tìm kiếm sản phẩm theo ảnh
        boolean isProductSearchIntent = message == null || message.isBlank() ||
                message.toLowerCase().contains("tìm") ||
                message.toLowerCase().contains("giống") ||
                message.toLowerCase().contains("tương tự") ||
                message.toLowerCase().contains("search") ||
                message.toLowerCase().contains("find");

        if (isProductSearchIntent) {
            try {
                log.info("🔍 Attempting image-based product search...");
                return searchProductsTool.searchProductsByImage(file, 5, 0.8);
            } catch (Exception ex) {
                log.warn("⚠️ Image search failed, falling back to vision chat. Error: {}", ex.getMessage());
            }
        }

        // ✅ Fallback: gửi ảnh + text cho LLM phân tích
        org.springframework.util.MimeType mime = MimeTypeUtils.APPLICATION_OCTET_STREAM;
        try {
            if (file.getContentType() != null) {
                mime = MimeTypeUtils.parseMimeType(file.getContentType());
            }
        } catch (Exception ignore) {
        }

        Media media = Media.builder()
                .mimeType(mime)
                .data(file.getResource())
                .build();

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.media(media).text(message))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                    .call()
                    .content();

            log.info("✅ Vision chat response generated successfully");
            return response;

        } catch (Exception e) {
            log.error("❌ Error calling Chat API with image: {}", e.getMessage(), e);
            return "Oops, có lỗi xảy ra khi xử lý hình ảnh! Thử lại sau nhé 😅";
        }
    }

    // ========== TOOL: Tìm kiếm sản phẩm qua SEMANTIC SEARCH (Gemini) ==========
    @Component
    public static class SearchProductsTool {
        private static final double SIM_THRESHOLD = 0.7;
        private final ObjectMapper mapper = new ObjectMapper();
        private final GeminiClient geminiClient;

        public SearchProductsTool(GeminiClient geminiClient) {
            this.geminiClient = geminiClient;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "searchProducts",
                description = "Tìm kiếm sản phẩm theo từ khóa văn bản. Sử dụng công cụ này khi người dùng hỏi về sản phẩm, giá cả, tìm đồ. " +
                        "Tool này sẽ trả về JSON với cấu trúc {type, message, items[]}. " +
                        "LLM PHẢI trả về JSON nguyên vẹn cho user, KHÔNG được tóm tắt hay chuyển sang markdown."
        )
        public String searchProducts(
                @org.springframework.ai.tool.annotation.ToolParam(description = "Từ khóa tìm kiếm (ví dụ: áo hoodie đen, giày thể thao)") String query,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Số lượng kết quả tối đa (mặc định: 4)") Integer limit) {

            int resultLimit = (limit != null && limit > 0) ? limit : 4;
            log.info("🔍 TOOL CALLED: searchProducts(query='{}', limit={}, threshold={})",
                    query, resultLimit, SIM_THRESHOLD);

            try {
                SearchResponse resp = geminiClient.semanticSearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(10)
                                .build()
                );

                if (resp == null || Boolean.FALSE.equals(resp.getSuccess()) || resp.getResults() == null) {
                    log.warn("⚠️ No results from semantic search");
                    return emptyPayload(query, "không có kết quả từ dịch vụ tìm kiếm");
                }

                var passed = resp.getResults().stream()
                        .map(r -> new ResultWrap(r.getProduct(), normalizeSimilarity(r.getSimilarityScore()), r.getMatchedText()))
                        .filter(x -> x.sim >= SIM_THRESHOLD)
                        .limit(resultLimit)
                        .toList();

                if (passed.isEmpty()) {
                    log.warn("⚠️ No results passed similarity threshold ({})", SIM_THRESHOLD);
                    return emptyPayload(query, "độ tương đồng < " + SIM_THRESHOLD);
                }

                ProductSearchPayload payload = new ProductSearchPayload();
                payload.type = "product_list"; // ✅ Thêm type
                payload.message = (query == null || query.isBlank()) ? null
                        : ("Tìm thấy " + passed.size() + " sản phẩm cho: \"" + query + "\"");

                payload.items = passed.stream().map(x -> {
                    Map<String, Object> p = x.product;

                    ProductSearchPayload.Item it = new ProductSearchPayload.Item();
                    it.id = extractId(p);
                    it.name = strOrDefault(p.get("name"), "(Chưa có tên)");
                    it.description = strOrDefault(p.get("description"), "");
                    it.price = extractFirstPriceFromSizes(p.get("variants"));
                    it.discount = extractDouble(p.get("percentDiscount"), 0.0);
                    it.url = "/products/" + it.id;
                    it.imageUrl = pickFirstImage(p);

                    return it;
                }).toList();

                log.info("✅ Found {} products", payload.items.size());
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            } catch (Exception e) {
                log.error("❌ Semantic search error: {}", e.getMessage(), e);
                return emptyPayload(query, "lỗi xử lý kết quả semantic");
            }
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "searchProductsByImage",
                description = "Tìm kiếm sản phẩm tương tự dựa trên hình ảnh. Sử dụng khi người dùng upload ảnh và muốn tìm sản phẩm giống. " +
                        "Tool này sẽ trả về JSON với cấu trúc {type, message, items[]}. " +
                        "LLM PHẢI trả về JSON nguyên vẹn cho user, KHÔNG được tóm tắt hay chuyển sang markdown."
        )
        public String searchProductsByImage(
                @org.springframework.ai.tool.annotation.ToolParam(description = "File ảnh để tìm kiếm") MultipartFile image,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Số lượng kết quả (mặc định 5)") Integer topK,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Ngưỡng tương đồng tối thiểu (0..1)") Double minSimilarity
        ) {
            int tk = (topK != null && topK > 0) ? topK : 5;
            double threshold = (minSimilarity != null) ? minSimilarity : 0.8;

            log.info("🖼️ TOOL CALLED: searchProductsByImage(topK={}, threshold={})", tk, threshold);

            try {
                var resp = geminiClient.searchByImageUpload(image, tk, 300, 8, threshold);

                if (resp == null || Boolean.FALSE.equals(resp.getSuccess()) || resp.getResults() == null) {
                    log.warn("⚠️ No results from image search");
                    return emptyPayload("", "không có kết quả image search");
                }

                var filtered = resp.getResults().stream()
                        .filter(r -> normalizeSimilarity(r.getSimilarityScore()) >= threshold)
                        .limit(tk)
                        .toList();

                if (filtered.isEmpty()) {
                    log.warn("⚠️ No results passed similarity threshold ({})", threshold);
                    return emptyPayload("", "độ tương đồng < " + threshold);
                }

                ProductSearchPayload payload = new ProductSearchPayload();
                payload.type = "product_list"; // ✅ Thêm type
                payload.message = "Tìm thấy " + filtered.size() + " sản phẩm tương tự từ hình ảnh";
                payload.items = filtered.stream().map(r -> {
                    Map<String, Object> p = r.getProduct();
                    ProductSearchPayload.Item it = new ProductSearchPayload.Item();
                    it.id = extractId(p);
                    it.name = strOrDefault(p.get("name"), "(Chưa có tên)");
                    it.description = strOrDefault(p.get("description"), "");
                    it.price = extractFirstPriceFromSizes(p.get("variants"));
                    it.discount = extractDouble(p.get("percentDiscount"), 0.0);
                    it.url = "/products/" + it.id;
                    it.imageUrl = pickFirstImage(p);
                    return it;
                }).toList();

                log.info("✅ Found {} similar products", payload.items.size());
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            } catch (Exception e) {
                log.error("❌ Image search error: {}", e.getMessage(), e);
                return emptyPayload("", "lỗi gọi image search");
            }
        }

        /* ------------ Helper Methods ------------ */

        private static class ResultWrap {
            final Map<String, Object> product;
            final double sim;
            final String matched;

            ResultWrap(Map<String, Object> product, double sim, String matched) {
                this.product = product;
                this.sim = sim;
                this.matched = matched;
            }
        }

        private static double normalizeSimilarity(Double score) {
            if (score == null) return 0.0;
            if (score > 1.0) {
                return 1.0 / (1.0 + score);
            }
            if (score < 0) return 0.0;
            return Math.min(score, 1.0);
        }

        private String emptyPayload(String query, String reason) {
            try {
                ProductSearchPayload payload = new ProductSearchPayload();
                payload.type = "product_list"; // ✅ Thêm type
                payload.message = "Không tìm thấy sản phẩm phù hợp" +
                        (reason != null && !reason.isEmpty() ? " (" + reason + ")" : "");
                payload.items = List.of();
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            } catch (Exception e) {
                return "{\"type\":\"product_list\",\"message\":\"Lỗi xử lý\",\"items\":[]}";
            }
        }

        private static String str(Object o) {
            return o == null ? "" : String.valueOf(o);
        }

        private static String strOrDefault(Object o, String def) {
            String s = str(o);
            return s.isEmpty() ? def : s;
        }

        private static Double extractDouble(Object v, double def) {
            try {
                if (v == null) return def;
                if (v instanceof Number n) return n.doubleValue();
                if (v instanceof java.math.BigDecimal bd) return bd.doubleValue();
                if (v instanceof Map<?, ?> m) {
                    Object nl = m.get("$numberLong");
                    if (nl != null) return Double.parseDouble(String.valueOf(nl));
                }
                return Double.parseDouble(v.toString());
            } catch (Exception e) {
                return def;
            }
        }

        @SuppressWarnings("unchecked")
        private static String extractId(Map<String, Object> product) {
            Object id = product.get("_id");
            if (id instanceof Map<?, ?> m) {
                Object oid = ((Map<String, Object>) m).get("$oid");
                if (oid != null) return oid.toString();
            }
            return str(id);
        }

        @SuppressWarnings("unchecked")
        private static Double extractFirstPriceFromSizes(Object sizes) {
            try {
                if (!(sizes instanceof List<?> list) || list.isEmpty()) return 0.0;
                Object first = list.get(0);
                if (first instanceof Map<?, ?> m) {
                    Object price = ((Map<String, Object>) m).get("price");
                    return extractPriceFlexible(price);
                }
                return 0.0;
            } catch (Exception e) {
                return 0.0;
            }
        }

        private static double extractPriceFlexible(Object price) {
            if (price == null) return 0.0;
            if (price instanceof Number n) return n.doubleValue();
            if (price instanceof java.math.BigDecimal bd) return bd.doubleValue();
            if (price instanceof Map<?, ?> m) {
                Object nl = m.get("$numberLong");
                if (nl != null) {
                    try {
                        return Double.parseDouble(String.valueOf(nl));
                    } catch (Exception ignore) {
                    }
                }
            }
            try {
                return Double.parseDouble(price.toString());
            } catch (Exception e) {
                return 0.0;
            }
        }

        @SuppressWarnings("unchecked")
        private static String pickFirstImage(Map<String, Object> productMap) {
            try {
                Object images = productMap.get("images");
                if (images instanceof List<?> list && !list.isEmpty()) {
                    for (Object el : list) {
                        if (el instanceof Map<?, ?> m) {
                            Object url = ((Map<String, Object>) m).get("url");
                            if (url != null && !url.toString().toLowerCase().endsWith(".mp4")) {
                                return url.toString();
                            }
                        } else if (el instanceof String s) {
                            if (!s.toLowerCase().endsWith(".mp4")) return s;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            return "/img/default.png";
        }
    }
}