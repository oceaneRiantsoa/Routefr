// frontend/src/components/ManagerPage.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout, getCurrentUser } from '../services/authService';
import CreateUserModal from './CreateUserModal';
import './ManagerPage.css';

const ManagerPage = () => {
  const navigate = useNavigate();
  const user = getCurrentUser();
  const [showCreateUserModal, setShowCreateUserModal] = useState(false);

  const handleLogout = async () => {
    if (window.confirm('Voulez-vous vraiment vous déconnecter ?')) {
      await logout();
      navigate('/manager/login');
    }
  };

  const handleUserCreated = (newUser) => {
    console.log('Nouvel utilisateur créé:', newUser);
    // Vous pouvez ajouter une notification ou rafraîchir une liste d'utilisateurs ici
  };

  return (
    <div className="manager-page">
      <div className="manager-header">
        <div className="header-top">
          <h1>🛠️ Espace Manager</h1>
          <div className="user-info">
            <button 
              className="create-user-button" 
              onClick={() => setShowCreateUserModal(true)}
            >
              ➕ Créer un utilisateur
            </button>
            <span className="user-name">👤 {user?.displayName || user?.email || 'Manager'}</span>
            <button className="logout-button" onClick={handleLogout}>
              🚪 Déconnexion
            </button>
          </div>
        </div>
        <p>Bienvenue dans l'interface de gestion</p>
      </div>

      {/* Modal de création d'utilisateur */}
      <CreateUserModal
        isOpen={showCreateUserModal}
        onClose={() => setShowCreateUserModal(false)}
        onUserCreated={handleUserCreated}
      />

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

        {/* Carte Statistiques */}
        <div 
          className="manager-card stats-card"
          onClick={() => navigate('/manager/statistiques')}
        >
          <div className="card-icon">📊</div>
          <h2>Statistiques & Avancement</h2>
          <p>Analyser les performances de traitement</p>
          <ul>
            <li>Délai moyen de traitement</li>
            <li>Répartition par statut (0%, 50%, 100%)</li>
            <li>Délais par type de problème</li>
          </ul>
          <button className="card-button">Voir les stats →</button>
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
