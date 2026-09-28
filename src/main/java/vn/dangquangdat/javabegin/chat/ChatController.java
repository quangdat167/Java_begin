package vn.dangquangdat.javabegin.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dangquangdat.javabegin.common.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    ApiResponse<ChatService.ChatView> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        ChatService.ChatRequest command = new ChatService.ChatRequest(
                request.message(), request.conversationId(), request.dataContext(), request.url(), request.modelName()
        );
        return ApiResponse.success(service.send(command, authentication.getName()));
    }

    @GetMapping("/chat_conversations/{id}/turns")
    ApiResponse<List<ChatService.ChatView>> history(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(service.history(id, authentication.getName()));
    }

    @GetMapping("/chat_conversations/{id}/chat-status")
    ApiResponse<Map<String, Object>> status(@PathVariable long id, Authentication authentication) {
        service.history(id, authentication.getName());
        return ApiResponse.success(Map.of("chatStatus", "completed", "processingPercent", 100));
    }

    @GetMapping("/models")
    ApiResponse<List<ModelView>> models() {
        return ApiResponse.success(List.of(
                new ModelView(1, "demo-java-model"),
                new ModelView(2, "interview-coach")
        ));
    }

    public record ChatRequest(
            @NotBlank String message,
            @Positive @JsonProperty("conversation_id") long conversationId,
            @JsonProperty("data_context") String dataContext,
            List<String> url,
            String modelName
    ) {
    }

    public record ModelView(long id, String name) {
    }
}
