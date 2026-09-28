package com.messmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One serving slot: a date plus a meal type. The pair (date, type) is unique. */
@Entity
@Table(name = "meals",
        uniqueConstraints = @UniqueConstraint(name = "uk_meal_date_type", columnNames = {"meal_date", "meal_type"}))
@Getter @Setter @NoArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 12)
    private MealType mealType;

    /** A meal is made of many menu items. Items cannot exist without their meal. */
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MenuItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "meal", cascade = CascadeType.REMOVE)
    private List<Feedback> feedbacks = new ArrayList<>();

    /** Inverse side of Resident.skippedMeals. */
    @ManyToMany(mappedBy = "skippedMeals")
    private Set<Resident> skippedBy = new HashSet<>();
}
