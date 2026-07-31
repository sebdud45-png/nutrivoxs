const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "content-type",
  "access-control-allow-methods": "POST, OPTIONS"
};

function response(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

function cleanJson(text) {
  const trimmed = String(text || "").trim();
  const withoutFence = trimmed
    .replace(/^```json\s*/i, "")
    .replace(/^```\s*/i, "")
    .replace(/\s*```$/i, "");
  const start = withoutFence.indexOf("{");
  const end = withoutFence.lastIndexOf("}");
  if (start < 0 || end < start) throw new Error("Réponse IA sans objet JSON");
  return JSON.parse(withoutFence.slice(start, end + 1));
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: JSON_HEADERS });
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return response({ ok: true, service: "NutriVox AI" });
    }

    if (url.pathname !== "/analyze" || request.method !== "POST") {
      return response({ error: "Route introuvable" }, 404);
    }

    if (!env.GEMINI_API_KEY) {
      return response({ error: "Secret GEMINI_API_KEY manquant" }, 500);
    }

    try {
      const body = await request.json();
      const imageBase64 = String(body.imageBase64 || "");
      const mimeType = String(body.mimeType || "image/jpeg");

      if (!imageBase64 || imageBase64.length < 100) {
        return response({ error: "Image manquante" }, 400);
      }
      if (imageBase64.length > 8_000_000) {
        return response({ error: "Image trop volumineuse" }, 413);
      }

      const prompt = `
Tu analyses une photo d'assiette pour une application française d'information nutritionnelle.
Identifie seulement les aliments réellement visibles. Ne prétends jamais connaître un ingrédient caché.
Estime prudemment les portions. Les nutriments sont des estimations, pas des mesures.
Réponds UNIQUEMENT avec un JSON valide, sans markdown, de cette forme exacte :
{
  "mealName": "nom court du plat",
  "warning": "limitation ou point important",
  "foods": [
    {
      "name": "aliment",
      "confidence": "élevée|moyenne|faible",
      "portionGrams": 0,
      "energyKcal": 0,
      "proteins": 0,
      "carbohydrates": 0,
      "fat": 0,
      "sodiumMg": 0,
      "potassiumMg": 0,
      "phosphorusMg": 0,
      "note": "courte précision"
    }
  ]
}
Les valeurs nutritionnelles correspondent à la portion estimée de chaque aliment.
S'il est impossible d'identifier un élément, indique-le avec une confiance faible au lieu de l'inventer.
Ne donne aucun diagnostic ni prescription médicale.
`;

      const geminiBody = {
        contents: [{
          role: "user",
          parts: [
            { text: prompt },
            { inline_data: { mime_type: mimeType, data: imageBase64 } }
          ]
        }],
        generationConfig: {
          temperature: 0.15,
          responseMimeType: "application/json"
        }
      };

      const endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" +
        encodeURIComponent(env.GEMINI_API_KEY);

      const gemini = await fetch(endpoint, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(geminiBody)
      });

      const raw = await gemini.json();
      if (!gemini.ok) {
        return response({ error: "Gemini a refusé la requête", details: raw }, 502);
      }

      const text = raw?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!text) return response({ error: "Réponse Gemini vide" }, 502);

      const parsed = cleanJson(text);
      return response(parsed);
    } catch (error) {
      return response({ error: String(error?.message || error) }, 500);
    }
  }
};
