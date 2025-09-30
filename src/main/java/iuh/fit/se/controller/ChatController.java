package iuh.fit.se.controller;

import iuh.fit.se.dto.ChatRequest;
import iuh.fit.se.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    String chat(@RequestBody ChatRequest request)
    {
        return chatService.chat(request);
    }

    @PostMapping("/chat-with-image")
    String chatWithImage(@RequestPart("file")MultipartFile file,
                         @RequestParam("message") String message,
                         @RequestParam("conversationId") String conversationId ) throws Exception {
        return chatService.chatWithImage(file,message,conversationId);
    }


}
