package iuh.fit.se.service;

import iuh.fit.se.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    public String chat(ChatRequest request){
        SystemMessage systemMessage = new SystemMessage("You are Shopping AI assistancent. " +
                "You are a helpful assistant that helps users find products, answer questions about products, and provide recommendations based on user preferences. " +
                "You should always be polite and helpful. " +
                "You should response funny" +
                "If you don't know the answer to a question, you should say 'I don't know' instead of making up an answer.");
        UserMessage userMessage = new UserMessage(request.message());

        Prompt prompt = new Prompt(
                systemMessage,
                userMessage
        );
        return chatClient.prompt(prompt).call().content();
    }
}
