# Tutoriel : Générer un APK Android avec Capacitor et Gradle

Ce guide explique comment builder une APK Android pour ce projet Ionic/Vue/Capacitor sans Android Studio, uniquement avec le SDK Android et Gradle.

---

## Prérequis

- Node.js (v16+ recommandé)
- npm ou yarn
- Java JDK (11 ou 17 recommandé)
- SDK Android installé et configuré (`ANDROID_HOME` dans le PATH)

---

## Étapes détaillées

### 1. Installer les dépendances Node.js

Dans le dossier `mobile/` :

```sh
npm install
```

### 2. Construire l’application web

Toujours dans `mobile/` :

```sh
npm run build
```

### 3. Ajouter la plateforme Android (si ce n’est pas déjà fait)

```sh
npx cap add android
```

### 4. Synchroniser le projet avec Capacitor

```sh
npx cap sync android
```

Cette commande :
- Copie les fichiers web compilés dans le projet Android
- Met à jour les plugins natifs et la config

### 5. Builder l’APK avec Gradle

Va dans le dossier Android :

```sh
cd android
```

Puis, sous Windows :

```sh
gradlew assembleRelease
```

Ou sous Linux/Mac :

```sh
./gradlew assembleRelease
```

L’APK généré se trouve dans :

```
mobile/android/app/build/outputs/apk/release/app-release.apk
```

Pour une version debug :

```sh
gradlew assembleDebug
```

APK : `mobile/android/app/build/outputs/apk/debug/app-debug.apk`

---

## Notes

- Si `gradlew` n’existe pas, exécute d’abord `npx cap add android`.
- Si tu modifies le code web, refais `npm run build` puis `npx cap sync android` avant de re-builder l’APK.
- Pour signer l’APK en production, configure la signature dans `android/app/build.gradle`.

---

## Liens utiles
- [Documentation Capacitor Android](https://capacitorjs.com/docs/guides/android)
- [Documentation officielle Gradle](https://docs.gradle.org/current/userguide/userguide.html)
