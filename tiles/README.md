# Serveur de Tuiles Offline - Antananarivo

## 📋 Configuration requise

Pour utiliser les cartes **sans connexion internet**, vous devez télécharger les tuiles d'Antananarivo.

---

## 🔽 Étape 1 : Télécharger les tuiles

### Option A : Depuis MapTiler (Recommandé - Plus simple)

1. Allez sur **https://data.maptiler.com/downloads/planet/**
2. Créez un compte gratuit
3. Naviguez vers **Africa** > **Madagascar**
4. Téléchargez le fichier `.mbtiles`
5. Renommez-le en `antananarivo.mbtiles`
6. Placez-le dans le dossier `tiles/`

### Option B : Depuis Geofabrik (Gratuit - Plus technique)

1. Téléchargez Madagascar : https://download.geofabrik.de/africa/madagascar-latest.osm.pbf
2. Installez `tilemaker` : https://github.com/systemed/tilemaker
3. Convertissez en mbtiles :
   ```bash
   tilemaker --input madagascar-latest.osm.pbf --output antananarivo.mbtiles
   ```

### Option C : Extraire seulement Antananarivo (fichier plus petit)

1. Installez `osmium-tool`
2. Extrayez la zone Antananarivo :
   ```bash
   osmium extract -b 47.4,-19.0,47.6,-18.7 madagascar-latest.osm.pbf -o antananarivo.osm.pbf
   ```
3. Convertissez avec tilemaker

---

## 🚀 Étape 2 : Démarrer le serveur

```powershell
cd RouteSignalement
docker-compose up -d --build
```

Le serveur de tuiles sera accessible sur : **http://localhost:8081**

---

## ✅ Vérification

1. Ouvrez **http://localhost:8081** dans votre navigateur
2. Vous devriez voir l'interface TileServer GL
3. Si la carte s'affiche, c'est configuré !

---

## 🔄 Basculer entre Online/Offline

Dans `frontend/src/MapView.jsx`, changez cette ligne :

```jsx
// true = utilise serveur local (offline)
// false = utilise OpenStreetMap (online)
const USE_OFFLINE_TILES = true;
```

---

## 📁 Structure des fichiers

```
RouteSignalement/
├── tiles/
│   ├── config.json           ← Configuration TileServer
│   └── antananarivo.mbtiles  ← FICHIER À AJOUTER (tuiles)
├── docker-compose.yml        ← Service tileserver ajouté
└── frontend/
    └── src/
        └── MapView.jsx       ← Configuration tuiles modifiée
```

---

## ⚠️ Taille des fichiers

| Zone | Taille approximative |
|------|---------------------|
| Madagascar complet | ~100-200 MB |
| Antananarivo seul | ~10-50 MB |
| Zurich (exemple) | ~50 MB |

---

## 🐛 Dépannage

**Le serveur de tuiles ne démarre pas ?**
- Vérifiez que le fichier `.mbtiles` est dans `tiles/`
- Vérifiez les logs : `docker logs tileserver_offline`

**La carte ne s'affiche pas ?**
- Ouvrez la console du navigateur (F12) pour voir les erreurs
- Vérifiez que le port 8081 n'est pas utilisé

**Fallback automatique :**
- Si le serveur offline est indisponible, l'application bascule automatiquement vers OpenStreetMap (online)
