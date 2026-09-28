package com.messmate.service;

import com.messmate.dto.FeedbackDtos.FeedbackRequest;
import com.messmate.dto.FeedbackDtos.FeedbackResponse;
import com.messmate.dto.FeedbackDtos.FeedbackUpdate;
import com.messmate.entity.Feedback;
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

import java.time.LocalDateTime;
import java.util.List;

/** Ratings (1-5) and comments. Every business rule is checked here, before anything is saved. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbacks;
    private final ResidentRepository residents;
    private final MealRepository meals;

    @Transactional
    public FeedbackResponse submit(FeedbackRequest req) {
        Resident resident = residents.findById(req.residentId())
                .orElseThrow(() -> ApiException.notFound("Resident", req.residentId()));
        Meal meal = meals.findById(req.mealId())
                .orElseThrow(() -> ApiException.notFound("Meal", req.mealId()));

        // Rule: opted-out meals were not eaten, so they cannot be rated.
        if (residents.countOptOut(resident.getId(), meal.getId()) > 0) {
            throw ApiException.conflict(resident.getName() + " opted out of this meal, so it cannot be rated.");
        }
        // Rule: a meal that has not been served yet cannot be rated.
        if (meal.getMealDate().isAfter(Dates.today())) {
            throw ApiException.badRequest("This meal is on " + meal.getMealDate() + ". Ratings open on the meal day.");
        }
        // Rule: one feedback entry per resident per meal per day (a meal is one date + one type).
        if (feedbacks.existsByResidentIdAndMealId(resident.getId(), meal.getId())) {
            throw ApiException.conflict(resident.getName() + " has already rated this meal. Update the existing rating instead.");
        }

        Feedback f = new Feedback();
        f.setResident(resident);
        f.setMeal(meal);
        f.setRating(req.rating());
        f.setComment(clean(req.comment()));
        f.setSubmittedAt(LocalDateTime.now(Dates.ZONE));
        return FeedbackResponse.from(feedbacks.save(f));
    }

    @Transactional
    public FeedbackResponse update(Long id, FeedbackUpdate req) {
        Feedback f = find(id);
        f.setRating(req.rating());
        f.setComment(clean(req.comment()));
        return FeedbackResponse.from(f);
    }

    @Transactional
    public void delete(Long id) {
        feedbacks.delete(find(id));
    }

    /** Filter by meal or by resident; with no filter, returns everything. */
    public List<FeedbackResponse> list(Long residentId, Long mealId) {
        List<Feedback> found;
        if (mealId != null) found = feedbacks.findByMealId(mealId);
        else if (residentId != null) found = feedbacks.findByResidentId(residentId);
        else found = feedbacks.findAll();
        return found.stream().map(FeedbackResponse::from).toList();
    }

    private Feedback find(Long id) {
        return feedbacks.findById(id).orElseThrow(() -> ApiException.notFound("Feedback", id));
    }

    private String clean(String comment) {
        return (comment == null || comment.isBlank()) ? null : comment.trim();
    }
}
