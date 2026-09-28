package com.messmate.controller;

import com.messmate.dto.MealDtos.MealRequest;
import com.messmate.dto.MealDtos.MealResponse;
import com.messmate.dto.MealDtos.MenuItemResponse;
import com.messmate.entity.Category;
import com.messmate.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MealController {

    private final MealService service;

    /** Weekly menu. weekStart is optional; any date inside the week works, default is the current week. */
    @GetMapping("/meals")
    public List<MealResponse> week(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.week(weekStart);
    }

    @GetMapping("/meals/{id}")
    public MealResponse one(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/meals")
    public ResponseEntity<MealResponse> create(@Valid @RequestBody MealRequest req) {
        MealResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/meals/" + created.id())).body(created);
    }

    @PutMapping("/meals/{id}")
    public MealResponse update(@PathVariable Long id, @Valid @RequestBody MealRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/meals/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Search/filter menu items by name fragment and/or category. */
    @GetMapping("/menu-items")
    public List<MenuItemResponse> items(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) Category category) {
        return service.searchItems(q, category);
    }
}
