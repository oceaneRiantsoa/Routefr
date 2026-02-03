# 🗄️ Structure Firebase Realtime Database pour Signalements

## 1. Activer Realtime Database dans Firebase Console

1. Aller sur https://console.firebase.google.com
2. Sélectionner votre projet **test-8f6f5**
3. Aller dans **Build > Realtime Database**
4. Cliquer sur **Create Database**
5. Choisir la région (eur3 pour Europe)
6. Démarrer en **mode test** pour le développement

## 2. URL de la Database

Votre URL sera quelque chose comme :
```
https://test-8f6f5-default-rtdb.firebaseio.com
```

⚠️ Mettez à jour cette URL dans `src/firebase.ts` si différente !

## 3. Structure JSON de la Base

```json
{
  "signalements": {
    "-NxYz123abc": {
      "id": "-NxYz123abc",
      "userId": "uid_firebase_user",
      "userEmail": "user@example.com",
      "latitude": -18.8792,
      "longitude": 47.5079,
      "problemeId": "nid_poule",
      "problemeNom": "Nid de poule",
      "description": "Détails supplémentaires...",
      "status": "nouveau",
      "dateCreation": 1737331200000
    }
  },
  "types_problemes": {
    "nid_poule": {
      "id": "nid_poule",
      "nom": "Nid de poule",
      "icone": "🕳️",
      "description": "Trou dans la chaussée",
      "priorite": 1,
      "actif": true
    },
    "fissure": {
      "id": "fissure",
      "nom": "Fissure",
      "icone": "⚡",
      "description": "Fissure ou craquelure",
      "priorite": 2,
      "actif": true
    },
    "affaissement": {
      "id": "affaissement",
      "nom": "Affaissement",
      "icone": "📉",
      "description": "Affaissement de la route",
      "priorite": 1,
      "actif": true
    },
    "inondation": {
      "id": "inondation",
      "nom": "Inondation",
      "icone": "🌊",
      "description": "Zone inondée",
      "priorite": 1,
      "actif": true
    },
    "eclairage": {
      "id": "eclairage",
      "nom": "Éclairage défaillant",
      "icone": "💡",
      "description": "Lampadaire cassé",
      "priorite": 3,
      "actif": true
    },
    "signalisation": {
      "id": "signalisation",
      "nom": "Signalisation manquante",
      "icone": "🚧",
      "description": "Panneau manquant",
      "priorite": 2,
      "actif": true
    },
    "trottoir": {
      "id": "trottoir",
      "nom": "Trottoir endommagé",
      "icone": "🚶",
      "description": "Trottoir cassé",
      "priorite": 2,
      "actif": true
    },
    "egout": {
      "id": "egout",
      "nom": "Égout bouché",
      "icone": "🚰",
      "description": "Canalisation bouchée",
      "priorite": 1,
      "actif": true
    },
    "debris": {
      "id": "debris",
      "nom": "Débris sur route",
      "icone": "🪨",
      "description": "Obstacles dangereux",
      "priorite": 1,
      "actif": true
    },
    "autre": {
      "id": "autre",
      "nom": "Autre problème",
      "icone": "❓",
      "description": "Autre type de problème",
      "priorite": 3,
      "actif": true
    }
  },
  "entreprises": {
    "colas": {
      "id": "colas",
      "nom": "COLAS Madagascar",
      "contact": "Direction Tana",
      "telephone": "+261 20 22 XXX",
      "specialite": "Routes et voiries",
      "actif": true
    },
    "sogea": {
      "id": "sogea",
      "nom": "SOGEA SATOM",
      "contact": "Bureau Antananarivo",
      "telephone": "+261 20 22 XXX",
      "specialite": "Travaux publics",
      "actif": true
    },
    "eiffage": {
      "id": "eiffage",
      "nom": "EIFFAGE",
      "contact": "Siège Madagascar",
      "telephone": "+261 20 22 XXX",
      "specialite": "Construction",
      "actif": true
    },
    "ravinala": {
      "id": "ravinala",
      "nom": "RAVINALA Roads",
      "contact": "Direction technique",
      "telephone": "+261 20 22 XXX",
      "specialite": "Entretien routier",
      "actif": true
    },
    "agetipa": {
      "id": "agetipa",
      "nom": "AGETIPA",
      "contact": "Agence nationale",
      "telephone": "+261 20 22 XXX",
      "specialite": "Infrastructure publique",
      "actif": true
    },
    "jirama": {
      "id": "jirama",
      "nom": "JIRAMA",
      "contact": "Service technique",
      "telephone": "+261 20 22 XXX",
      "specialite": "Éclairage et réseaux",
      "actif": true
    }
  }
}
```

## 4. Règles de Sécurité (Firebase Rules)

Coller ces règles dans **Realtime Database > Rules** :

```json
{
  "rules": {
    "signalements": {
      ".read": "auth != null",
      ".write": "auth != null",
      ".indexOn": ["userId", "status", "dateCreation"],
      "$signalement_id": {
        ".validate": "newData.hasChildren(['userId', 'userEmail', 'latitude', 'longitude', 'description', 'status', 'surface', 'dateCreation'])",
        "userId": {
          ".validate": "newData.val() === auth.uid"
        },
        "status": {
          ".validate": "newData.val() === 'nouveau' || newData.val() === 'en_cours' || newData.val() === 'termine'"
        },
        "surface": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        },
        "budget": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        }
      }
    },
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

## 5. Données de Test à Importer

Vous pouvez importer ces données via **Firebase Console > Realtime Database > Import JSON** :

```json
{
  "signalements": {
    "test1": {
      "id": "test1",
      "userId": "test_user_id",
      "userEmail": "test@test.com",
      "latitude": -18.8792,
      "longitude": 47.5079,
      "description": "Nid de poule important devant la mairie d'Analakely",
      "status": "nouveau",
      "surface": 15,
      "budget": 300000,
      "entreprise": "Non assignée",
      "dateCreation": 1737331200000
    },
    "test2": {
      "id": "test2",
      "userId": "test_user_id",
      "userEmail": "test@test.com",
      "latitude": -18.8850,
      "longitude": 47.5150,
      "description": "Affaissement de chaussée près du Lac Anosy",
      "status": "en_cours",
      "surface": 50,
      "budget": 1500000,
      "entreprise": "COLAS Madagascar",
      "dateCreation": 1737244800000,
      "dateModification": 1737331200000
    },
    "test3": {
      "id": "test3",
      "userId": "test_user_id",
      "userEmail": "test@test.com",
      "latitude": -18.8700,
      "longitude": 47.5200,
      "description": "Travaux terminés route d'Ivato",
      "status": "termine",
      "surface": 200,
      "budget": 5000000,
      "entreprise": "SOGEA SATOM",
      "dateCreation": 1737158400000,
      "dateModification": 1737331200000
    }
  }
}
```

## 6. Statuts des Signalements

| Status | Couleur | Description |
|--------|---------|-------------|
| `nouveau` | 🔴 Rouge | Signalement créé, non traité |
| `en_cours` | 🟡 Orange | Travaux en cours |
| `termine` | 🟢 Vert | Problème résolu |

## 7. Entreprises Prédéfinies

- COLAS Madagascar
- SOGEA SATOM
- EIFFAGE
- RAVINALA
- JIRAMA
- AGETIPA
- Non assignée

## 8. Architecture Hybride

```
┌─────────────────────────────────────┐
│  Mobile Ionic (Vue 3 + Capacitor)   │
│  ├─ LoginPage.vue (Firebase Auth)   │
│  ├─ MapSignalementPage.vue (Leaflet)│
│  └─ Services/signalementService.ts  │
└──────────────┬──────────────────────┘
               │
        ┌──────▼─────────────────────┐
        │  Firebase (Cloud)           │
        │  ├─ Authentication          │
        │  └─ Realtime Database       │
        │      └─ /signalements/...   │
        └─────────────────────────────┘
               │
        ┌──────▼─────────────────────┐
        │  Spring Boot (optionnel)    │
        │  ├─ Sync PostgreSQL         │
        │  └─ Admin / Statistiques    │
        └─────────────────────────────┘
```

## 9. Commandes Utiles

```bash
# Lancer le dev server
npm run dev

# Build pour production
npm run build

# Sync avec Capacitor (Android/iOS)
npx cap sync

# Ouvrir dans Android Studio
npx cap open android
```
