# 🔥 Guide de Test Postman - API Authentification Firebase

## 📋 Table des Matières
1. [Configuration Initiale](#configuration-initiale)
2. [Tests des Endpoints](#tests-des-endpoints)
3. [Scénarios de Test](#scénarios-de-test)

---

## 🛠️ Configuration Initiale

### 1. **Démarrer PostgreSQL avec Docker**
```bash
# Dans le dossier du projet
docker-compose up -d

# Vérifier que la DB est active
docker ps
```

### 2. **Lancer l'Application Spring Boot**
```bash
mvn clean install
mvn spring-boot:run
```

### 3. **Vérifier Swagger UI**
Ouvrir dans le navigateur:
```
http://localhost:8080/swagger-ui.html
```

---

## 🧪 Tests des Endpoints

### **BASE URL**
```
http://localhost:8080/api/auth
```

---

## 1️⃣ **INSCRIPTION (Register)**

### **Endpoint**: `POST /api/auth/register`

### **Headers**:
```json
Content-Type: application/json
```

### **Body**:
```json
{
  "email": "user1@example.com",
  "password": "Test123456!",
  "displayName": "Jean Dupont"
}
```

### **Réponse Attendue** (201 Created):
```json
{
  "success": true,
  "message": "Inscription réussie",
  "user": {
    "uid": "firebase-uid-123456",
    "email": "user1@example.com",
    "displayName": "Jean Dupont",
    "emailVerified": false,
    "accountLocked": false,
    "failedAttempts": 0,
    "role": "USER",
    "createdAt": "2026-01-20T10:30:00"
  },
  "sessionToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### **À Sauvegarder**:
- ✅ `sessionToken` → Pour les appels suivants
- ✅ `uid` → Pour modifier/désactiver le compte

---

## 2️⃣ **CONNEXION (Login)**

### **Endpoint**: `POST /api/auth/login`

### **Headers**:
```json
Content-Type: application/json
```

### **Body**:
```json
{
  "email": "user1@example.com",
  "password": "Test123456!"
}
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Connexion réussie",
  "customToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "sessionToken": "b2c3d4e5-f6g7-8901-bcde-fg2345678901",
  "uid": "firebase-uid-123456",
  "email": "user1@example.com"
}
```

### **Test Échec - Compte Bloqué** (3 tentatives échouées):
Essayez 3 fois avec un mauvais mot de passe:
```json
{
  "email": "user1@example.com",
  "password": "MauvaisMotDePasse"
}
```

**Réponse Attendue** (401 Unauthorized - après 3 tentatives):
```json
{
  "success": false,
  "message": "Compte bloqué après 3 tentatives."
}
```

---

## 3️⃣ **VÉRIFICATION SESSION**

### **Endpoint**: `POST /api/auth/verify-session`

### **Headers**:
```json
Content-Type: application/json
Session-Token: b2c3d4e5-f6g7-8901-bcde-fg2345678901
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Session valide",
  "session": {
    "id": 1,
    "firebaseUid": "firebase-uid-123456",
    "sessionToken": "b2c3d4e5-f6g7-8901-bcde-fg2345678901",
    "createdAt": "2026-01-20T10:35:00",
    "expiresAt": "2026-01-20T11:35:00",
    "active": true,
    "ipAddress": "127.0.0.1"
  }
}
```

---

## 4️⃣ **PROLONGER SESSION (Refresh)**

### **Endpoint**: `POST /api/auth/refresh-session`

### **Headers**:
```json
Session-Token: b2c3d4e5-f6g7-8901-bcde-fg2345678901
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Session prolongée"
}
```

---

## 5️⃣ **VÉRIFICATION TOKEN FIREBASE**

### **Endpoint**: `POST /api/auth/verify`

### **Headers**:
```json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Note**: Ce token est obtenu côté client Firebase SDK (pas dans cette API).

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "uid": "firebase-uid-123456",
  "email": "user1@example.com",
  "claims": {
    "role": "USER"
  }
}
```

---

## 6️⃣ **MODIFIER INFORMATIONS UTILISATEUR**

### **Endpoint**: `PUT /api/auth/users/{uid}`

### **Headers**:
```json
Content-Type: application/json
Authorization: Bearer {firebase-id-token}
```

### **Body**:
```json
{
  "displayName": "Jean-Michel Dupont",
  "email": "jm.dupont@example.com"
}
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Utilisateur mis à jour",
  "user": {
    "uid": "firebase-uid-123456",
    "email": "jm.dupont@example.com",
    "displayName": "Jean-Michel Dupont"
  }
}
```

---

## 7️⃣ **DÉCONNEXION (Logout)**

### **Endpoint**: `POST /api/auth/logout`

### **Headers**:
```json
Session-Token: b2c3d4e5-f6g7-8901-bcde-fg2345678901
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Déconnexion réussie"
}
```

---

## 8️⃣ **DÉCONNEXION GLOBALE (Tous les appareils)**

### **Endpoint**: `POST /api/auth/logout-all`

### **Headers**:
```json
Authorization: Bearer {firebase-id-token}
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Toutes les sessions invalidées"
}
```

---

## 9️⃣ **RÉINITIALISER TENTATIVES (Manager)**

### **Endpoint**: `POST /api/auth/users/{email}/reset-attempts`

### **Exemple**: `POST /api/auth/users/user1@example.com/reset-attempts`

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Tentatives réinitialisées pour user1@example.com"
}
```

---

## 🔟 **DÉSACTIVER COMPTE (Manager)**

### **Endpoint**: `POST /api/auth/users/{uid}/disable`

### **Headers**:
```json
Authorization: Bearer {firebase-id-token}
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Compte désactivé"
}
```

---

## 1️⃣1️⃣ **RÉACTIVER COMPTE (Manager)**

### **Endpoint**: `POST /api/auth/users/{uid}/enable`

### **Headers**:
```json
Authorization: Bearer {firebase-id-token}
```

### **Réponse Attendue** (200 OK):
```json
{
  "success": true,
  "message": "Compte réactivé"
}
```

---

## 📊 Scénarios de Test Complets

### **Scénario 1: Inscription → Connexion → Modification**

1. **Inscrire un utilisateur**
   ```
   POST /api/auth/register
   → Sauvegarder sessionToken et uid
   ```

2. **Se connecter**
   ```
   POST /api/auth/login
   → Vérifier que sessionToken est différent
   ```

3. **Modifier les infos**
   ```
   PUT /api/auth/users/{uid}
   → Besoin d'un Authorization Bearer token
   ```

---

### **Scénario 2: Test Limite Tentatives**

1. **Inscrire un utilisateur**
   ```
   POST /api/auth/register avec email: test@example.com
   ```

2. **Essayer 3 connexions avec mauvais mot de passe**
   ```
   POST /api/auth/login (3 fois avec password incorrect)
   ```

3. **4ème tentative → Compte bloqué**
   ```
   Réponse: "Compte bloqué après 3 tentatives"
   ```

4. **Réinitialiser**
   ```
   POST /api/auth/users/test@example.com/reset-attempts
   ```

5. **Connexion réussie**
   ```
   POST /api/auth/login (avec bon password)
   ```

---

### **Scénario 3: Gestion des Sessions**

1. **Se connecter**
   ```
   POST /api/auth/login
   → Récupérer sessionToken
   ```

2. **Vérifier session**
   ```
   POST /api/auth/verify-session
   Header: Session-Token
   ```

3. **Attendre 60 minutes (ou changer la config)**
   ```
   Session expirée automatiquement
   ```

4. **Tenter de vérifier session expirée**
   ```
   POST /api/auth/verify-session
   → Réponse: "Session invalide ou expirée"
   ```

---

## 🔧 Configuration Paramétrable

### **Modifier dans `application.properties`**:

```properties
# Nombre max de tentatives (défaut: 3)
app.auth.max-failed-attempts=5

# Durée de vie des sessions en minutes (défaut: 60)
app.session.duration-minutes=30
```

---

## ✅ Checklist Complète

- [ ] Inscription utilisateur
- [ ] Connexion réussie
- [ ] Connexion échouée (3 tentatives)
- [ ] Compte bloqué
- [ ] Réinitialisation tentatives
- [ ] Vérification session
- [ ] Prolongation session
- [ ] Session expirée
- [ ] Modification infos utilisateur
- [ ] Déconnexion simple
- [ ] Déconnexion globale
- [ ] Désactivation compte
- [ ] Réactivation compte

---

## 🎯 Points Clés

✅ **Sessions**: Durée de vie paramétrable (défaut 60 min)  
✅ **Tentatives**: Limite paramétrable (défaut 3)  
✅ **API REST**: Toutes les fonctionnalités testables via Postman  
✅ **Swagger**: Documentation interactive sur `/swagger-ui.html`  
✅ **Docker**: Base PostgreSQL isolée

---

## 🐛 Dépannage

### Erreur: "Cannot resolve table 'local_users'"
→ Vérifier que PostgreSQL est démarré: `docker ps`

### Erreur: "Firebase initialization failed"
→ Vérifier que `serviceAccountKey.json` est dans `src/main/resources/`

### Erreur: Session expirée immédiatement
→ Vérifier `app.session.duration-minutes` dans `application.properties`

---

**Bon test! 🚀**
