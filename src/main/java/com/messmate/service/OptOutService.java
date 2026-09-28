package com.messmate.service;

import com.messmate.entity.Meal;
import com.messmate.entity.Resident;
import com.messmate.exception.ApiException;
import com.messmate.repository.FeedbackRepository;
import com.messmate.repository.MealRepository;
import com.messmate.repository.ResidentRepository;
import com.messmate.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** A resident skipping a meal. Opted-out meals are left out of the headcount. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptOutService {

    private final ResidentRepository residents;
    private final MealRepository meals;
    private final FeedbackRepository feedbacks;

    @Transactional
    public void skip(Long residentId, Long mealId) {
        Resident resident = residents.findById(residentId).orElseThrow(() -> ApiException.notFound("Resident", residentId));
        Meal meal = meals.findById(mealId).orElseThrow(() -> ApiException.notFound("Meal", mealId));

        if (meal.getMealDate().isBefore(Dates.today())) {
            throw ApiException.badRequest("This meal is already over, so it cannot be skipped.");
        }
        if (feedbacks.existsByResidentIdAndMealId(residentId, mealId)) {
            throw ApiException.conflict("You already rated this meal, so it cannot be skipped.");
        }
        if (residents.countOptOut(residentId, mealId) > 0) {
            throw ApiException.conflict("This meal is already marked as skipped.");
        }
        resident.getSkippedMeals().add(meal);
    }

    @Transactional
    public void undo(Long residentId, Long mealId) {
        Resident resident = residents.findById(residentId).orElseThrow(() -> ApiException.notFound("Resident", residentId));
        Meal meal = meals.findById(mealId).orElseThrow(() -> ApiException.notFound("Meal", mealId));
        if (!resident.getSkippedMeals().remove(meal)) {
            throw ApiException.notFound("Opt-out for meal", mealId);
        }
    }

    /** Ids of every meal this resident has opted out of. */
    public List<Long> skippedMealIds(Long residentId) {
        if (!residents.existsById(residentId)) throw ApiException.notFound("Resident", residentId);
        return residents.findSkippedMealIds(residentId);
    }
}
