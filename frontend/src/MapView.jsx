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
const TILE_URL_OFFLINE = 'http://localhost:8081/styles/basic/{z}/{x}/{y}.png';
const TILE_URL_ONLINE = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
const USE_OFFLINE_TILES = false;

// Formater les nombres avec séparateurs de milliers
const formatNumber = (num) => {
  return new Intl.NumberFormat('fr-FR').format(num);
};

// Formater la date
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Couleur selon le statut
const getStatusColor = (status) => {
  switch (status) {
    case 'Nouveau': return '#e74c3c';
    case 'En cours': return '#f39c12';
    case 'Terminé': return '#27ae60';
    default: return '#95a5a6';
  }
};

// Icône personnalisée selon le statut
const createCustomIcon = (status) => {
  const color = getStatusColor(status);
  return L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background-color: ${color};
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: 3px solid white;
      box-shadow: 0 2px 5px rgba(0,0,0,0.3);
    "></div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    popupAnchor: [0, -12]
  });
};

const MapView = () => {
  const [points, setPoints] = useState([]);
  const [recap, setRecap] = useState(null);
  const [tileError, setTileError] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Charger les points et le récapitulatif
    Promise.all([
      axios.get('http://localhost:8080/api/public/map/points'),
      axios.get('http://localhost:8080/api/public/map/recap')
    ])
      .then(([pointsRes, recapRes]) => {
        setPoints(pointsRes.data);
        setRecap(recapRes.data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Erreur chargement données:', err);
        setLoading(false);
      });
  }, []);

  const tileUrl = (USE_OFFLINE_TILES && !tileError) ? TILE_URL_OFFLINE : TILE_URL_ONLINE;

  return (
    <div style={{ position: 'relative', height: '100vh', width: '100%' }}>
      {/* Tableau récapitulatif */}
      <div className="recap-panel">
        <h3>📊 Récapitulatif</h3>
        {loading ? (
          <p>Chargement...</p>
        ) : recap ? (
          <table className="recap-table">
            <tbody>
              <tr>
                <td>📍 Nb de points</td>
                <td><strong>{recap.nbPoints}</strong></td>
              </tr>
              <tr>
                <td>📐 Surface totale</td>
                <td><strong>{formatNumber(recap.totalSurface)} m²</strong></td>
              </tr>
              <tr>
                <td>📈 Avancement</td>
                <td>
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{ width: `${recap.avancementPourcent}%` }}
                    ></div>
                  </div>
                  <strong>{recap.avancementPourcent}%</strong>
                </td>
              </tr>
              <tr>
                <td>💰 Budget total</td>
                <td><strong>{formatNumber(recap.totalBudget)} Ar</strong></td>
              </tr>
            </tbody>
          </table>
        ) : (
          <p>Aucune donnée</p>
        )}

        {/* Légende */}
        <div className="legend">
          <h4>Légende</h4>
          <div className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: '#e74c3c' }}></span>
            Nouveau
          </div>
          <div className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: '#f39c12' }}></span>
            En cours
          </div>
          <div className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: '#27ae60' }}></span>
            Terminé
          </div>
        </div>
      </div>

      {/* Carte */}
      <MapContainer center={[-18.8792, 47.5146]} zoom={13} style={{ height: '100%', width: '100%' }}>
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
          <Marker
            key={point.id}
            position={[point.lat, point.lng]}
            icon={createCustomIcon(point.status)}
          >
            <Popup className="custom-popup">
              <div className="popup-content">
                <h4>{point.probleme || 'Problème signalé'}</h4>
                <table className="popup-table">
                  <tbody>
                    <tr>
                      <td>📅 Date:</td>
                      <td>{formatDate(point.dateSignalement)}</td>
                    </tr>
                    <tr>
                      <td>🔄 Statut:</td>
                      <td>
                        <span
                          className="status-badge"
                          style={{ backgroundColor: getStatusColor(point.status) }}
                        >
                          {point.status}
                        </span>
                      </td>
                    </tr>
                    <tr>
                      <td>📐 Surface:</td>
                      <td>{formatNumber(point.surface)} m²</td>
                    </tr>
                    <tr>
                      <td>💰 Budget:</td>
                      <td>{formatNumber(point.budget)} Ar</td>
                    </tr>
                    <tr>
                      <td>🏢 Entreprise:</td>
                      <td>{point.entreprise || 'Non assignée'}</td>
                    </tr>
                  </tbody>
                </table>
                {point.commentaires && (
                  <div className="popup-comment">
                    <em>💬 {point.commentaires}</em>
                  </div>
                )}
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
};

export default MapView;
