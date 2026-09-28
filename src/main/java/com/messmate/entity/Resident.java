package com.messmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "residents")
@Getter @Setter @NoArgsConstructor
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "room_no", nullable = false, length = 10)
    private String roomNo;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** A resident has many feedback entries. Deleting the resident deletes them too. */
    @OneToMany(mappedBy = "resident", cascade = CascadeType.REMOVE)
    private List<Feedback> feedbacks = new ArrayList<>();

    /**
     * Opt-outs: many residents can skip many meals, so this is a many-to-many.
     * Resident owns the relationship; rows live in the join table meal_opt_outs.
     */
    @ManyToMany
    @JoinTable(name = "meal_opt_outs",
            joinColumns = @JoinColumn(name = "resident_id"),
            inverseJoinColumns = @JoinColumn(name = "meal_id"))
    private Set<Meal> skippedMeals = new HashSet<>();
}
