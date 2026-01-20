# ============================
# Script de téléchargement des tuiles Antananarivo
# ============================

Write-Host "=== Téléchargement des tuiles Antananarivo ===" -ForegroundColor Green

$tilesDir = ".\tiles"

# Créer le dossier si nécessaire
if (-not (Test-Path $tilesDir)) {
    New-Item -ItemType Directory -Path $tilesDir
}

# Option 1: Télécharger depuis OpenMapTiles (fichier mbtiles pré-généré pour Madagascar)
# Note: Vous devrez peut-être créer un compte gratuit sur https://data.maptiler.com/

Write-Host ""
Write-Host "OPTION 1 - Téléchargement automatique (Madagascar complet)" -ForegroundColor Yellow
Write-Host "Taille: ~50-100 MB" -ForegroundColor Cyan

$mbtilesUrl = "https://github.com/openmaptiles/openmaptiles/releases/download/v3.13/zurich_switzerland.mbtiles"
# Note: Remplacer par le lien Madagascar quand disponible

Write-Host ""
Write-Host "=== INSTRUCTIONS MANUELLES ===" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Allez sur: https://data.maptiler.com/downloads/planet/" -ForegroundColor White
Write-Host "2. Créez un compte gratuit" -ForegroundColor White
Write-Host "3. Téléchargez 'Africa' > 'Madagascar'" -ForegroundColor White
Write-Host "4. Renommez le fichier en 'antananarivo.mbtiles'" -ForegroundColor White
Write-Host "5. Placez-le dans le dossier: $tilesDir" -ForegroundColor White
Write-Host ""
Write-Host "OU utilisez l'alternative gratuite:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Allez sur: https://download.geofabrik.de/africa/madagascar.html" -ForegroundColor White
Write-Host "2. Téléchargez 'madagascar-latest.osm.pbf'" -ForegroundColor White
Write-Host "3. Convertissez avec tilemaker (voir README)" -ForegroundColor White
Write-Host ""

# Vérifier si le fichier existe
if (Test-Path "$tilesDir\antananarivo.mbtiles") {
    Write-Host "✓ Fichier antananarivo.mbtiles trouvé!" -ForegroundColor Green
} else {
    Write-Host "✗ Fichier antananarivo.mbtiles manquant" -ForegroundColor Red
    Write-Host "  Placez le fichier .mbtiles dans: $tilesDir" -ForegroundColor Red
}
