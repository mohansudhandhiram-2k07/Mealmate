package com.messmate.dto;

import com.messmate.entity.Category;
import com.messmate.entity.Meal;
import com.messmate.entity.MealType;
import com.messmate.entity.MenuItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class MealDtos {
    private MealDtos() {}

    public record MenuItemRequest(
            @NotBlank(message = "item name is required")
            @Size(max = 80, message = "item name must be at most 80 characters") String name,
            @NotNull(message = "item category is required (MAIN, SIDE, DESSERT, BEVERAGE)") Category category,
            boolean veg) {}

    /** Used for both creating a meal (POST) and replacing its items (PUT). */
    public record MealRequest(
            @NotNull(message = "date is required (yyyy-MM-dd)") LocalDate date,
            @NotNull(message = "mealType is required (BREAKFAST, LUNCH, DINNER)") MealType mealType,
            @NotEmpty(message = "add at least one menu item") @Valid List<MenuItemRequest> items) {}

    public record MenuItemResponse(Long id, String name, Category category, boolean veg,
                                   Long mealId, LocalDate mealDate, MealType mealType) {
        public static MenuItemResponse from(MenuItem i) {
            Meal m = i.getMeal();
            return new MenuItemResponse(i.getId(), i.getName(), i.getCategory(), i.isVeg(),
                    m.getId(), m.getMealDate(), m.getMealType());
        }
    }

    public record MealResponse(Long id, LocalDate date, MealType mealType, List<MenuItemResponse> items) {
        public static MealResponse from(Meal m) {
            return new MealResponse(m.getId(), m.getMealDate(), m.getMealType(),
                    m.getItems().stream().map(MenuItemResponse::from).toList());
        }
    }
}
