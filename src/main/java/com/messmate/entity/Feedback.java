package com.messmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A resident's rating of one meal. Since a meal is already one date + one type,
 * unique(resident_id, meal_id) means "one feedback per resident per meal per day".
 * The service layer checks this first; the constraint is the safety net.
 */
@Entity
@Table(name = "feedback",
        uniqueConstraints = @UniqueConstraint(name = "uk_feedback_resident_meal", columnNames = {"resident_id", "meal_id"}))
@Getter @Setter @NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "comment_text", length = 500)
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
