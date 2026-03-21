package com.postit.infrastructure.adapters.in;

import com.postit.application.ports.PostitServicePort;
import com.postit.domain.Postit;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/postits")
public class PostitController {

    private final PostitServicePort service;

    public PostitController(PostitServicePort service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PostitResponse> create(@RequestBody @Valid PostitRequest request) {
        Postit domain = Postit.create(request.content(), request.color());
        Postit saved = service.create(domain);
        PostitResponse response = PostitResponse.fromDomain(saved);
        
        return ResponseEntity
                .created(URI.create("/api/v1/postits/" + saved.id()))
                .body(response);
    }

    @GetMapping
    public List<PostitResponse> findAll() {
        return service.findAll().stream()
                .map(PostitResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostitResponse> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(PostitResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostitResponse> update(@PathVariable Long id, @RequestBody @Valid PostitRequest request) {
        Postit domain = new Postit(id, request.content(), request.color(), null, null);
        Postit updated = service.update(id, domain);
        return ResponseEntity.ok(PostitResponse.fromDomain(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
