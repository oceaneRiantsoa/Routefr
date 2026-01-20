// frontend/src/MapView.jsx
import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import axios from 'axios';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Correct marker icon issue
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

// ============================
// CONFIGURATION DES TUILES
// ============================
// Mode OFFLINE : Utilise le serveur local tileserver-gl (tuiles raster converties)
const TILE_URL_OFFLINE = 'http://localhost:8081/styles/basic/{z}/{x}/{y}.png';

// Mode ONLINE : Utilise OpenStreetMap (nécessite internet)
const TILE_URL_ONLINE = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

// Choisir le mode ici : true = offline, false = online
// ⚠️ Pour activer offline, mets true ET assure-toi que tileserver fonctionne
const USE_OFFLINE_TILES = false;

const MapView = () => {
  const [points, setPoints] = useState([]);
  const [tileError, setTileError] = useState(false);

  useEffect(() => {
    axios.get('http://localhost:8080/api/map/points')
      .then(res => setPoints(res.data))
      .catch(err => console.error('Error fetching points:', err));
  }, []);

  // Utiliser les tuiles offline si activé, sinon online
  // Si erreur avec offline, basculer automatiquement vers online
  const tileUrl = (USE_OFFLINE_TILES && !tileError) ? TILE_URL_OFFLINE : TILE_URL_ONLINE;

  return (
    <MapContainer center={[-18.8792, 47.5146]} zoom={13} style={{ height: '100vh', width: '100%' }}>
      <TileLayer
        url={tileUrl}
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        eventHandlers={{
          tileerror: () => {
            if (USE_OFFLINE_TILES && !tileError) {
              console.warn('Serveur de tuiles offline non disponible, bascule vers online');
              setTileError(true);
            }
          }
        }}
      />
      {points.map(point => (
        <Marker key={point.id} position={[point.lat, point.lng]}>
          <Popup>
            ID: {point.id}<br />
            Lat: {point.lat}<br />
            Lng: {point.lng}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
};

export default MapView;
