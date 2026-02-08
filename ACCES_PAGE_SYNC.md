# 🔄 Accès à la page de synchronisation Firebase

## 🎯 URLs d'accès

### Option 1 : Via l'interface Manager (recommandé)
1. Ouvrez **http://localhost:3000/manager**
2. Cliquez sur la carte **"🔄 Synchronisation Firebase"**
3. Vous serez redirigé vers `/manager/sync`

### Option 2 : Accès direct
Ouvrez directement : **http://localhost:3000/manager/sync**

## ⚠️ Si la page ne s'affiche pas correctement

### Problème : Erreur 404 sur bundle.js
**Cause** : Cache du navigateur

**Solutions** :

#### Solution 1 : Vider le cache du navigateur (RECOMMANDÉ)
- **Firefox** : `Ctrl + Shift + Suppr` → Cocher "Cache" → "Effacer maintenant"
- **Chrome** : `Ctrl + Shift + Suppr` → "Images et fichiers en cache" → "Effacer les données"
- Puis : `Ctrl + F5` pour recharger la page

#### Solution 2 : Mode navigation privée
1. Ouvrez une fenêtre de navigation privée (`Ctrl + Shift + P` sur Firefox)
2. Accédez à http://localhost:3000/manager

#### Solution 3 : Forcer le rechargement sans cache
- Appuyez sur `Ctrl + Shift + R` (ou `Ctrl + F5`)
- Cela force le navigateur à ignorer le cache

#### Solution 4 : Rebuild complet du frontend
```bash
cd /home/finoana/Documents/GitHub/Routefr
docker-compose build --no-cache frontend
docker-compose up -d frontend
```

## 📋 Fonctionnalités de la page Sync

Une fois sur la page `/manager/sync`, vous aurez accès à :

### 🔄 Synchronisation
- **Bouton "Synchroniser maintenant"** : Lance la synchronisation avec Firebase
- **Statut de la dernière synchronisation** : Affiche les résultats
- **Horodatage** : Quand la dernière sync a eu lieu

### 📊 Statistiques
- **Total Firebase** : Nombre de signalements dans Firebase
- **Nouveaux** : Signalements ajoutés à la base locale
- **Mis à jour** : Signalements existants modifiés
- **Ignorés** : Signalements inchangés
- **Erreurs** : Nombre d'erreurs rencontrées

### 👁️ Aperçu des signalements
Liste des signalements disponibles dans Firebase avec :
- ID Firebase
- Email de l'utilisateur
- Coordonnées (latitude/longitude)
- Type de problème
- Description
- Date de création

## 🧪 Test rapide

Pour tester que tout fonctionne, exécutez dans le terminal :

```bash
# Test de l'endpoint de synchronisation
curl -X POST http://localhost:8086/api/manager/sync/pull | jq

# Test de l'aperçu
curl http://localhost:8086/api/manager/sync/preview | jq

# Test des statistiques
curl http://localhost:8086/api/manager/sync/stats | jq
```

## 📱 Navigation dans l'application

```
http://localhost:3000/
├── /                           → Carte publique
└── /manager                    → Page d'accueil Manager
    ├── /manager/sync           → 🔄 Synchronisation Firebase
    ├── /manager/signalements   → 🗺️ Gestion des signalements
    └── /manager/users          → 👤 Gestion des utilisateurs
```

## ✅ Vérification que tout fonctionne

### Backend
```bash
docker logs springboot_api --tail 10
```
Vous devriez voir : "✅ Firebase Realtime Database initialisé avec succès!"

### Frontend
```bash
docker logs react_frontend --tail 10
```
Aucune erreur 404 ne doit apparaître

### Base de données
```bash
docker exec -i postgis_db psql -U postgres -d route_signalement -c "SELECT COUNT(*) FROM signalement_firebase;"
```
Vous devriez voir le nombre de signalements synchronisés

## 🎉 Résultat attendu

Après avoir cliqué sur "Synchroniser maintenant" :
- ✅ Message de succès avec statistiques
- ✅ Liste des signalements apparaît
- ✅ Signalements visibles dans `/manager/signalements`
- ✅ Points affichés sur la carte publique

---

**Note** : Si vous voyez toujours des erreurs 404 sur `bundle.js` dans les logs nginx, c'est juste le cache du navigateur. La vraie ressource est `main.dc7b8c95.js` et elle fonctionne correctement.
