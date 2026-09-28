package com.messmate.config;

import com.messmate.entity.*;
import com.messmate.repository.FeedbackRepository;
import com.messmate.repository.MealRepository;
import com.messmate.repository.ResidentRepository;
import com.messmate.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo data so the app is not empty on first run: 8 residents plus last week's and this week's
 * menus with ratings and opt-outs. Runs only when messmate.seed=true AND the database has no residents.
 * Item format: "Name:CATEGORY:V" (veg) or ":N" (non-veg).
 */
@Component
@ConditionalOnProperty(name = "messmate.seed", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String[][][] MENU = {
        { // breakfast, Monday to Sunday
            {"Idli:MAIN:V", "Sambar:SIDE:V", "Coconut chutney:SIDE:V", "Filter coffee:BEVERAGE:V"},
            {"Pongal:MAIN:V", "Medu vada:SIDE:V", "Sambar:SIDE:V", "Tea:BEVERAGE:V"},
            {"Dosa:MAIN:V", "Tomato chutney:SIDE:V", "Sambar:SIDE:V", "Milk:BEVERAGE:V"},
            {"Poori:MAIN:V", "Potato masala:SIDE:V", "Tea:BEVERAGE:V"},
            {"Upma:MAIN:V", "Coconut chutney:SIDE:V", "Banana:DESSERT:V", "Coffee:BEVERAGE:V"},
            {"Chapati:MAIN:V", "Veg kurma:SIDE:V", "Boiled egg:SIDE:N", "Tea:BEVERAGE:V"},
            {"Aloo paratha:MAIN:V", "Curd:SIDE:V", "Pickle:SIDE:V", "Coffee:BEVERAGE:V"}
        },
        { // lunch
            {"Steamed rice:MAIN:V", "Sambar:SIDE:V", "Beans poriyal:SIDE:V", "Rasam:SIDE:V", "Curd:SIDE:V"},
            {"Veg biryani:MAIN:V", "Onion raita:SIDE:V", "Gobi 65:SIDE:V"},
            {"Steamed rice:MAIN:V", "Vathal kuzhambu:SIDE:V", "Cabbage poriyal:SIDE:V", "Appalam:SIDE:V"},
            {"Lemon rice:MAIN:V", "Potato fry:SIDE:V", "Curd:SIDE:V"},
            {"Steamed rice:MAIN:V", "Dal fry:SIDE:V", "Chicken curry:SIDE:N", "Rasam:SIDE:V"},
            {"Chicken biryani:MAIN:N", "Raita:SIDE:V", "Boiled egg:SIDE:N"},
            {"Full meals:MAIN:V", "Sambar:SIDE:V", "Kootu:SIDE:V", "Payasam:DESSERT:V"}
        },
        { // dinner
            {"Chapati:MAIN:V", "Paneer butter masala:SIDE:V"},
            {"Dosa:MAIN:V", "Sambar:SIDE:V", "Mint chutney:SIDE:V"},
            {"Fried rice:MAIN:V", "Gobi manchurian:SIDE:V"},
            {"Idiyappam:MAIN:V", "Veg kurma:SIDE:V"},
            {"Parotta:MAIN:V", "Salna:SIDE:V", "Omelette:SIDE:N"},
            {"Chapati:MAIN:V", "Dal tadka:SIDE:V", "Milk:BEVERAGE:V"},
            {"Veg pulao:MAIN:V", "Raita:SIDE:V", "Ice cream:DESSERT:V"}
        }
    };

    private static final String[] LOW = {"Cold and bland", "Too oily today", "Rice was undercooked", "Not enough quantity"};
    private static final String[] MID = {"Okay, nothing special", "Could use more salt", "Decent"};
    private static final String[] HIGH = {"Really good today", "Tasty, please repeat", "Fresh and hot", "Loved it"};

    private final ResidentRepository residents;
    private final MealRepository meals;
    private final FeedbackRepository feedbacks;

    @Override
    @Transactional
    public void run(String... args) {
        if (residents.count() > 0) return;

        String[][] people = {
            {"Arun Kumar", "A-101"}, {"Bharath S", "A-102"}, {"Deepak R", "A-204"}, {"Harish M", "B-110"},
            {"Karthik V", "B-115"}, {"Naveen P", "B-207"}, {"Sanjay T", "C-302"}, {"Vignesh K", "C-305"}
        };
        List<Resident> rs = new ArrayList<>();
        for (String[] p : people) {
            Resident r = new Resident();
            r.setName(p[0]);
            r.setRoomNo(p[1]);
            r.setEmail(p[0].toLowerCase().replace(' ', '.') + "@hostel.example");
            rs.add(residents.save(r));
        }

        LocalDate today = Dates.today();
        LocalDate thisMonday = Dates.weekStart(today);
        Random rnd = new Random(7);

        for (int w = -1; w <= 0; w++) {
            for (int d = 0; d < 7; d++) {
                LocalDate date = thisMonday.plusWeeks(w).plusDays(d);
                for (int t = 0; t < 3; t++) {
                    Meal meal = new Meal();
                    meal.setMealDate(date);
                    meal.setMealType(MealType.values()[t]);
                    for (String s : MENU[t][d]) {
                        String[] p = s.split(":");
                        MenuItem item = new MenuItem();
                        item.setName(p[0]);
                        item.setCategory(Category.valueOf(p[1]));
                        item.setVeg(p[2].equals("V"));
                        item.setMeal(meal);
                        meal.getItems().add(item);
                    }
                    meals.save(meal);

                    for (int k = 0; k < rs.size(); k++) {
                        Resident r = rs.get(k);
                        if ((d + t + k) % 7 == 0) {            // some residents skip some meals
                            r.getSkippedMeals().add(meal);
                            continue;
                        }
                        if (date.isAfter(today) || rnd.nextInt(10) >= 7) continue; // only served meals, ~70% rate
                        int base = ((d * 3 + t) % 5 == 2) ? 2 : 4;      // a few meals are genuinely bad
                        int rating = Math.max(1, Math.min(5, base + rnd.nextInt(3) - 1));
                        String[] pool = rating <= 2 ? LOW : rating == 3 ? MID : HIGH;
                        Feedback f = new Feedback();
                        f.setResident(r);
                        f.setMeal(meal);
                        f.setRating(rating);
                        f.setComment(pool[rnd.nextInt(pool.length)]);
                        f.setSubmittedAt(LocalDateTime.now(Dates.ZONE));
                        feedbacks.save(f);
                    }
                }
            }
        }
    }
}
