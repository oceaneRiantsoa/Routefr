# 🚨 Application de Signalement - Antananarivo

Application mobile Ionic + Vue.js pour signaler les problèmes d'infrastructure routière à Antananarivo.

## 🚀 Installation

### Prérequis
- Node.js 18+
- npm ou yarn
- Compte Firebase

### Installation des dépendances

```bash
npm install
```

### Configuration Firebase

1. **Créer un fichier `.env.local`** à la racine du projet :

```env
VITE_FIREBASE_API_KEY=votre_api_key
VITE_FIREBASE_AUTH_DOMAIN=votre_projet.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=votre_projet_id
VITE_FIREBASE_STORAGE_BUCKET=votre_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=votre_sender_id
VITE_FIREBASE_APP_ID=votre_app_id
VITE_FIREBASE_MEASUREMENT_ID=votre_measurement_id
VITE_FIREBASE_DATABASE_URL=https://votre_projet.firebaseio.com
```

Vous pouvez copier `.env.example` et le renommer en `.env.local`, puis remplir avec vos vraies clés Firebase.

2. **Configurer Firebase Console** :
   - Activer **Authentication** (Email/Password)
   - Activer **Realtime Database**
   - Configurer les règles de sécurité (voir [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md))
   - Importer les données de test si nécessaire

3. **Règles Firebase recommandées** :

```json
{
  "rules": {
    "signalements": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$signalementId": {
        ".write": "auth.uid == data.child('userId').val() || auth.uid == newData.child('userId').val()"
      }
    },
    "types_problemes": {
      ".read": "auth != null",
      ".write": false
    },
    "entreprises": {
      ".read": "auth != null",
      ".write": false
    }
  }
}
```

## 📱 Lancement

### Mode développement
```bash
npm run dev
```

### Build pour production
```bash
npm run build
```

### Synchronisation Capacitor
```bash
npx cap sync
```

### Ouvrir sur mobile
```bash
# Android
npx cap open android

# iOS
npx cap open ios
```

## 🛠️ Technologies

- **Frontend** : Ionic 8 + Vue 3 + TypeScript
- **Backend** : Firebase (Authentication + Realtime Database)
- **Carte** : Leaflet + OpenStreetMap
- **Mobile** : Capacitor
- **Camera** : Capacitor Camera API

## 📁 Structure du projet

```
photo-gallery/
├── src/
│   ├── views/              # Pages de l'application
│   │   ├── LoginPage.vue
│   │   ├── SignupPage.vue
│   │   ├── MapSignalementPage.vue
│   │   └── ...
│   ├── services/           # Services métier
│   │   └── signalementService.ts
│   ├── composables/        # Composables Vue
│   │   └── usePhotoGallery.ts
│   ├── types/              # Types TypeScript
│   │   └── signalement.ts
│   ├── router/             # Configuration du routeur
│   └── firebase.ts         # Configuration Firebase
├── android/                # Projet Android Capacitor
├── ios/                    # Projet iOS Capacitor
├── public/                 # Assets statiques
└── tests/                  # Tests E2E et unitaires
```

## 🔐 Sécurité

⚠️ **Ne JAMAIS commiter** :
- Le fichier `.env.local` (credentials Firebase)
- Les fichiers `*.keystore` ou `*.jks` (signature Android)
- Le fichier `android/keystore.properties`
- Le fichier `firebase-data.json` (si contient des données sensibles)

Ces fichiers sont déjà dans `.gitignore`.

## 🧪 Tests

```bash
# Tests unitaires
npm run test:unit

# Tests E2E avec Cypress
npm run test:e2e
```

## 📖 Documentation

- [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) - Configuration complète Firebase
- [`WebFinal-todo.md`](WebFinal-todo.md) - Roadmap du projet
- [`WebFinal-conception.sql`](WebFinal-conception.sql) - Schéma de base de données PostgreSQL (si migration prévue)

## 🌍 Fonctionnalités

- ✅ Authentification utilisateur (inscription/connexion)
- ✅ Création de signalements avec photo
- ✅ Géolocalisation automatique
- ✅ Carte interactive des signalements
- ✅ Filtrage par type de problème
- ✅ Consultation des détails de signalement
- ✅ Support mobile (Android/iOS)

## 🤝 Contribution

Pour contribuer au projet :

1. Cloner le repository
2. Créer une branche feature : `git checkout -b feature/ma-fonctionnalite`
3. Commiter les changements : `git commit -m "Ajout de ma fonctionnalité"`
4. Pusher la branche : `git push origin feature/ma-fonctionnalite`
5. Créer une Pull Request

## 👥 Auteurs

- Votre équipe de développement

## 📄 Licence

Ce projet est développé dans le cadre d'un projet académique.
