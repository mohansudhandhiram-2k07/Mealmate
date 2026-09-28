package com.messmate.repository;

import com.messmate.entity.Meal;
import com.messmate.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {

    boolean existsByMealDateAndMealType(LocalDate mealDate, MealType mealType);

    List<Meal> findByMealDateBetween(LocalDate start, LocalDate end);

    List<Meal> findByMealDate(LocalDate mealDate);
}
