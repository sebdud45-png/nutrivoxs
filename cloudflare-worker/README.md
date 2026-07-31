# Serveur IA NutriVox — Cloudflare Worker

## Installation gratuite

1. Créez un compte Cloudflare.
2. Ouvrez un terminal dans ce dossier.
3. Exécutez :
   npm install
   npx wrangler login
4. Créez une clé Gemini dans Google AI Studio.
5. Enregistrez-la comme secret :
   npx wrangler secret put GEMINI_API_KEY
6. Déployez :
   npm run deploy
7. Cloudflare affiche une adresse ressemblant à :
   https://nutrivox-ai.VOTRE-SOUS-DOMAINE.workers.dev
8. Dans l'application NutriVox, ouvrez **Configuration IA** et collez cette adresse.

La clé Gemini n'est jamais incluse dans l'APK.
