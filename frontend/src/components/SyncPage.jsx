// frontend/src/components/SyncPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './SyncPage.css';

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8086';

const SyncPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [pushLoading, setPushLoading] = useState(false);
  const [pushPreviewLoading, setPushPreviewLoading] = useState(false);
  const [syncResult, setSyncResult] = useState(null);
  const [pushResult, setPushResult] = useState(null);
  const [previewData, setPreviewData] = useState(null);
  const [pushPreviewData, setPushPreviewData] = useState(null);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState(null);
  const [activeTab, setActiveTab] = useState('apercu');
  const [activeSection, setActiveSection] = useState('pull');

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

  const handlePreview = async () => {
    setPreviewLoading(true);
    setError(null);
    setPreviewData(null);
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/preview');
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Erreur serveur: ' + response.status);
      }
      if (Array.isArray(data)) {
        setPreviewData(data);
        setActiveTab('apercu');
      } else if (data.error) {
        throw new Error(data.error);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleSync = async () => {
    if (!window.confirm('Voulez-vous synchroniser les signalements depuis Firebase?')) {
      return;
    }
    setLoading(true);
    setError(null);
    setSyncResult(null);
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/pull', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        throw new Error('Erreur serveur: ' + response.status);
      }
      const data = await response.json();
      setSyncResult(data);
      setActiveTab('resultat');
      loadStats();
    } catch (err) {
      setError('Erreur lors de la synchronisation: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePushPreview = async () => {
    setPushPreviewLoading(true);
    setError(null);
    setPushPreviewData(null);
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/push/preview');
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Erreur serveur: ' + response.status);
      }
      if (Array.isArray(data)) {
        setPushPreviewData(data);
        setActiveTab('push-apercu');
      } else if (data.error) {
        throw new Error(data.error);
      }
    } catch (err) {
      setError('Erreur apercu push: ' + err.message);
    } finally {
      setPushPreviewLoading(false);
    }
  };

  const handlePush = async () => {
    if (!window.confirm('Voulez-vous envoyer tous les signalements vers Firebase?')) {
      return;
    }
    setPushLoading(true);
    setError(null);
    setPushResult(null);
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/push', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        throw new Error('Erreur serveur: ' + response.status);
      }
      const data = await response.json();
      setPushResult(data);
      setActiveTab('push-resultat');
      loadStats();
    } catch (err) {
      setError('Erreur envoi Firebase: ' + err.message);
    } finally {
      setPushLoading(false);
    }
  };

  const handlePushSingle = async (signalementId) => {
    if (!window.confirm('Envoyer le signalement ' + signalementId + ' vers Firebase?')) {
      return;
    }
    try {
      const response = await fetch(BACKEND_URL + '/api/manager/sync/push/' + signalementId, {
        method: 'POST'
      });
      const data = await response.json();
      if (data.success) {
        alert('Signalement envoye avec succes!');
        handlePushPreview();
      } else {
        alert('Erreur: ' + data.message);
      }
    } catch (err) {
      alert('Erreur: ' + err.message);
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

  const getStatusBadge = (status) => {
    const statusMap = {
      'nouveau': { label: 'Nouveau', className: 'status-nouveau' },
      'en_cours': { label: 'En cours', className: 'status-en-cours' },
      'traite': { label: 'Traite', className: 'status-traite' },
      'rejete': { label: 'Rejete', className: 'status-rejete' }
    };
    const info = statusMap[status] || { label: status || 'Inconnu', className: 'status-unknown' };
    return <span className={'status-badge ' + info.className}>{info.label}</span>;
  };

  const getSourceBadge = (source) => {
    if (source === 'local') {
      return <span className="source-badge source-local">Local</span>;
    }
    return <span className="source-badge source-firebase">Firebase</span>;
  };

  return (
    <div className="sync-page">
      <div className="sync-header">
        <button className="back-button" onClick={() => navigate('/manager')}>
          Retour
        </button>
        <h1>Synchronisation Firebase</h1>
        <p>Synchronisation bidirectionnelle avec Firebase</p>
      </div>

      <div className="section-tabs">
        <button 
          className={'section-tab ' + (activeSection === 'pull' ? 'active' : '')}
          onClick={() => { setActiveSection('pull'); setActiveTab('apercu'); }}
        >
          Recuperer (Pull)
        </button>
        <button 
          className={'section-tab ' + (activeSection === 'push' ? 'active' : '')}
          onClick={() => { setActiveSection('push'); setActiveTab('push-apercu'); }}
        >
          Envoyer (Push)
        </button>
      </div>

      {stats && (
        <div className="stats-section">
          <h3>Statistiques locales</h3>
          <div className="stats-cards">
            <div className="stat-card">
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

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {activeSection === 'pull' && (
        <>
          <div className="section-header">
            <h2>Recuperer depuis Firebase</h2>
            <p>Telecharger les signalements de application mobile vers la base locale</p>
          </div>

          <div className="action-buttons">
            <button 
              className="preview-button"
              onClick={handlePreview}
              disabled={previewLoading}
            >
              {previewLoading ? 'Chargement...' : 'Apercu Firebase'}
            </button>
            
            <button 
              className="sync-button"
              onClick={handleSync}
              disabled={loading}
            >
              {loading ? 'Synchronisation...' : 'Synchroniser maintenant'}
            </button>
          </div>

          <div className="tabs">
            <button 
              className={'tab ' + (activeTab === 'apercu' ? 'active' : '')}
              onClick={() => setActiveTab('apercu')}
            >
              Apercu Firebase
            </button>
            <button 
              className={'tab ' + (activeTab === 'resultat' ? 'active' : '')}
              onClick={() => setActiveTab('resultat')}
            >
              Resultat synchronisation
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'apercu' && previewData && (
              <div className="preview-section">
                <h3>{previewData.length} signalement(s) dans Firebase</h3>
                <div className="signalements-list">
                  {previewData.map((sig, index) => (
                    <div key={sig.id || index} className="signalement-card">
                      <div className="signalement-header">
                        <span className="signalement-id">#{sig.id ? sig.id.substring(0, 8) : index + 1}</span>
                        {getStatusBadge(sig.status)}
                      </div>
                      <div className="signalement-body">
                        <div className="info-row">
                          <span className="label">Probleme:</span>
                          <span>{sig.problemeNom || sig.problemeId || 'Non defini'}</span>
                        </div>
                        <div className="info-row">
                          <span className="label">Position:</span>
                          <span>{sig.latitude ? sig.latitude.toFixed(5) : 'N/A'}, {sig.longitude ? sig.longitude.toFixed(5) : 'N/A'}</span>
                        </div>
                        <div className="info-row">
                          <span className="label">Utilisateur:</span>
                          <span>{sig.userEmail || 'Anonyme'}</span>
                        </div>
                        {sig.surface && (
                          <div className="info-row">
                            <span className="label">Surface:</span>
                            <span>{sig.surface} m2</span>
                          </div>
                        )}
                        {sig.description && (
                          <div className="info-row description">
                            <span className="label">Description:</span>
                            <span>{sig.description}</span>
                          </div>
                        )}
                        <div className="info-row">
                          <span className="label">Date:</span>
                          <span>{formatDate(sig.dateCreation)}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                  {previewData.length === 0 && (
                    <div className="empty-message">
                      Aucun signalement trouve dans Firebase
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'resultat' && syncResult && (
              <div className="result-section">
                <div className={'result-banner ' + (syncResult.success ? 'success' : 'error')}>
                  {syncResult.success ? 'Succes' : 'Erreur'} - {syncResult.message}
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
                    <span className="result-label">Mis a jour</span>
                  </div>
                  <div className="result-stat ignored">
                    <span className="result-number">{syncResult.ignores || 0}</span>
                    <span className="result-label">Ignores</span>
                  </div>
                  <div className="result-stat error">
                    <span className="result-number">{syncResult.erreurs || 0}</span>
                    <span className="result-label">Erreurs</span>
                  </div>
                </div>

                {syncResult.erreursDetails && syncResult.erreursDetails.length > 0 && (
                  <div className="errors-details">
                    <h4>Details des erreurs:</h4>
                    <ul>
                      {syncResult.erreursDetails.map((err, i) => (
                        <li key={i}>{err}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="sync-timestamp">
                  Synchronise le: {formatDate(syncResult.dateSynchronisation)}
                </div>
              </div>
            )}

            {activeTab === 'apercu' && !previewData && !previewLoading && (
              <div className="empty-tab">
                <p>Cliquez sur Apercu Firebase pour voir les signalements disponibles</p>
              </div>
            )}

            {activeTab === 'resultat' && !syncResult && (
              <div className="empty-tab">
                <p>Aucune synchronisation effectuee. Cliquez sur Synchroniser maintenant</p>
              </div>
            )}
          </div>
        </>
      )}

      {activeSection === 'push' && (
        <>
          <div className="section-header">
            <h2>Envoyer vers Firebase</h2>
            <p>Publier les signalements pour affichage sur application mobile</p>
          </div>

          <div className="action-buttons">
            <button 
              className="preview-button"
              onClick={handlePushPreview}
              disabled={pushPreviewLoading}
            >
              {pushPreviewLoading ? 'Chargement...' : 'Apercu a envoyer'}
            </button>
            
            <button 
              className="push-button"
              onClick={handlePush}
              disabled={pushLoading}
            >
              {pushLoading ? 'Envoi...' : 'Envoyer vers Firebase'}
            </button>
          </div>

          <div className="tabs">
            <button 
              className={'tab ' + (activeTab === 'push-apercu' ? 'active' : '')}
              onClick={() => setActiveTab('push-apercu')}
            >
              Apercu a envoyer
            </button>
            <button 
              className={'tab ' + (activeTab === 'push-resultat' ? 'active' : '')}
              onClick={() => setActiveTab('push-resultat')}
            >
              Resultat envoi
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'push-apercu' && pushPreviewData && (
              <div className="preview-section">
                <h3>{pushPreviewData.length} signalement(s) a envoyer</h3>
                <div className="signalements-list">
                  {pushPreviewData.map((sig, index) => (
                    <div key={sig.id || index} className="signalement-card">
                      <div className="signalement-header">
                        <span className="signalement-id">#{sig.id ? sig.id.substring(0, 8) : (sig.localId || index + 1)}</span>
                        {getStatusBadge(sig.status)}
                        {getSourceBadge(sig.source)}
                      </div>
                      <div className="signalement-body">
                        <div className="info-row">
                          <span className="label">Probleme:</span>
                          <span>{sig.problemeNom || sig.problemeId || 'Non defini'}</span>
                        </div>
                        <div className="info-row">
                          <span className="label">Position:</span>
                          <span>{sig.latitude ? sig.latitude.toFixed(5) : 'N/A'}, {sig.longitude ? sig.longitude.toFixed(5) : 'N/A'}</span>
                        </div>
                        {sig.entrepriseNom && (
                          <div className="info-row">
                            <span className="label">Entreprise:</span>
                            <span>{sig.entrepriseNom}</span>
                          </div>
                        )}
                        {sig.surface && (
                          <div className="info-row">
                            <span className="label">Surface:</span>
                            <span>{sig.surface} m2</span>
                          </div>
                        )}
                        {sig.budget && (
                          <div className="info-row">
                            <span className="label">Budget:</span>
                            <span>{sig.budget} EUR</span>
                          </div>
                        )}
                        {sig.description && (
                          <div className="info-row description">
                            <span className="label">Description:</span>
                            <span>{sig.description}</span>
                          </div>
                        )}
                        <div className="info-row">
                          <span className="label">Mise a jour:</span>
                          <span>{formatDate(sig.dateMiseAJour)}</span>
                        </div>
                      </div>
                      <div className="signalement-actions">
                        <button 
                          className="push-single-button"
                          onClick={() => handlePushSingle(sig.id)}
                          title="Envoyer ce signalement"
                        >
                          Envoyer
                        </button>
                      </div>
                    </div>
                  ))}
                  {pushPreviewData.length === 0 && (
                    <div className="empty-message">
                      Aucun signalement a envoyer
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'push-resultat' && pushResult && (
              <div className="result-section">
                <div className={'result-banner ' + (pushResult.success ? 'success' : 'error')}>
                  {pushResult.success ? 'Succes' : 'Erreur'} - {pushResult.message}
                </div>
                
                <div className="result-stats">
                  <div className="result-stat">
                    <span className="result-number">{pushResult.totalEnvoyes || 0}</span>
                    <span className="result-label">Total envoyes</span>
                  </div>
                  <div className="result-stat new">
                    <span className="result-number">{pushResult.nouveaux || 0}</span>
                    <span className="result-label">Nouveaux</span>
                  </div>
                  <div className="result-stat updated">
                    <span className="result-number">{pushResult.misAJour || 0}</span>
                    <span className="result-label">Mis a jour</span>
                  </div>
                  <div className="result-stat error">
                    <span className="result-number">{pushResult.erreurs || 0}</span>
                    <span className="result-label">Erreurs</span>
                  </div>
                </div>

                {pushResult.erreursDetails && pushResult.erreursDetails.length > 0 && (
                  <div className="errors-details">
                    <h4>Details des erreurs:</h4>
                    <ul>
                      {pushResult.erreursDetails.map((err, i) => (
                        <li key={i}>{err}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {pushResult.signalementsEnvoyes && pushResult.signalementsEnvoyes.length > 0 && (
                  <div className="sent-list">
                    <h4>Signalements envoyes:</h4>
                    <ul>
                      {pushResult.signalementsEnvoyes.map((id, i) => (
                        <li key={i}>{id}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="sync-timestamp">
                  Envoye le: {formatDate(pushResult.datePush)}
                </div>
              </div>
            )}

            {activeTab === 'push-apercu' && !pushPreviewData && !pushPreviewLoading && (
              <div className="empty-tab">
                <p>Cliquez sur Apercu a envoyer pour voir les signalements a publier</p>
              </div>
            )}

            {activeTab === 'push-resultat' && !pushResult && (
              <div className="empty-tab">
                <p>Aucun envoi effectue. Cliquez sur Envoyer vers Firebase</p>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default SyncPage;
