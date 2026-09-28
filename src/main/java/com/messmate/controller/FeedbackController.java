package com.messmate.controller;

import com.messmate.dto.FeedbackDtos.FeedbackRequest;
import com.messmate.dto.FeedbackDtos.FeedbackResponse;
import com.messmate.dto.FeedbackDtos.FeedbackUpdate;
import com.messmate.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService service;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest req) {
        FeedbackResponse created = service.submit(req);
        return ResponseEntity.created(URI.create("/api/feedback/" + created.id())).body(created);
    }

    @GetMapping
    public List<FeedbackResponse> list(@RequestParam(required = false) Long residentId,
                                       @RequestParam(required = false) Long mealId) {
        return service.list(residentId, mealId);
    }

    @PutMapping("/{id}")
    public FeedbackResponse update(@PathVariable Long id, @Valid @RequestBody FeedbackUpdate req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
