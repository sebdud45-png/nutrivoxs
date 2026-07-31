package com.nutrivox.app;

import java.util.ArrayList;
import java.util.List;

public class MealAnalysis {
    public String mealName = "";
    public String warning = "";
    public final List<FoodItem> foods = new ArrayList<>();

    public double totalEnergy() {
        double v = 0; for (FoodItem f : foods) v += f.energyKcal; return v;
    }
    public double totalProtein() {
        double v = 0; for (FoodItem f : foods) v += f.proteins; return v;
    }
    public double totalSodium() {
        double v = 0; for (FoodItem f : foods) v += f.sodiumMg; return v;
    }
    public double totalPotassium() {
        double v = 0; for (FoodItem f : foods) v += f.potassiumMg; return v;
    }
    public double totalPhosphorus() {
        double v = 0; for (FoodItem f : foods) v += f.phosphorusMg; return v;
    }
}
