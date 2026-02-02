# 🔥 Configuration Firebase pour le Push Mobile

## ⚠️ Important : Configurer les règles de sécurité Firebase

Pour que l'envoi des données vers Firebase fonctionne, vous devez configurer les règles de sécurité dans votre console Firebase.

### Étape 1 : Accéder à la console Firebase

1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionnez votre projet : `test-8f6f5`
3. Dans le menu à gauche, cliquez sur **"Realtime Database"**
4. Cliquez sur l'onglet **"Règles"**

### Étape 2 : Modifier les règles

Remplacez les règles existantes par celles-ci :

```json
{
  "rules": {
    "signalements": {
      ".read": true,
      ".write": true
    },
    "signalements_mobile": {
      ".read": true,
      ".write": true
    },
    "_metadata": {
      ".read": true,
      ".write": true
    }
  }
}
```

### ⚠️ Note sur la sécurité

Ces règles sont permissives pour le développement. En production, vous devriez restreindre l'écriture :

```json
{
  "rules": {
    "signalements": {
      ".read": true,
      ".write": "auth != null"
    },
    "signalements_mobile": {
      ".read": true,
      ".write": "auth != null && auth.token.admin === true"
    }
  }
}
```

### Étape 3 : Publier les règles

Cliquez sur **"Publier"** pour appliquer les nouvelles règles.

---

## 🧪 Tester le Push

### 1. Aperçu des données à envoyer

```bash
curl -s http://localhost:8086/api/manager/sync/push/preview | jq 'length'
# Devrait retourner le nombre de signalements à envoyer
```

### 2. Envoyer tous les signalements

```bash
curl -s -X POST http://localhost:8086/api/manager/sync/push | jq
```

### 3. Envoyer un seul signalement

```bash
# Par ID local
curl -s -X POST http://localhost:8086/api/manager/sync/push/1 | jq

# Par ID Firebase
curl -s -X POST http://localhost:8086/api/manager/sync/push/fb_test_001 | jq
```

---

## 📱 Structure des données dans Firebase

Après le push, les données seront stockées dans `signalements_mobile` avec cette structure :

```json
{
  "signalements_mobile": {
    "local_1": {
      "localId": 1,
      "latitude": -18.8792,
      "longitude": 47.5079,
      "problemeNom": "Nid de poule",
      "description": "Description du problème",
      "status": "nouveau",
      "statutLibelle": "En attente",
      "surface": 15.5,
      "budget": 445625,
      "entrepriseNom": "Entreprise XYZ",
      "notesManager": "Notes du manager",
      "dateCreation": 1738123456789,
      "datePush": 1738234567890,
      "source": "local",
      "couleur": "#FFC107",
      "icone": "pothole"
    },
    "fb_test_001": {
      "id": "fb_test_001",
      "localId": 1,
      "latitude": -18.8792,
      "longitude": 47.5079,
      "problemeNom": "Nid de poule",
      "status": "nouveau",
      "userEmail": "user@example.com",
      "source": "firebase",
      "couleur": "#FFC107",
      "icone": "pothole"
    },
    "_metadata": {
      "lastPush": 1738234567890,
      "totalSignalements": 5,
      "source": "manager-web"
    }
  }
}
```

---

## 📲 Lecture sur l'application mobile

L'application mobile Flutter/React Native peut lire ces données avec :

### Flutter (Dart)
```dart
final ref = FirebaseDatabase.instance.ref('signalements_mobile');
final snapshot = await ref.get();
if (snapshot.exists) {
  final data = snapshot.value as Map<dynamic, dynamic>;
  data.forEach((key, value) {
    if (key != '_metadata') {
      print('Signalement: $key');
      print('  - Problème: ${value['problemeNom']}');
      print('  - Position: ${value['latitude']}, ${value['longitude']}');
      print('  - Statut: ${value['statutLibelle']}');
    }
  });
}
```

### React Native (JavaScript)
```javascript
import database from '@react-native-firebase/database';

const ref = database().ref('/signalements_mobile');
ref.on('value', snapshot => {
  const data = snapshot.val();
  Object.keys(data).forEach(key => {
    if (key !== '_metadata') {
      console.log('Signalement:', key);
      console.log('  - Problème:', data[key].problemeNom);
      console.log('  - Position:', data[key].latitude, data[key].longitude);
    }
  });
});
```

---

## 🔄 API Endpoints

### Pull (Firebase → PostgreSQL)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/manager/sync/pull` | Synchroniser depuis Firebase |
| GET | `/api/manager/sync/preview` | Aperçu des données Firebase |
| GET | `/api/manager/sync/stats` | Statistiques de synchronisation |

### Push (PostgreSQL → Firebase)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/manager/sync/push` | Envoyer tous les signalements vers Firebase |
| POST | `/api/manager/sync/push/{id}` | Envoyer un signalement spécifique |
| GET | `/api/manager/sync/push/preview` | Aperçu des données à envoyer |
| GET | `/api/manager/sync/stats/full` | Statistiques complètes (pull + push) |

---

## 🎨 Couleurs et icônes par statut

### Couleurs
| Statut | Code couleur | Signification |
|--------|--------------|---------------|
| En attente | `#FFC107` | Jaune |
| En cours | `#2196F3` | Bleu |
| Traité | `#4CAF50` | Vert |
| Rejeté | `#F44336` | Rouge |

### Icônes
| Type de problème | Icône |
|------------------|-------|
| Nid de poule / Trou | `pothole` |
| Fissure | `crack` |
| Affaissement | `collapse` |
| Inondation | `flood` |
| Débris / Obstacle | `debris` |
| Autre | `warning` |

Ces informations permettent à l'application mobile d'afficher les signalements avec les bonnes couleurs et icônes sur la carte.
