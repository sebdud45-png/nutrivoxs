package com.nutrivox.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenFoodFactsClient {
    private static final String USER_AGENT = "NutriVox-Android/1.0";

    public static NutritionProduct findByBarcode(String code) throws Exception {
        String endpoint = "https://world.openfoodfacts.org/api/v2/product/" +
                URLEncoder.encode(code, "UTF-8") +
                ".json?fields=code,product_name,brands,ingredients_text_fr,ingredients_text,nutriments";
        JSONObject json = request(endpoint);
        if (json.optInt("status", 0) != 1) return null;
        return parse(json.getJSONObject("product"));
    }

    public static NutritionProduct searchFirst(String query) throws Exception {
        String endpoint = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" +
                URLEncoder.encode(query, "UTF-8") +
                "&search_simple=1&action=process&json=1&page_size=1&fields=code,product_name,brands,ingredients_text_fr,ingredients_text,nutriments";
        JSONObject json = request(endpoint);
        JSONArray products = json.optJSONArray("products");
        if (products == null || products.length() == 0) return null;
        return parse(products.getJSONObject(0));
    }

    private static JSONObject request(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", USER_AGENT);
        c.setRequestProperty("Accept", "application/json");
        int status = c.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);
        reader.close();
        c.disconnect();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
        return new JSONObject(body.toString());
    }

    private static NutritionProduct parse(JSONObject product) {
        NutritionProduct p = new NutritionProduct();
        p.code = product.optString("code", "");
        p.name = product.optString("product_name", "Produit sans nom").trim();
        if (p.name.isEmpty()) p.name = "Produit sans nom";
        p.brands = product.optString("brands", "");
        p.ingredients = product.optString("ingredients_text_fr",
                product.optString("ingredients_text", "Ingrédients non renseignés"));
        JSONObject n = product.optJSONObject("nutriments");
        if (n != null) {
            p.energyKcal = value(n, "energy-kcal_100g");
            p.proteins = value(n, "proteins_100g");
            p.carbohydrates = value(n, "carbohydrates_100g");
            p.fat = value(n, "fat_100g");
            p.fiber = value(n, "fiber_100g");
            p.sugars = value(n, "sugars_100g");
            p.saltG = value(n, "salt_100g");
            p.sodiumMg = value(n, "sodium_100g") * 1000.0;
            p.potassiumMg = value(n, "potassium_100g") * 1000.0;
            p.phosphorusMg = value(n, "phosphorus_100g") * 1000.0;
        }
        return p;
    }

    private static double value(JSONObject n, String key) {
        return n.has(key) ? n.optDouble(key, 0) : 0;
    }
}
