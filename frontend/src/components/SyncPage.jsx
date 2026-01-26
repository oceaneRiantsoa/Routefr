// frontend/src/components/SyncPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './SyncPage.css';

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8086';

const SyncPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [syncResult, setSyncResult] = useState(null);
  const [previewData, setPreviewData] = useState(null);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState(null);
  const [activeTab, setActiveTab] = useState('apercu');

  // Charger les statistiques au démarrage
  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/api/manager/sync/stats`);
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Erreur chargement stats:', err);
    }
  };

  // Aperçu des signalements Firebase
  const handlePreview = async () => {
    setPreviewLoading(true);
    setError(null);
    setPreviewData(null);

    try {
      const response = await fetch(`${BACKEND_URL}/api/manager/sync/preview`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || `Erreur serveur: ${response.status}`);
      }
      
      // Vérifier si c'est un tableau (succès) ou un objet erreur
      if (Array.isArray(data)) {
        setPreviewData(data);
        setActiveTab('apercu');
      } else if (data.error) {
        throw new Error(data.error);
      }
    } catch (err) {
      if (err.message.includes('NetworkError') || err.message.includes('fetch')) {
        setError('Impossible de contacter le serveur. Vérifiez que le backend est démarré.');
      } else {
        setError(err.message);
      }
    } finally {
      setPreviewLoading(false);
    }
  };

  // Synchronisation des signalements
  const handleSync = async () => {
    if (!window.confirm('Voulez-vous synchroniser les signalements depuis Firebase?')) {
      return;
    }

    setLoading(true);
    setError(null);
    setSyncResult(null);

    try {
      const response = await fetch(`${BACKEND_URL}/api/manager/sync/pull`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Erreur serveur: ${response.status}`);
      }

      const data = await response.json();
      setSyncResult(data);
      setActiveTab('resultat');
      
      // Recharger les stats après synchronisation
      loadStats();
    } catch (err) {
      setError('Erreur lors de la synchronisation: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'nouveau': { label: 'Nouveau', className: 'status-nouveau' },
      'en_cours': { label: 'En cours', className: 'status-en-cours' },
      'traite': { label: 'Traité', className: 'status-traite' },
      'rejete': { label: 'Rejeté', className: 'status-rejete' }
    };
    const info = statusMap[status] || { label: status || 'Inconnu', className: 'status-unknown' };
    return <span className={`status-badge ${info.className}`}>{info.label}</span>;
  };

  return (
    <div className="sync-page">
      <div className="sync-header">
        <button className="back-button" onClick={() => navigate('/manager')}>
          ← Retour
        </button>
        <h1>🔄 Synchronisation Firebase</h1>
        <p>Récupérer les signalements depuis la base de données en ligne</p>
      </div>

      {/* Statistiques locales */}
      {stats && (
        <div className="stats-section">
          <h3>📊 Statistiques locales</h3>
          <div className="stats-cards">
            <div className="stat-card">
              <div className="stat-number">{stats.total || 0}</div>
              <div className="stat-label">Total synchronisés</div>
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
              <div className="stat-label">Traités</div>
            </div>
          </div>
        </div>
      )}

      {/* Boutons d'action */}
      <div className="action-buttons">
        <button 
          className="preview-button"
          onClick={handlePreview}
          disabled={previewLoading}
        >
          {previewLoading ? '⏳ Chargement...' : '👀 Aperçu Firebase'}
        </button>
        
        <button 
          className="sync-button"
          onClick={handleSync}
          disabled={loading}
        >
          {loading ? '⏳ Synchronisation...' : '🔄 Synchroniser maintenant'}
        </button>
      </div>

      {/* Erreur */}
      {error && (
        <div className="error-message">
          ❌ {error}
        </div>
      )}

      {/* Onglets */}
      <div className="tabs">
        <button 
          className={`tab ${activeTab === 'apercu' ? 'active' : ''}`}
          onClick={() => setActiveTab('apercu')}
        >
          👀 Aperçu Firebase
        </button>
        <button 
          className={`tab ${activeTab === 'resultat' ? 'active' : ''}`}
          onClick={() => setActiveTab('resultat')}
        >
          📋 Résultat synchronisation
        </button>
      </div>

      {/* Contenu des onglets */}
      <div className="tab-content">
        {activeTab === 'apercu' && previewData && (
          <div className="preview-section">
            <h3>📥 {previewData.length} signalement(s) dans Firebase</h3>
            <div className="signalements-list">
              {previewData.map((sig, index) => (
                <div key={sig.id || index} className="signalement-card">
                  <div className="signalement-header">
                    <span className="signalement-id">#{sig.id?.substring(0, 8) || index + 1}</span>
                    {getStatusBadge(sig.status)}
                  </div>
                  <div className="signalement-body">
                    <div className="info-row">
                      <span className="label">🏷️ Problème:</span>
                      <span>{sig.problemeNom || sig.problemeId || 'Non défini'}</span>
                    </div>
                    <div className="info-row">
                      <span className="label">📍 Position:</span>
                      <span>{sig.latitude?.toFixed(5)}, {sig.longitude?.toFixed(5)}</span>
                    </div>
                    <div className="info-row">
                      <span className="label">👤 Utilisateur:</span>
                      <span>{sig.userEmail || 'Anonyme'}</span>
                    </div>
                    {sig.surface && (
                      <div className="info-row">
                        <span className="label">📐 Surface:</span>
                        <span>{sig.surface} m²</span>
                      </div>
                    )}
                    {sig.description && (
                      <div className="info-row description">
                        <span className="label">📝 Description:</span>
                        <span>{sig.description}</span>
                      </div>
                    )}
                    <div className="info-row">
                      <span className="label">📅 Date:</span>
                      <span>{formatDate(sig.dateCreation)}</span>
                    </div>
                  </div>
                </div>
              ))}
              {previewData.length === 0 && (
                <div className="empty-message">
                  Aucun signalement trouvé dans Firebase
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'resultat' && syncResult && (
          <div className="result-section">
            <div className={`result-banner ${syncResult.success ? 'success' : 'error'}`}>
              {syncResult.success ? '✅' : '❌'} {syncResult.message}
            </div>
            
            <div className="result-stats">
              <div className="result-stat">
                <span className="result-number">{syncResult.totalFirebase || 0}</span>
                <span className="result-label">Total Firebase</span>
              </div>
              <div className="result-stat new">
                <span className="result-number">{syncResult.nouveaux || 0}</span>
                <span className="result-label">Nouveaux</span>
              </div>
              <div className="result-stat updated">
                <span className="result-number">{syncResult.misAJour || 0}</span>
                <span className="result-label">Mis à jour</span>
              </div>
              <div className="result-stat ignored">
                <span className="result-number">{syncResult.ignores || 0}</span>
                <span className="result-label">Ignorés</span>
              </div>
              <div className="result-stat error">
                <span className="result-number">{syncResult.erreurs || 0}</span>
                <span className="result-label">Erreurs</span>
              </div>
            </div>

            {syncResult.erreursDetails && syncResult.erreursDetails.length > 0 && (
              <div className="errors-details">
                <h4>⚠️ Détails des erreurs:</h4>
                <ul>
                  {syncResult.erreursDetails.map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="sync-timestamp">
              Synchronisé le: {formatDate(syncResult.dateSynchronisation)}
            </div>
          </div>
        )}

        {activeTab === 'apercu' && !previewData && !previewLoading && (
          <div className="empty-tab">
            <p>Cliquez sur "Aperçu Firebase" pour voir les signalements disponibles</p>
          </div>
        )}

        {activeTab === 'resultat' && !syncResult && (
          <div className="empty-tab">
            <p>Aucune synchronisation effectuée. Cliquez sur "Synchroniser maintenant"</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default SyncPage;
