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
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are Shopping AI Assistant for an e-commerce platform.
            You help users find products, answer product questions, and recommend items based on preferences.
            Always be polite, concise, and a bit playful (light humor, no sarcasm).
            If you don't know, say "I don't know".
            Reply in the user's language when possible.
            """;

    private final ChatClient chatClient;
    private final SearchProductsTool searchProductsTool;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       JdbcChatMemoryRepository jdbcChatMemoryRepository,
                       SearchProductsTool searchProductsTool) {
        this.searchProductsTool = searchProductsTool;
        log.info("🔧 ChatClient created with tools: {}", searchProductsTool.getClass().getSimpleName());

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(15)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        String userMessage = request.message().toLowerCase();

        // Nếu là yêu cầu tìm kiếm sản phẩm -> dùng Semantic search (Gemini)
        if (isProductSearchRequest(userMessage)) {
            String query = extractQuery(userMessage);
            int limit = 4; // mặc định
            log.info("Semantic product search ONLY: query={}, limit={}", query, limit);
            return searchProductsTool.searchProducts(query, limit);
        }

        // Các yêu cầu khác -> gọi LLM như cũ
        Prompt prompt = new Prompt(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(request.message())
        );

        ChatOptions chatOptions = ChatOptions.builder()
                .temperature(0.25)
                .maxTokens(2000)
                .build();

        try {
            return chatClient.prompt(prompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .options(chatOptions)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error calling Chat API: {}", e.getMessage());
            return "Oops, có lỗi xảy ra! Thử lại sau nhé 😅";
        }
    }

    private boolean isProductSearchRequest(String message) {
        String[] keywords = {"tìm", "có", "sản phẩm", "mua", "giày", "áo", "quần"};
        return Arrays.stream(keywords).anyMatch(message::contains);
    }

    private String extractQuery(String message) {
        String[] stopwords = {"tìm", "có", "sản phẩm", "mua"};
        String query = message;
        for (String stopword : stopwords) {
            query = query.replaceAll("(?i)" + stopword, "").trim();
        }
        return query.isEmpty() ? "" : query;
    }

    public String chatWithImage(MultipartFile file, String message, String conversationId) {
        String cid = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        // 1) Nếu là ngữ cảnh mua sắm -> ưu tiên search bằng ảnh (multipart)
        String lower = (message == null) ? "" : message.toLowerCase();
        boolean wantShopping = lower.isBlank() || isProductSearchRequest(lower);
        if (wantShopping) {
            try {
                // topK = 5, minSimilarity = 0.85 (bạn có thể chỉnh)
                return searchProductsTool.searchProductsByImage(file, 5, 0.85);
            } catch (Exception ex) {
                log.warn("Image search failed, falling back to vision chat. {}", ex.getMessage());
                // tiếp tục fallback xuống LLM
            }
        }

        // 2) Fallback: gửi ảnh + text cho LLM như trước
        org.springframework.util.MimeType mime = org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM;
        try {
            if (file != null && file.getContentType() != null) {
                mime = org.springframework.util.MimeTypeUtils.parseMimeType(file.getContentType());
            }
        } catch (Exception ignore) {}

        Media media = Media.builder()
                .mimeType(mime)
                .data(file.getResource())
                .build();

        ChatOptions chatOptions = ChatOptions.builder()
                .temperature(0.25)
                .maxTokens(2000)
                .build();

        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.media(media).text(message))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                    .options(chatOptions)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error calling Chat API with image: {}", e.getMessage());
            return "Oops, có mấy khi lỗi xảy ra khi xử lý hình ảnh! Thử lại sau nhé 😅";
        }
    }


    // ========== TOOL: dùng SEMANTIC SEARCH (Gemini) thay cho Elasticsearch ==========
    // trong ChatService.java
    // trong ChatService.SearchProductsTool
    @Component
    public static class SearchProductsTool {
        private static final double SIM_THRESHOLD = 0.7; // giữ như bạn đang dùng
        private final ObjectMapper mapper = new ObjectMapper();
        private final GeminiClient geminiClient;

        public SearchProductsTool(GeminiClient geminiClient) {
            this.geminiClient = geminiClient;
        }

        @org.springframework.ai.tool.annotation.Tool(
                name = "searchProducts",
                description = "Semantic search for products via Gemini Flask service. Use this for all product searches."
        )
        public String searchProducts(
                @org.springframework.ai.tool.annotation.ToolParam(description = "The search query keyword") String query,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Maximum number of results to return (default: 4)") Integer limit) {

            int resultLimit = (limit != null && limit > 0) ? limit : 4;
            log.info("🔍 TOOL CALLED (semantic): searchProducts query='{}', limit={}, threshold={}",
                    query, resultLimit, SIM_THRESHOLD);

            try {
                SearchResponse resp = geminiClient.semanticSearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(resultLimit)
                                .build()
                );

                if (resp == null || Boolean.FALSE.equals(resp.getSuccess()) || resp.getResults() == null) {
                    return emptyPayload(query, "không có kết quả hoặc lỗi semantic search");
                }

                var passed = resp.getResults().stream()
                        .map(r -> new ResultWrap(r.getProduct(), normalizeSimilarity(r.getSimilarityScore()), r.getMatchedText()))
                        .filter(x -> x.sim >= SIM_THRESHOLD)
                        .limit(resultLimit)
                        .toList();

                if (passed.isEmpty()) {
                    return emptyPayload(query, "độ tương đồng < " + SIM_THRESHOLD);
                }

                // Map đúng các field; giá lấy phần tử đầu tiên trong sizes
                ProductSearchPayload payload = new ProductSearchPayload();
                payload.message = (query == null || query.isBlank()) ? null
                        : ("kết quả cho: \"" + query + "\" (sim≥" + SIM_THRESHOLD + ")");

                payload.items = passed.stream().map(x -> {
                    Map<String, Object> p = x.product;

                    ProductSearchPayload.Item it = new ProductSearchPayload.Item();
                    it.id = extractId(p);                                       // _id có thể là String hoặc {"$oid": "..."}
                    it.name = strOrDefault(p.get("name"), "(Chưa có tên)");
                    it.description = strOrDefault(p.get("description"), "");
                    it.price = extractFirstPriceFromSizes(p.get("sizes"));      // CHANGED: lấy giá đầu tiên trong sizes
                    it.discount = extractDouble(p.get("percentDiscount"), 0.0); // percentDiscount: 25 hoặc 25.0
                    it.url = "/products/" + it.id;
                    it.imageUrl = pickFirstImage(p);                            // images[0].url

                    return it;
                }).toList();

                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            } catch (Exception e) {
                log.error("Semantic search error: {}", e.getMessage(), e);
                return emptyPayload(query, "lỗi xử lý kết quả semantic");
            }
        }


        @org.springframework.ai.tool.annotation.Tool(
                name = "searchProductsByImage",
                description = "Search similar products by image (multipart upload)."
        )
        public String searchProductsByImage(
                @org.springframework.ai.tool.annotation.ToolParam(description = "Image file to search") org.springframework.web.multipart.MultipartFile image,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Top K results (default 4)") Integer topK,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Min similarity (0..1), optional") Double minSimilarity
        ) {
            int tk = (topK != null && topK > 0) ? topK : 4;
            double threshold = (minSimilarity != null) ? minSimilarity : SIM_THRESHOLD; // dùng 0.85 mặc định

            try {
                var resp = geminiClient.searchByImageUpload(image, tk, 300, 8, threshold);

                if (resp == null || Boolean.FALSE.equals(resp.getSuccess()) || resp.getResults() == null) {
                    return emptyPayload("không có kết quả image search","");
                }

                var filtered = resp.getResults().stream()
                        .filter(r -> normalizeSimilarity(r.getSimilarityScore()) >= threshold)
                        .limit(tk)
                        .toList();
                for ( var r : filtered
                     ) {
                    log.info("Image search result: id={}, sim={}", extractId(r.getProduct()), normalizeSimilarity(r.getSimilarityScore()));
                }
                if (filtered.isEmpty()) {
                    return emptyPayload("độ tương đồng < " + threshold,"");
                }

                ProductSearchPayload payload = new ProductSearchPayload();
                payload.message = "kết quả tìm theo ảnh (sim≥" + threshold + ")";
                payload.items = filtered.stream().map(r -> {
                    Map<String, Object> p = r.getProduct();
                    ProductSearchPayload.Item it = new ProductSearchPayload.Item();
                    it.id = extractId(p);
                    it.name = strOrDefault(p.get("name"), "(Chưa có tên)");
                    it.description = strOrDefault(p.get("description"), "");
                    it.price = extractFirstPriceFromSizes(p.get("variants"));   // giá = size đầu tiên
                    it.discount = extractDouble(p.get("percentDiscount"), 0.0);
                    it.url = "/products/" + it.id;
                    it.imageUrl = pickFirstImage(p);                         // ảnh đầu tiên
                    return it;
                }).toList();

                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            } catch (Exception e) {
                log.error("Image search error: {}", e.getMessage(), e);
                return emptyPayload("lỗi gọi image search","lỗi");
            }
        }
        /* ------------ Helpers ------------ */

        private static class ResultWrap {
            final Map<String, Object> product;
            final double sim;
            final String matched;
            ResultWrap(Map<String, Object> product, double sim, String matched) {
                this.product = product; this.sim = sim; this.matched = matched;
            }
        }

        private static double normalizeSimilarity(Double score) {
            if (score == null) return 0.0;
            if (score > 1.0) {                    // nếu là distance
                double d = score;
                return 1.0 / (1.0 + d);
            }
            if (score < 0) return 0.0;
            return Math.min(score, 1.0);
        }

        private String emptyPayload(String query, String reason) {
            try {
                ProductSearchPayload payload = new ProductSearchPayload();
                payload.message = (query == null || query.isBlank()) ? null
                        : ("không tìm thấy sản phẩm phù hợp (" + reason + ")");
                payload.items = java.util.List.of();
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            } catch (Exception e) {
                return "{\"type\":\"product_list\",\"items\":[]}";
            }
        }

        private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
        private static String strOrDefault(Object o, String def) {
            String s = str(o);
            return s.isEmpty() ? def : s;
        }

        private static Double extractDouble(Object v, double def) {
            try {
                if (v == null) return def;
                if (v instanceof Number n) return n.doubleValue();
                if (v instanceof java.math.BigDecimal bd) return bd.doubleValue();
                if (v instanceof Map<?,?> m) { // { "$numberLong": "11" }
                    Object nl = m.get("$numberLong");
                    if (nl != null) return Double.parseDouble(String.valueOf(nl));
                }
                return Double.parseDouble(v.toString());
            } catch (Exception e) {
                return def;
            }
        }

        /** _id: String | { "$oid": "..." } */
        @SuppressWarnings("unchecked")
        private static String extractId(Map<String, Object> product) {
            Object id = product.get("_id");
            if (id instanceof Map<?,?> m) {
                Object oid = ((Map<String,Object>) m).get("$oid");
                if (oid != null) return oid.toString();
            }
            return str(id);
        }

        /** Giá = price của phần tử ĐẦU TIÊN trong mảng sizes (chuỗi hoặc số) */
        @SuppressWarnings("unchecked")
        private static Double extractFirstPriceFromSizes(Object sizes) {
            try {
                if (!(sizes instanceof List<?> list) || list.isEmpty()) return 0.0;
                Object first = list.get(0);
                if (first instanceof Map<?,?> m) {
                    Object price = ((Map<String,Object>) m).get("price");
                    return extractPriceFlexible(price);
                }
                return 0.0;
            } catch (Exception e) {
                return 0.0;
            }
        }

        /** "120000" | 120000 | BigDecimal | { $numberLong:"..." } -> double */
        private static double extractPriceFlexible(Object price) {
            if (price == null) return 0.0;
            if (price instanceof Number n) return n.doubleValue();
            if (price instanceof java.math.BigDecimal bd) return bd.doubleValue();
            if (price instanceof Map<?,?> m) {
                Object nl = ((Map<?,?>) m).get("$numberLong");
                if (nl != null) { try { return Double.parseDouble(String.valueOf(nl)); } catch (Exception ignore) {} }
            }
            try { return Double.parseDouble(price.toString()); } catch (Exception e) { return 0.0; }
        }

        /** images: [{url, position}] | ["..."] → lấy url đầu tiên không phải .mp4 */
        @SuppressWarnings("unchecked")
        private static String pickFirstImage(Map<String, Object> productMap) {
            try {
                Object images = productMap.get("images");
                if (images instanceof List<?> list && !list.isEmpty()) {
                    for (Object el : list) {
                        if (el instanceof Map<?,?> m) {
                            Object url = ((Map<String, Object>) m).get("url");
                            if (url != null && !url.toString().toLowerCase().endsWith(".mp4")) {
                                return url.toString();
                            }
                        } else if (el instanceof String s) {
                            if (!s.toLowerCase().endsWith(".mp4")) return s;
                        }
                    }
                }
            } catch (Exception ignore) {}
            return "/img/default.png";
        }
    }

}
