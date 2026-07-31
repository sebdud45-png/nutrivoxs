package com.nutrivox.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int VOICE_REQUEST = 10;
    private static final int PHOTO_REQUEST = 11;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final int GREEN = Color.rgb(7,143,85);
    private final int DARK = Color.rgb(18,35,27);
    private final int MUTED = Color.rgb(92,108,99);
    private LinearLayout content;
    private ProgressBar progress;
    private LocalDatabase database;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        database = new LocalDatabase(this);
        prefs = getSharedPreferences("nutrivox", MODE_PRIVATE);
        showHome();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        database.close();
        super.onDestroy();
    }

    private void screen(String title, boolean back) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(8), dp(8), dp(4));
        if (back) {
            Button b = button("‹", v -> showHome());
            b.setTextSize(28);
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setTextColor(DARK);
            header.addView(b, new LinearLayout.LayoutParams(dp(58), dp(54)));
        }
        TextView h = text(title, 21, DARK, true);
        h.setGravity(Gravity.CENTER);
        header.addView(h, new LinearLayout.LayoutParams(0, dp(54), 1));
        if (back) header.addView(new View(this), new LinearLayout.LayoutParams(dp(58), dp(54)));
        root.addView(header);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(6), 0, dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(dp(16), dp(10), dp(16), dp(10));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setOnClickListener(listener);
        return b;
    }

    private TextView card(String title, String subtitle, int background, View.OnClickListener listener) {
        TextView v = text(title + "\n" + subtitle, 17, DARK, true);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setMinHeight(dp(86));
        v.setBackgroundColor(background);
        v.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(16), dp(7), dp(16), dp(7));
        v.setLayoutParams(p);
        return v;
    }

    private Button primary(String title, View.OnClickListener listener) {
        Button b = button(title, listener);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(GREEN);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(16), dp(7), dp(16), dp(7));
        b.setLayoutParams(p);
        return b;
    }

    private void showHome() {
        screen("NutriVox", false);
        TextView logo = text("💚 NutriVox", 31, GREEN, true);
        logo.setGravity(Gravity.CENTER);
        content.addView(logo);
        TextView sub = text("Assistant nutritionnel avec analyse d’assiette par IA", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        content.addView(sub);

        content.addView(card("📷  Analyser mon assiette", "Photo envoyée au serveur IA, puis confirmation des aliments",
                Color.rgb(232,247,240), v -> startPhoto()));
        content.addView(card("▣  Scanner un code-barres", "Recherche réelle dans Open Food Facts",
                Color.rgb(233,245,255), v -> scanBarcode()));
        content.addView(card("🎙  Recherche vocale", "Dites le nom d’un aliment ou produit",
                Color.rgb(244,249,246), v -> startVoice()));
        content.addView(card("⌨  Recherche manuelle", "Saisir le nom d’un produit",
                Color.rgb(244,249,246), v -> showSearch()));
        content.addView(card("🕘  Historique local", "Résultats conservés sur ce téléphone",
                Color.rgb(247,247,247), v -> showHistory()));
        content.addView(card("⚙  Configuration IA", "Indiquer l’adresse de votre Cloudflare Worker",
                Color.rgb(255,249,232), v -> showSettings()));

        TextView warning = text(
                "Attention : l’IA fournit une estimation visuelle. Confirmez toujours les aliments et les portions. " +
                "Les recommandations médicales doivent être validées avec votre équipe de dialyse.",
                14, MUTED, false);
        warning.setBackgroundColor(Color.rgb(232,247,240));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(dp(16), dp(14), dp(16), dp(8));
        warning.setLayoutParams(wp);
        content.addView(warning);
    }

    private void startPhoto() {
        String url = workerUrl();
        if (url.contains("VOTRE-WORKER")) {
            Toast.makeText(this, "Configurez d’abord l’adresse du serveur IA", Toast.LENGTH_LONG).show();
            showSettings();
            return;
        }
        try {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), PHOTO_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Appareil photo indisponible", Toast.LENGTH_LONG).show();
        }
    }

    private void analyzePhoto(Bitmap bitmap) {
        screen("Analyse IA en cours", true);
        progress.setVisibility(View.VISIBLE);
        content.addView(text("Analyse de la photo et détection des aliments…", 17, DARK, true));
        content.addView(text("La photo est envoyée au Cloudflare Worker puis à Gemini. Elle n’est pas enregistrée par l’application.", 14, MUTED, false));

        executor.execute(() -> {
            try {
                MealAnalysis result = AiClient.analyze(workerUrl(), bitmap);
                runOnUiThread(() -> showMeal(result));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    content.addView(text("Échec de l’analyse :\n" + e.getMessage(), 15, Color.RED, false));
                    content.addView(primary("Réessayer", v -> showHome()));
                });
            }
        });
    }

    private void showMeal(MealAnalysis result) {
        database.add(result.mealName, "Énergie " + round(result.totalEnergy()) + " kcal • K " +
                round(result.totalPotassium()) + " mg • Na " + round(result.totalSodium()) + " mg");
        screen("Résultat de l’assiette", true);
        content.addView(text(result.mealName, 25, DARK, true));
        content.addView(text("Aliments détectés — vérifiez et corrigez les portions si nécessaire.", 14, MUTED, false));

        for (FoodItem f : result.foods) {
            String details = "Portion estimée : " + round(f.portionGrams) + " g • confiance " + f.confidence +
                    "\n" + round(f.energyKcal) + " kcal • protéines " + one(f.proteins) + " g" +
                    "\nPotassium " + round(f.potassiumMg) + " mg • sodium " + round(f.sodiumMg) +
                    " mg • phosphore " + round(f.phosphorusMg) + " mg" +
                    (f.note.isEmpty() ? "" : "\n" + f.note);
            content.addView(card("🍽 " + f.name, details, Color.rgb(244,249,246), null));
        }

        content.addView(text("TOTAL ESTIMÉ", 17, DARK, true));
        nutrient("Énergie", result.totalEnergy(), "kcal");
        nutrient("Protéines", result.totalProtein(), "g");
        nutrient("Potassium", result.totalPotassium(), "mg");
        nutrient("Sodium", result.totalSodium(), "mg");
        nutrient("Phosphore", result.totalPhosphorus(), "mg");

        if (!result.warning.isEmpty()) {
            TextView warning = text("⚠ " + result.warning, 15, DARK, true);
            warning.setBackgroundColor(Color.rgb(255,249,232));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.setMargins(dp(16), dp(12), dp(16), dp(8));
            warning.setLayoutParams(p);
            content.addView(warning);
        }
        content.addView(primary("Nouvelle analyse", v -> showHome()));
    }

    private void scanBarcode() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner.startScan()
                .addOnSuccessListener(b -> {
                    String code = b.getRawValue();
                    if (code != null) searchBarcode(code);
                })
                .addOnCanceledListener(() -> Toast.makeText(this, "Scan annulé", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void searchBarcode(String code) {
        loading("Recherche du produit…");
        executor.execute(() -> {
            try {
                NutritionProduct p = OpenFoodFactsClient.findByBarcode(code);
                runOnUiThread(() -> {
                    if (p == null) notFound(code); else showProduct(p);
                });
            } catch (Exception e) {
                runOnUiThread(() -> error(e));
            }
        });
    }

    private void showSearch() {
        screen("Recherche d’un produit", true);
        EditText input = new EditText(this);
        input.setHint("Ex. soupe, yaourt, céréales…");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(16), dp(12), dp(16), dp(8));
        input.setLayoutParams(p);
        content.addView(input);
        content.addView(primary("Rechercher", v -> {
            String q = input.getText().toString().trim();
            if (q.length() >= 2) searchName(q);
        }));
    }

    private void searchName(String query) {
        loading("Recherche de « " + query + " »…");
        executor.execute(() -> {
            try {
                NutritionProduct p = OpenFoodFactsClient.searchFirst(query);
                runOnUiThread(() -> {
                    if (p == null) notFound(query); else showProduct(p);
                });
            } catch (Exception e) {
                runOnUiThread(() -> error(e));
            }
        });
    }

    private void showProduct(NutritionProduct p) {
        database.add(p.name, "Énergie " + round(p.energyKcal) + " kcal • K " +
                round(p.potassiumMg) + " mg • Na " + round(p.sodiumMg) + " mg");
        screen("Produit alimentaire", true);
        content.addView(text(p.name, 25, DARK, true));
        if (!p.brands.isEmpty()) content.addView(text(p.brands, 15, MUTED, false));
        content.addView(text("Valeurs pour 100 g ou 100 ml, selon la fiche Open Food Facts.", 14, MUTED, false));
        nutrient("Énergie", p.energyKcal, "kcal");
        nutrient("Protéines", p.proteins, "g");
        nutrient("Glucides", p.carbohydrates, "g");
        nutrient("Lipides", p.fat, "g");
        nutrient("Sucres", p.sugars, "g");
        nutrient("Fibres", p.fiber, "g");
        nutrient("Potassium", p.potassiumMg, "mg");
        nutrient("Sodium", p.sodiumMg, "mg");
        nutrient("Phosphore", p.phosphorusMg, "mg");
        content.addView(card("Ingrédients", p.ingredients, Color.rgb(244,249,246), null));
        content.addView(primary("Retour à l’accueil", v -> showHome()));
    }

    private void nutrient(String name, double value, String unit) {
        content.addView(text(name + "                         " +
                (value <= 0 ? "non renseigné" : one(value) + " " + unit), 15, DARK, false));
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites le nom d’un aliment");
        try {
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Reconnaissance vocale indisponible", Toast.LENGTH_LONG).show();
        }
    }

    private void showSettings() {
        screen("Configuration IA", true);
        content.addView(text("Adresse du Cloudflare Worker", 16, DARK, true));
        EditText input = new EditText(this);
        input.setText(workerUrl());
        input.setHint("https://mon-worker.workers.dev");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(16), dp(8), dp(16), dp(8));
        input.setLayoutParams(p);
        content.addView(input);
        content.addView(text(
                "La clé Gemini reste uniquement dans le secret Cloudflare GEMINI_API_KEY. Ne la placez jamais dans l’application.",
                14, MUTED, false));
        content.addView(primary("Enregistrer", v -> {
            String url = input.getText().toString().trim();
            if (!url.startsWith("https://")) {
                Toast.makeText(this, "L’adresse doit commencer par https://", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putString("worker_url", url).apply();
            Toast.makeText(this, "Adresse enregistrée", Toast.LENGTH_SHORT).show();
            showHome();
        }));
    }

    private String workerUrl() {
        return prefs.getString("worker_url", BuildConfig.DEFAULT_WORKER_URL);
    }

    private void showHistory() {
        screen("Historique local", true);
        List<String> rows = database.history();
        if (rows.isEmpty()) content.addView(text("Aucun résultat enregistré.", 17, MUTED, false));
        else for (String row : rows) content.addView(card("🕘 " + row, "Stocké uniquement sur ce téléphone",
                Color.rgb(247,247,247), null));
    }

    private void loading(String message) {
        screen("Chargement", true);
        progress.setVisibility(View.VISIBLE);
        content.addView(text(message, 17, DARK, true));
    }

    private void notFound(String query) {
        screen("Introuvable", true);
        content.addView(text("Aucun produit trouvé pour : " + query, 18, DARK, true));
        content.addView(primary("Nouvelle recherche", v -> showSearch()));
    }

    private void error(Exception e) {
        screen("Erreur", true);
        content.addView(text(e.getMessage(), 15, Color.RED, false));
        content.addView(primary("Retour", v -> showHome()));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> values = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (values != null && !values.isEmpty()) searchName(values.get(0));
        } else if (requestCode == PHOTO_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Object extra = data.getExtras() == null ? null : data.getExtras().get("data");
            if (extra instanceof Bitmap) analyzePhoto((Bitmap) extra);
            else Toast.makeText(this, "Photo non récupérée", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private int round(double value) { return (int)Math.round(value); }
    private String one(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.01) return String.valueOf((int)Math.round(value));
        return String.format(Locale.FRANCE, "%.1f", value);
    }
}
