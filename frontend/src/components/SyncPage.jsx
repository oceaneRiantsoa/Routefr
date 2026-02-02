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

  useEffect(() => {
    loadStats();
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
      <div className="sync-header">
        <button className="back-button" onClick={() => navigate('/manager')}>
          Retour
        </button>
        <h1>Synchronisation Firebase</h1>
        <p>Synchronisation automatique bidirectionnelle avec Firebase</p>
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
            <div className="info-icon">📥</div>
            <h4>Pull</h4>
            <p>Recupere les signalements crees par les utilisateurs depuis application mobile Firebase.</p>
          </div>
          <div className="info-card">
            <div className="info-icon">📤</div>
            <h4>Push</h4>
            <p>Envoie les signalements traites vers Firebase pour affichage mobile.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SyncPage;
