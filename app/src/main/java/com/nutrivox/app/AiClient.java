package com.nutrivox.app;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiClient {
    public static MealAnalysis analyze(String workerUrl, Bitmap bitmap) throws Exception {
        Bitmap resized = resize(bitmap, 1280);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 78, bytes);

        JSONObject request = new JSONObject();
        request.put("mimeType", "image/jpeg");
        request.put("imageBase64", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP));

        String endpoint = workerUrl.replaceAll("/+$", "") + "/analyze";
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");

        try (OutputStream out = c.getOutputStream()) {
            out.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = c.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);
        reader.close();
        c.disconnect();

        if (status < 200 || status >= 300) throw new Exception("Serveur IA : " + body);
        return parse(new JSONObject(body.toString()));
    }

    private static MealAnalysis parse(JSONObject json) {
        MealAnalysis result = new MealAnalysis();
        result.mealName = json.optString("mealName", "Assiette analysée");
        result.warning = json.optString("warning", "");
        JSONArray foods = json.optJSONArray("foods");
        if (foods != null) {
            for (int i = 0; i < foods.length(); i++) {
                JSONObject o = foods.optJSONObject(i);
                if (o == null) continue;
                FoodItem f = new FoodItem();
                f.name = o.optString("name", "Aliment");
                f.confidence = o.optString("confidence", "incertaine");
                f.portionGrams = o.optDouble("portionGrams", 0);
                f.energyKcal = o.optDouble("energyKcal", 0);
                f.proteins = o.optDouble("proteins", 0);
                f.carbohydrates = o.optDouble("carbohydrates", 0);
                f.fat = o.optDouble("fat", 0);
                f.sodiumMg = o.optDouble("sodiumMg", 0);
                f.potassiumMg = o.optDouble("potassiumMg", 0);
                f.phosphorusMg = o.optDouble("phosphorusMg", 0);
                f.note = o.optString("note", "");
                result.foods.add(f);
            }
        }
        return result;
    }

    private static Bitmap resize(Bitmap source, int maxSide) {
        int w = source.getWidth();
        int h = source.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSide) return source;
        float scale = (float) maxSide / max;
        return Bitmap.createScaledBitmap(source, Math.round(w * scale), Math.round(h * scale), true);
    }
}
