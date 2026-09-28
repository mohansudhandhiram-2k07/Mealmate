package com.messmate.controller;

import com.messmate.dto.ResidentDtos.ResidentRequest;
import com.messmate.dto.ResidentDtos.ResidentResponse;
import com.messmate.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService service;

    @GetMapping
    public List<ResidentResponse> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResidentResponse one(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ResidentResponse> create(@Valid @RequestBody ResidentRequest req) {
        ResidentResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/residents/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResidentResponse update(@PathVariable Long id, @Valid @RequestBody ResidentRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
