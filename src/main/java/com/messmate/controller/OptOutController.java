package com.messmate.controller;

import com.messmate.dto.ResidentDtos.OptOutRequest;
import com.messmate.service.OptOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/opt-outs")
@RequiredArgsConstructor
public class OptOutController {

    private final OptOutService service;

    @PostMapping
    public ResponseEntity<Void> skip(@Valid @RequestBody OptOutRequest req) {
        service.skip(req.residentId(), req.mealId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> undo(@RequestParam Long residentId, @RequestParam Long mealId) {
        service.undo(residentId, mealId);
        return ResponseEntity.noContent().build();
    }

    /** Ids of the meals a resident has opted out of. */
    @GetMapping
    public List<Long> skipped(@RequestParam Long residentId) {
        return service.skippedMealIds(residentId);
    }
}
