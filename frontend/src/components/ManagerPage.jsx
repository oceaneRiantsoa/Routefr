// frontend/src/components/ManagerPage.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import './ManagerPage.css';

const ManagerPage = () => {
  const navigate = useNavigate();

  return (
    <div className="manager-page">
      <div className="manager-header">
        <h1>🛠️ Espace Manager</h1>
        <p>Bienvenue dans l'interface de gestion</p>
      </div>

      <div className="manager-cards">
        {/* Carte Synchronisation Firebase */}
        <div 
          className="manager-card sync-card"
          onClick={() => navigate('/manager/sync')}
        >
          <div className="card-icon">🔄</div>
          <h2>Synchronisation Firebase</h2>
          <p>Récupérer les signalements en ligne</p>
          <ul>
            <li>Aperçu des signalements Firebase</li>
            <li>Synchronisation vers base locale</li>
            <li>Statistiques de synchronisation</li>
          </ul>
          <button className="card-button">Synchroniser →</button>
        </div>

        <div 
          className="manager-card signalements-card"
          onClick={() => navigate('/manager/signalements')}
        >
          <div className="card-icon">🗺️</div>
          <h2>Gestion des Signalements</h2>
          <p>Consulter et gérer les signalements routiers</p>
          <ul>
            <li>Voir tous les signalements</li>
            <li>Modifier les informations (surface, budget, entreprise)</li>
            <li>Changer les statuts</li>
            <li>Ajouter des notes</li>
          </ul>
          <button className="card-button">Accéder →</button>
        </div>

        <div 
          className="manager-card users-card"
          onClick={() => navigate('/manager/users')}
        >
          <div className="card-icon">👤</div>
          <h2>Gestion des Utilisateurs</h2>
          <p>Gérer les utilisateurs bloqués</p>
          <ul>
            <li>Voir les utilisateurs bloqués</li>
            <li>Débloquer les utilisateurs</li>
            <li>Consulter l'historique</li>
          </ul>
          <button className="card-button">Accéder →</button>
        </div>

        <div 
          className="manager-card map-card"
          onClick={() => navigate('/')}
        >
          <div className="card-icon">🌍</div>
          <h2>Carte des Signalements</h2>
          <p>Voir la carte interactive</p>
          <ul>
            <li>Visualiser tous les points</li>
            <li>Récapitulatif en temps réel</li>
            <li>Statistiques globales</li>
          </ul>
          <button className="card-button">Voir la carte →</button>
        </div>
      </div>

      <div className="manager-footer">
        <p>© 2026 Route Signalement - Interface Manager</p>
      </div>
    </div>
  );
};

export default ManagerPage;
