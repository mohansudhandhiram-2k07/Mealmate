package com.messmate.dto;

import com.messmate.entity.Feedback;
import com.messmate.entity.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    public record FeedbackRequest(
            @NotNull(message = "residentId is required") @Positive(message = "residentId must be positive") Long residentId,
            @NotNull(message = "mealId is required") @Positive(message = "mealId must be positive") Long mealId,
            @NotNull(message = "rating is required")
            @Min(value = 1, message = "rating must be between 1 and 5")
            @Max(value = 5, message = "rating must be between 1 and 5") Integer rating,
            @Size(max = 500, message = "comment must be at most 500 characters") String comment) {}

    /** Editing a rating only changes the score and comment, never who rated or which meal. */
    public record FeedbackUpdate(
            @NotNull(message = "rating is required")
            @Min(value = 1, message = "rating must be between 1 and 5")
            @Max(value = 5, message = "rating must be between 1 and 5") Integer rating,
            @Size(max = 500, message = "comment must be at most 500 characters") String comment) {}

    public record FeedbackResponse(Long id, Long residentId, String residentName, Long mealId,
                                   LocalDate mealDate, MealType mealType, Integer rating,
                                   String comment, LocalDateTime submittedAt) {
        public static FeedbackResponse from(Feedback f) {
            return new FeedbackResponse(f.getId(), f.getResident().getId(), f.getResident().getName(),
                    f.getMeal().getId(), f.getMeal().getMealDate(), f.getMeal().getMealType(),
                    f.getRating(), f.getComment(), f.getSubmittedAt());
        }
    }
}
