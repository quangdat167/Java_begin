package vn.dangquangdat.javabegin.conversation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.dangquangdat.javabegin.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat_conversations")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<ConversationService.ConversationView>> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "") String keyword,
            Authentication authentication
    ) {
        Page<ConversationService.ConversationView> result =
                service.findAll(authentication.getName(), keyword, page, limit);
        return ApiResponse.page(result);
    }

    @GetMapping("/{id}")
    ApiResponse<ConversationService.ConversationView> findOne(
            @PathVariable long id,
            Authentication authentication
    ) {
        return ApiResponse.success(service.findOne(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ConversationService.ConversationView> create(
            @Valid @RequestBody ConversationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Conversation created",
                service.create(request.title(), authentication.getName()));
    }

    @PutMapping("/{id}")
    ApiResponse<ConversationService.ConversationView> rename(
            @PathVariable long id,
            @Valid @RequestBody ConversationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Conversation renamed",
                service.rename(id, request.title(), authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id, Authentication authentication) {
        service.delete(id, authentication.getName());
    }

    public record ConversationRequest(
            @NotBlank(message = "title is required")
            @Size(max = 120, message = "title can contain at most 120 characters")
            String title
    ) {
    }
}

