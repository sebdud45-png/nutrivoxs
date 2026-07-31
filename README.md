# NutriVox Android

Projet Android Gradle prêt à être compilé par GitHub Actions.

## Obtenir l'APK sur GitHub

1. Décompressez cette archive.
2. Téléversez **le contenu** de ce dossier à la racine du dépôt GitHub. Le dossier `app` et les fichiers `settings.gradle` et `build.gradle` doivent être visibles dès l'onglet **Code**.
3. Ouvrez **Actions** → **Construire l'APK NutriVox**.
4. Appuyez sur **Run workflow**.
5. À la fin du travail, téléchargez l'artefact **NutriVox-APK-debug**.
6. Décompressez l'artefact pour obtenir `app-debug.apk`.

Le workflow installe lui-même Java 17, le SDK Android 35 et Gradle 8.9. Il n'utilise donc pas la commande `./gradlew` et évite l'erreur « gradlew: No such file or directory ».

## Configuration de l'analyse IA

L'application doit utiliser l'adresse HTTPS d'un Cloudflare Worker. Dans l'application, ouvrez **Configuration IA** et saisissez l'adresse du Worker.

Ne placez jamais une clé Gemini directement dans l'application Android. Conservez-la dans le secret Cloudflare `GEMINI_API_KEY`.

Le dossier `cloudflare-worker` contient le code du Worker et ses instructions.
