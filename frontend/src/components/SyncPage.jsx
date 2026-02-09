import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './SyncPage.css';

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8086';

const SyncPage = () => {
  const navigate = useNavigate();
  const [syncing, setSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState(null);
  const [syncResult, setSyncResult] = useState(null);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState(null);

  // État pour la synchronisation des utilisateurs
  const [usersSyncStatus, setUsersSyncStatus] = useState(null);
  const [syncingUsers, setSyncingUsers] = useState(false);
  const [usersSyncResult, setUsersSyncResult] = useState(null);

  useEffect(() => {
    loadStats();
    loadUsersSyncStatus();
  }, []);

  const loadStats = async () => {
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/stats');
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Erreur chargement stats:', err);
    }
  };

  const loadUsersSyncStatus = async () => {
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/users/status');
      if (response.ok) {
        const data = await response.json();
        setUsersSyncStatus(data);
      }
    } catch (err) {
      console.error('Erreur chargement statut utilisateurs:', err);
    }
  };

  const handleSyncUsers = async () => {
    if (!window.confirm('Synchroniser les utilisateurs vers Firebase?')) {
      return;
    }

    setSyncingUsers(true);
    setUsersSyncResult(null);
    setError(null);

    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      const result = await response.json();
      setUsersSyncResult(result);

      // Recharger le statut
      loadUsersSyncStatus();

    } catch (err) {
      setError('Erreur synchronisation utilisateurs: ' + err.message);
    } finally {
      setSyncingUsers(false);
    }
  };

  const handleFullSync = async () => {
    if (!window.confirm('Lancer la synchronisation complete?')) {
      return;
    }

    setSyncing(true);
    setError(null);
    setSyncResult(null);
    setSyncProgress({ step: 1, message: 'Etape 1/2 : Recuperation depuis Firebase (Pull)...' });

    let pullResult = null;
    let pushResult = null;

    try {
      const pullResponse = await fetch(BACKEND_URL + '/api/manager/sync/pull', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!pullResponse.ok) {
        throw new Error('Erreur lors du Pull: ' + pullResponse.status);
      }

      pullResult = await pullResponse.json();

      setSyncProgress({ step: 2, message: 'Etape 2/2 : Envoi vers Firebase (Push)...' });

      const pushResponse = await fetch(BACKEND_URL + '/api/manager/sync/push', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!pushResponse.ok) {
        throw new Error('Erreur lors du Push: ' + pushResponse.status);
      }

      pushResult = await pushResponse.json();

      setSyncResult({
        success: true,
        pull: pullResult,
        push: pushResult,
        timestamp: new Date().toISOString()
      });

      loadStats();

    } catch (err) {
      setError('Erreur synchronisation: ' + err.message);
      setSyncResult({
        success: false,
        pull: pullResult,
        push: pushResult,
        error: err.message,
        timestamp: new Date().toISOString()
      });
    } finally {
      setSyncing(false);
      setSyncProgress(null);
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  return (
    <div className="sync-page">
      <div className="page-top-bar">
        <span className="page-subtitle">Synchronisation bidirectionnelle avec Firebase</span>
      </div>

      {stats && (
        <div className="stats-section">
          <h3>Statistiques locales</h3>
          <div className="stats-cards">
            <div className="stat-card total">
              <div className="stat-number">{stats.total || 0}</div>
              <div className="stat-label">Total synchronises</div>
            </div>
            <div className="stat-card nouveau">
              <div className="stat-number">{stats.nouveaux || 0}</div>
              <div className="stat-label">Nouveaux</div>
            </div>
            <div className="stat-card en-cours">
              <div className="stat-number">{stats.enCours || 0}</div>
              <div className="stat-label">En cours</div>
            </div>
            <div className="stat-card traite">
              <div className="stat-number">{stats.traites || 0}</div>
              <div className="stat-label">Traites</div>
            </div>
          </div>
        </div>
      )}

      <div className="sync-action-section">
        <div className="sync-description">
          <h2>Synchronisation complete</h2>
          <p>Ce bouton effectue automatiquement :</p>
          <ul>
            <li><strong>Pull :</strong> Recupere les nouveaux signalements depuis Firebase</li>
            <li><strong>Push :</strong> Envoie les signalements traites vers Firebase</li>
          </ul>
        </div>

        <button
          className={'main-sync-button ' + (syncing ? 'syncing' : '')}
          onClick={handleFullSync}
          disabled={syncing}
        >
          {syncing ? 'Synchronisation en cours...' : 'Synchroniser maintenant'}
        </button>

        {syncProgress && (
          <div className="sync-progress">
            <div className="progress-bar">
              <div
                className="progress-fill"
                style={{ width: syncProgress.step === 1 ? '50%' : '100%' }}
              ></div>
            </div>
            <p className="progress-message">{syncProgress.message}</p>
          </div>
        )}
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {syncResult && (
        <div className={'sync-results ' + (syncResult.success ? 'success' : 'partial')}>
          <h3>
            {syncResult.success ? 'Synchronisation reussie !' : 'Synchronisation partielle'}
          </h3>

          <div className="results-grid">
            <div className="result-card pull-result">
              <h4>Pull (Recuperation)</h4>
              {syncResult.pull ? (
                <div className="result-details">
                  <div className="result-item">
                    <span className="result-label">Total Firebase:</span>
                    <span className="result-value">{syncResult.pull.totalFirebase || 0}</span>
                  </div>
                  <div className="result-item success">
                    <span className="result-label">Nouveaux:</span>
                    <span className="result-value">{syncResult.pull.nouveaux || 0}</span>
                  </div>
                  <div className="result-item">
                    <span className="result-label">Mis a jour:</span>
                    <span className="result-value">{syncResult.pull.misAJour || 0}</span>
                  </div>
                  <div className="result-item">
                    <span className="result-label">Ignores:</span>
                    <span className="result-value">{syncResult.pull.ignores || 0}</span>
                  </div>
                </div>
              ) : (
                <p className="result-error">Non execute</p>
              )}
            </div>

            <div className="result-card push-result">
              <h4>Push (Envoi)</h4>
              {syncResult.push ? (
                <div className="result-details">
                  <div className="result-item success">
                    <span className="result-label">Total envoyes:</span>
                    <span className="result-value">{syncResult.push.totalEnvoyes || 0}</span>
                  </div>
                  <div className="result-item">
                    <span className="result-label">Nouveaux:</span>
                    <span className="result-value">{syncResult.push.nouveaux || 0}</span>
                  </div>
                  <div className="result-item">
                    <span className="result-label">Mis a jour:</span>
                    <span className="result-value">{syncResult.push.misAJour || 0}</span>
                  </div>
                </div>
              ) : (
                <p className="result-error">Non execute</p>
              )}
            </div>
          </div>

          <div className="sync-timestamp">
            Synchronise le: {formatDate(syncResult.timestamp)}
          </div>
        </div>
      )}

      <div className="info-section">
        <h3>Comment ca marche ?</h3>
        <div className="info-cards">
          <div className="info-card">
            <div className="info-icon"></div>
            <h4>Pull</h4>
            <p>Recupere les signalements crees par les utilisateurs depuis application mobile Firebase.</p>
          </div>
          <div className="info-card">
            <div className="info-icon"></div>
            <h4>Push</h4>
            <p>Envoie les signalements traites vers Firebase pour affichage mobile.</p>
          </div>
        </div>
      </div>

      {/* Section Synchronisation Utilisateurs */}
      <div className="users-sync-section">
        <h2>Synchronisation des Utilisateurs</h2>
        <p>Les comptes créés localement doivent être synchronisés vers Firebase pour fonctionner sur l'application mobile.</p>

        {usersSyncStatus && (
          <div className="users-status">
            <div className="users-count">
              <span className="count-number">{usersSyncStatus.usersNotSynced || 0}</span>
              <span className="count-label">utilisateur(s) à synchroniser</span>
            </div>

            {usersSyncStatus.users && usersSyncStatus.users.length > 0 && (
              <div className="users-list">
                <h4>Utilisateurs en attente :</h4>
                <table className="users-table">
                  <thead>
                    <tr>
                      <th>Email</th>
                      <th>Nom</th>
                      <th>Rôle</th>
                      <th>Créé le</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usersSyncStatus.users.map((user, index) => (
                      <tr key={index}>
                        <td>{user.email}</td>
                        <td>{user.displayName || '-'}</td>
                        <td><span className={`role-badge ${user.role?.toLowerCase()}`}>{user.role}</span></td>
                        <td>{user.createdAt ? formatDate(user.createdAt) : '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        <button
          className={`sync-users-button ${syncingUsers ? 'syncing' : ''}`}
          onClick={handleSyncUsers}
          disabled={syncingUsers || (usersSyncStatus?.usersNotSynced === 0)}
        >
          {syncingUsers ? 'Synchronisation...' : 'Synchroniser les utilisateurs vers Firebase'}
        </button>

        {usersSyncResult && (
          <div className={`users-sync-result ${usersSyncResult.success ? 'success' : 'error'}`}>
            <h4>{usersSyncResult.success ? 'Succès' : 'Terminé avec erreurs'}</h4>
            <p>{usersSyncResult.message}</p>
            <div className="sync-details">
              <span>Envoyés: {usersSyncResult.pushedToFirebase || 0}</span>
              {usersSyncResult.errors > 0 && <span>Erreurs: {usersSyncResult.errors}</span>}
            </div>
            {usersSyncResult.errorDetails && usersSyncResult.errorDetails.length > 0 && (
              <div className="error-details">
                <h5>Détails des erreurs:</h5>
                <ul>
                  {usersSyncResult.errorDetails.map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default SyncPage;
