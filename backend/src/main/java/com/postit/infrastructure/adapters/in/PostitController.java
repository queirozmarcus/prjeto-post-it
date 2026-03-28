package com.postit.infrastructure.adapters.in;

import com.postit.application.ports.PostitServicePort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.Postit;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/postits")
public class PostitController {

    private final PostitServicePort service;
    private final UserRepositoryPort userRepository;

    public PostitController(PostitServicePort service, UserRepositoryPort userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<PostitResponse> create(
            @RequestBody @Valid PostitRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Postit domain = Postit.create(request.content(), request.color(), userId);
        Postit saved = service.create(domain, userId);
        return ResponseEntity
                .created(URI.create("/api/v1/postits/" + saved.id()))
                .body(PostitResponse.fromDomain(saved));
    }

    @GetMapping
    public List<PostitResponse> findAll(Authentication authentication) {
        Long userId = getUserId(authentication);
        return service.findAllByUser(userId).stream()
                .map(PostitResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostitResponse> findById(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        // Retorna 404 quando não pertence ao usuário — não revela existência do postit de outro usuário
        return service.findById(id, userId)
                .map(PostitResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostitResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid PostitRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        // Postit parcial — userId e timestamps serão preservados pelo use case
        Postit domain = new Postit(id, request.content(), request.color(), null, null, null);
        Postit updated = service.update(id, domain, userId);
        return ResponseEntity.ok(PostitResponse.fromDomain(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        service.delete(id, userId);
    }

    // Resolve o userId a partir do email contido no token JWT
    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email))
                .id();
    }
}
