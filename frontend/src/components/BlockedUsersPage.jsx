import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './BlockedUsersPage.css';

const API_BASE_URL = 'http://localhost:8086/api/manager';

const BlockedUsersPage = () => {
  const navigate = useNavigate();
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unblocking, setUnblocking] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  // Paramètres de sécurité
  const [securitySettings, setSecuritySettings] = useState({
    sessionDurationMinutes: 60,
    maxFailedAttempts: 3
  });
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsChanged, setSettingsChanged] = useState(false);

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

  // Charger les paramètres de sécurité
  const fetchSecuritySettings = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/security`);
      setSecuritySettings(response.data);
      setSettingsChanged(false);
    } catch (err) {
      console.error('Erreur chargement paramètres:', err);
    }
  };

  useEffect(() => {
    fetchBlockedUsers();
    fetchSecuritySettings();
  }, []);

  // Mettre à jour les paramètres de sécurité
  const handleUpdateSettings = async () => {
    try {
      setSettingsLoading(true);
      const response = await axios.put(`${API_BASE_URL}/settings/security`, securitySettings);
      setSecuritySettings(response.data);
      setSettingsChanged(false);
      setSuccessMessage('✅ Paramètres de sécurité mis à jour !');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      const errorMsg = err.response?.data?.error || 'Erreur lors de la mise à jour';
      setError(errorMsg);
    } finally {
      setSettingsLoading(false);
    }
  };

  // Réinitialiser les paramètres
  const handleResetSettings = async () => {
    if (!window.confirm('Voulez-vous réinitialiser les paramètres aux valeurs par défaut ?')) {
      return;
    }
    try {
      setSettingsLoading(true);
      const response = await axios.post(`${API_BASE_URL}/settings/security/reset`);
      setSecuritySettings(response.data);
      setSettingsChanged(false);
      setSuccessMessage('✅ Paramètres réinitialisés !');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      setError('Erreur lors de la réinitialisation');
    } finally {
      setSettingsLoading(false);
    }
  };

  // Gérer le changement des inputs
  const handleSettingChange = (field, value) => {
    const numValue = parseInt(value, 10);
    if (!isNaN(numValue) && numValue >= 0) {
      setSecuritySettings(prev => ({
        ...prev,
        [field]: numValue
      }));
      setSettingsChanged(true);
    }
  };

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
          <button className="back-button" onClick={() => navigate('/manager')}>
            ← Retour au Manager
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

        {/* Section Paramètres de sécurité */}
        <div className="settings-card">
          <h2>⚙️ Paramètres de sécurité</h2>
          <div className="settings-form">
            <div className="setting-item">
              <label htmlFor="sessionDuration">
                ⏱️ Durée de vie des sessions (minutes)
              </label>
              <input
                type="number"
                id="sessionDuration"
                min="1"
                max="1440"
                value={securitySettings.sessionDurationMinutes}
                onChange={(e) => handleSettingChange('sessionDurationMinutes', e.target.value)}
                className="setting-input"
              />
              <span className="setting-hint">Min: 1, Max: 1440 (24h)</span>
            </div>
            
            <div className="setting-item">
              <label htmlFor="maxAttempts">
                🔐 Limite de tentatives de connexion
              </label>
              <input
                type="number"
                id="maxAttempts"
                min="1"
                max="10"
                value={securitySettings.maxFailedAttempts}
                onChange={(e) => handleSettingChange('maxFailedAttempts', e.target.value)}
                className="setting-input"
              />
              <span className="setting-hint">Min: 1, Max: 10</span>
            </div>

            <div className="settings-actions">
              <button 
                className="save-settings-button"
                onClick={handleUpdateSettings}
                disabled={settingsLoading || !settingsChanged}
              >
                {settingsLoading ? '⏳ Sauvegarde...' : '💾 Sauvegarder'}
              </button>
              <button 
                className="reset-settings-button"
                onClick={handleResetSettings}
                disabled={settingsLoading}
              >
                🔄 Réinitialiser
              </button>
            </div>
          </div>
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
