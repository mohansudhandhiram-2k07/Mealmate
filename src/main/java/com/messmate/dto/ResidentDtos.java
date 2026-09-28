package com.messmate.dto;

import com.messmate.entity.Resident;
import jakarta.validation.constraints.*;

public final class ResidentDtos {
    private ResidentDtos() {}

    public record ResidentRequest(
            @NotBlank(message = "name is required")
            @Size(max = 80, message = "name must be at most 80 characters") String name,
            @NotBlank(message = "room number is required")
            @Size(max = 10, message = "room number must be at most 10 characters") String roomNo,
            @NotBlank(message = "email is required")
            @Email(message = "email is not a valid address")
            @Size(max = 120, message = "email must be at most 120 characters") String email) {}

    public record ResidentResponse(Long id, String name, String roomNo, String email) {
        public static ResidentResponse from(Resident r) {
            return new ResidentResponse(r.getId(), r.getName(), r.getRoomNo(), r.getEmail());
        }
    }

    public record OptOutRequest(
            @NotNull(message = "residentId is required") @Positive(message = "residentId must be positive") Long residentId,
            @NotNull(message = "mealId is required") @Positive(message = "mealId must be positive") Long mealId) {}
}
