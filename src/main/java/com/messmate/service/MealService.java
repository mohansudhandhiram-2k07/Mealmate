package com.messmate.service;

import com.messmate.dto.MealDtos.MealRequest;
import com.messmate.dto.MealDtos.MealResponse;
import com.messmate.dto.MealDtos.MenuItemRequest;
import com.messmate.dto.MealDtos.MenuItemResponse;
import com.messmate.entity.Category;
import com.messmate.entity.Meal;
import com.messmate.entity.MenuItem;
import com.messmate.exception.ApiException;
import com.messmate.repository.MealRepository;
import com.messmate.repository.MenuItemRepository;
import com.messmate.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Weekly menu: meals per day, each made of menu items. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealService {

    private final MealRepository meals;
    private final MenuItemRepository menuItems;

    /** All meals of the Monday-to-Sunday week containing weekStart (current week when null), in serving order. */
    public List<MealResponse> week(LocalDate weekStart) {
        LocalDate start = Dates.weekStart(weekStart);
        return meals.findByMealDateBetween(start, start.plusDays(6)).stream()
                .sorted(Comparator.comparing(Meal::getMealDate).thenComparing(Meal::getMealType))
                .map(MealResponse::from)
                .toList();
    }

    public MealResponse get(Long id) {
        return MealResponse.from(find(id));
    }

    @Transactional
    public MealResponse create(MealRequest req) {
        if (meals.existsByMealDateAndMealType(req.date(), req.mealType())) {
            throw ApiException.conflict(label(req) + " is already published. Edit that meal instead.");
        }
        Meal meal = new Meal();
        meal.setMealDate(req.date());
        meal.setMealType(req.mealType());
        replaceItems(meal, req.items());
        return MealResponse.from(meals.save(meal));
    }

    @Transactional
    public MealResponse update(Long id, MealRequest req) {
        Meal meal = find(id);
        boolean slotChanged = !meal.getMealDate().equals(req.date()) || meal.getMealType() != req.mealType();
        if (slotChanged && meals.existsByMealDateAndMealType(req.date(), req.mealType())) {
            throw ApiException.conflict(label(req) + " is already published. Pick a different slot.");
        }
        meal.setMealDate(req.date());
        meal.setMealType(req.mealType());
        replaceItems(meal, req.items());
        return MealResponse.from(meal);
    }

    /** Deleting a meal also deletes its items and feedback (cascade) and clears its opt-outs. */
    @Transactional
    public void delete(Long id) {
        Meal meal = find(id);
        meal.getSkippedBy().forEach(r -> r.getSkippedMeals().remove(meal));
        meals.delete(meal);
    }

    /** Menu item search/filter: by name fragment and/or category. */
    public List<MenuItemResponse> searchItems(String q, Category category) {
        String name = (q == null || q.isBlank()) ? null : q.trim();
        List<MenuItem> found;
        if (name != null && category != null) found = menuItems.findByCategoryAndNameContainingIgnoreCase(category, name);
        else if (category != null) found = menuItems.findByCategory(category);
        else if (name != null) found = menuItems.findByNameContainingIgnoreCase(name);
        else found = menuItems.findAll();
        return found.stream()
                .sorted(Comparator.comparing((MenuItem i) -> i.getMeal().getMealDate()).reversed())
                .map(MenuItemResponse::from)
                .toList();
    }

    Meal find(Long id) {
        return meals.findById(id).orElseThrow(() -> ApiException.notFound("Meal", id));
    }

    private void replaceItems(Meal meal, List<MenuItemRequest> requested) {
        meal.getItems().clear(); // orphanRemoval deletes the old rows
        for (MenuItemRequest r : requested) {
            MenuItem item = new MenuItem();
            item.setName(r.name().trim());
            item.setCategory(r.category());
            item.setVeg(r.veg());
            item.setMeal(meal);
            meal.getItems().add(item);
        }
    }

    private String label(MealRequest req) {
        return req.mealType() + " on " + req.date();
    }
}
