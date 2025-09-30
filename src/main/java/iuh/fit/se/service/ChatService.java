package iuh.fit.se.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.dto.ChatRequest;
import iuh.fit.se.dto.ProductSearchPayload;
import iuh.fit.se.entity.ProductElastic;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
                       ProductService productService,
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
        // Kiểm tra yêu cầu tìm kiếm sản phẩm
        if (isProductSearchRequest(userMessage)) {
            String query = extractQuery(userMessage);
            int limit = 3; // Mặc định 5 sản phẩm
            log.info("Manual product search: query={}, limit={}", query, limit);
            return searchProductsTool.searchProducts(query, limit);
        }

        // Gọi LLM cho các yêu cầu khác
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
        // Kiểm tra các từ khóa tìm kiếm
        String[] keywords = {"tìm", "có", "sản phẩm", "mua", "giày", "áo", "quần"};
        return Arrays.stream(keywords).anyMatch(message::contains);
    }

    private String extractQuery(String message) {
        // Loại bỏ từ khóa tìm kiếm, lấy query
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

        Media media = Media.builder()
                .mimeType(org.springframework.util.MimeTypeUtils.parseMimeType(file.getContentType()))
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
            return "Oops, có lỗi xảy ra khi xử lý hình ảnh! Thử lại sau nhé 😅";
        }
    }

    // giữ nguyên inner class, nhưng KHÔNG phụ thuộc ChatService
    @Component
    public static class SearchProductsTool {
        private final ProductService productService;
        private final ObjectMapper mapper = new ObjectMapper();

        public SearchProductsTool(ProductService productService) {
            this.productService = productService;
        }

        @org.springframework.ai.tool.annotation.Tool(name = "searchProducts",
                description = "Search for products by keyword or description using Elasticsearch full-text search with vietnamese_analyzer. Use this for general product searches.")
        public String searchProducts(
                @org.springframework.ai.tool.annotation.ToolParam(description = "The search query keyword") String query,
                @org.springframework.ai.tool.annotation.ToolParam(description = "Maximum number of results to return (default: 3)") Integer limit) {

            int resultLimit = (limit != null && limit > 0) ? limit : 3;

            log.info("🔍 TOOL CALLED: searchProducts with query: {}, limit: {}", query, resultLimit);
            List<ProductElastic> products = productService.searchProducts(query);

            ProductSearchPayload payload = new ProductSearchPayload();
            payload.message = (query == null || query.isBlank()) ? null : ("Kết quả cho: \"" + query + "\"");

            if (products == null || products.isEmpty()) {
                payload.items = java.util.List.of();
                try {
                    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
                } catch (Exception e) {
                    log.error("JSON serialize error (empty): {}", e.getMessage());
                    return "{\"type\":\"product_list\",\"items\":[],\"message\":\"Không tìm thấy sản phẩm phù hợp\"}";
                }
            }

            payload.items = products.stream()
                    .limit(resultLimit)
                    .map(p -> {
                        ProductSearchPayload.Item it = new ProductSearchPayload.Item();
                        it.id = safeId(p);
                        it.name = (p.getOriginalName() != null) ? p.getOriginalName() : "(Chưa có tên)";
                        it.price = extractPrice(p);
                        it.discount = (p.getPercentDiscount() != null) ? p.getPercentDiscount().doubleValue() : 0.0;
                        it.description = (p.getDescription() != null) ? p.getDescription() : "";
                        it.url = "/products/" + it.id;
                        it.imageUrl = pickImageUrl(p);
                        return it;
                    })
                    .toList();

            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            } catch (Exception e) {
                log.error("JSON serialize error: {}", e.getMessage());
                return "{\"type\":\"product_list\",\"items\":[],\"message\":\"Có lỗi khi tạo dữ liệu kết quả\"}";
            }
        }

        // --- Helpers ---
        private static String safeId(ProductElastic p) {
            try { return p.getId(); } catch (Exception e) { return ""; }
        }

        private static Double extractPrice(ProductElastic p) {
            try {
                if (p.getSizes() != null && !p.getSizes().isEmpty() && p.getSizes().get(0).price() != null) {
                    Object val = p.getSizes().get(0).price();
                    if (val instanceof Number n) return n.doubleValue();
                    if (val instanceof BigDecimal bd) return bd.doubleValue();
                    return Double.parseDouble(val.toString());
                }
            } catch (Exception ignore) {}
            return 0.0;
        }

        private static String pickImageUrl(ProductElastic p) {
            try {
                var imgs = p.getImages();
                if (imgs != null) {
                    for (var img : imgs) {
                        var url = String.valueOf(img.url());
                        if (url != null && !url.toLowerCase().endsWith(".mp4")) {
                            return url;
                        }
                    }
                }
            } catch (Exception ignore) {}
            return "/img/default.png"; // fallback FE có thể override
        }
    }
}
