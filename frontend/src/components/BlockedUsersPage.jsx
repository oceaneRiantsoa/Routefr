import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './BlockedUsersPage.css';

const API_BASE_URL = 'http://localhost:8086/api/manager';

const BlockedUsersPage = ({ onBack }) => {
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unblocking, setUnblocking] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  // Charger les utilisateurs bloqués
  const fetchBlockedUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axios.get(`${API_BASE_URL}/blocked-users`);
      setBlockedUsers(response.data);
    } catch (err) {
      setError('Erreur lors du chargement des utilisateurs bloqués');
      console.error('Erreur:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBlockedUsers();
  }, []);

  // Débloquer un utilisateur
  const handleUnblock = async (userId, userEmail) => {
    if (!window.confirm(`Voulez-vous vraiment débloquer l'utilisateur ${userEmail} ?`)) {
      return;
    }

    try {
      setUnblocking(userId);
      await axios.post(`${API_BASE_URL}/users/${userId}/unblock`);
      
      // Retirer l'utilisateur de la liste
      setBlockedUsers(prev => prev.filter(user => user.id !== userId));
      
      // Afficher message de succès
      setSuccessMessage(`✅ ${userEmail} a été débloqué avec succès !`);
      setTimeout(() => setSuccessMessage(''), 3000);
      
    } catch (err) {
      setError(`Erreur lors du déblocage de ${userEmail}`);
      console.error('Erreur:', err);
    } finally {
      setUnblocking(null);
    }
  };

  // Formater la date
  const formatDate = (dateString) => {
    if (!dateString) return 'Jamais connecté';
    return new Date(dateString).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="blocked-users-container">
      {/* Header */}
      <header className="page-header">
        <div className="header-content">
          <button className="back-button" onClick={onBack}>
            ← Retour à la carte
          </button>
          <h1>🔒 Gestion des Utilisateurs Bloqués</h1>
          <p className="subtitle">Interface Manager - Déblocage des comptes</p>
        </div>
      </header>

      {/* Main content */}
      <main className="main-content">
        {/* Stats card */}
        <div className="stats-card">
          <div className="stat-item">
            <span className="stat-number">{blockedUsers.length}</span>
            <span className="stat-label">Utilisateurs bloqués</span>
          </div>
          <button className="refresh-button" onClick={fetchBlockedUsers} disabled={loading}>
            🔄 Actualiser
          </button>
        </div>

        {/* Messages */}
        {successMessage && (
          <div className="alert alert-success">
            {successMessage}
          </div>
        )}

        {error && (
          <div className="alert alert-error">
            ⚠️ {error}
            <button className="close-alert" onClick={() => setError(null)}>×</button>
          </div>
        )}

        {/* Loading state */}
        {loading && (
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Chargement des utilisateurs...</p>
          </div>
        )}

        {/* Empty state */}
        {!loading && blockedUsers.length === 0 && (
          <div className="empty-state">
            <div className="empty-icon">✨</div>
            <h3>Aucun utilisateur bloqué</h3>
            <p>Tous les comptes sont en règle !</p>
          </div>
        )}

        {/* Users table */}
        {!loading && blockedUsers.length > 0 && (
          <div className="table-container">
            <table className="users-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Email</th>
                  <th>Nom</th>
                  <th>Rôle</th>
                  <th>Tentatives échouées</th>
                  <th>Dernière connexion</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {blockedUsers.map(user => (
                  <tr key={user.id}>
                    <td className="id-cell">#{user.id}</td>
                    <td className="email-cell">
                      <span className="email-icon">📧</span>
                      {user.email}
                    </td>
                    <td>{user.displayName || <span className="no-data">Non renseigné</span>}</td>
                    <td>
                      <span className={`role-badge role-${user.role?.toLowerCase()}`}>
                        {user.role || 'USER'}
                      </span>
                    </td>
                    <td className="attempts-cell">
                      <span className="attempts-badge">
                        {user.failedAttempts} tentative{user.failedAttempts > 1 ? 's' : ''}
                      </span>
                    </td>
                    <td className="date-cell">{formatDate(user.lastLogin)}</td>
                    <td className="actions-cell">
                      <button
                        className="unblock-button"
                        onClick={() => handleUnblock(user.id, user.email)}
                        disabled={unblocking === user.id}
                      >
                        {unblocking === user.id ? (
                          <>
                            <span className="btn-spinner"></span>
                            Déblocage...
                          </>
                        ) : (
                          <>
                            🔓 Débloquer
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="page-footer">
        <p>🛣️ Route Signalement - Interface Manager</p>
      </footer>
    </div>
  );
};

export default BlockedUsersPage;
