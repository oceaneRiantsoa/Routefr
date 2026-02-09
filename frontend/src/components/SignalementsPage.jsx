// frontend/src/components/SignalementsPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import SignalementDetailModal from './SignalementDetailModal';
import './SignalementsPage.css';

const API_BASE_URL = 'http://localhost:8086';

// Mapping des statuts (idStatut -> infos)
const STATUTS = {
  10: { code: 'EN_ATTENTE', libelle: 'En attente', color: '#f39c12', icon: '' },
  20: { code: 'EN_COURS', libelle: 'En cours', color: '#3498db', icon: '' },
  30: { code: 'TRAITE', libelle: 'Traité', color: '#27ae60', icon: '' },
  40: { code: 'REJETE', libelle: 'Rejeté', color: '#e74c3c', icon: '' }
};

// Mapping des avancements
const AVANCEMENTS = {
  0: { statut: 'nouveau', libelle: 'Nouveau', color: '#f39c12', icon: '' },
  50: { statut: 'en_cours', libelle: 'En cours', color: '#3498db', icon: '' },
  100: { statut: 'termine', libelle: 'Terminé', color: '#27ae60', icon: '' }
};

// Formatage des nombres
const formatNumber = (num) => {
  if (num === null || num === undefined) return '-';
  return new Intl.NumberFormat('fr-FR').format(num);
};

// Formatage des dates
const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Couleurs des statuts
const getStatusInfo = (idStatut) => {
  return STATUTS[idStatut] || STATUTS[10];
};

// Obtenir l'info d'avancement
const getAvancementInfo = (pourcentage) => {
  return AVANCEMENTS[pourcentage] || AVANCEMENTS[0];
};

const SignalementsPage = () => {
  const navigate = useNavigate();
  const [signalements, setSignalements] = useState([]);
  const [filteredSignalements, setFilteredSignalements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedFilter, setSelectedFilter] = useState('TOUS');
  const [selectedSignalement, setSelectedSignalement] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [statistiques, setStatistiques] = useState({});
  const [entreprises, setEntreprises] = useState([]);
  const [successMessage, setSuccessMessage] = useState('');
  const [updatingAvancement, setUpdatingAvancement] = useState(null); // ID du signalement en cours de màj

  // Charger les données
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [signalementsRes, statsRes, entreprisesRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/api/manager/signalements`),
        axios.get(`${API_BASE_URL}/api/manager/signalements/statistiques`),
        axios.get(`${API_BASE_URL}/api/manager/signalements/entreprises`)
      ]);
      setSignalements(signalementsRes.data);
      setFilteredSignalements(signalementsRes.data);
      setStatistiques(statsRes.data);
      setEntreprises(entreprisesRes.data);
    } catch (err) {
      console.error('Erreur chargement:', err);
      setError('Erreur lors du chargement des signalements');
    } finally {
      setLoading(false);
    }
  };

  // Filtrer par statut
  useEffect(() => {
    if (selectedFilter === 'TOUS') {
      setFilteredSignalements(signalements);
    } else {
      const idStatut = Object.keys(STATUTS).find(key => STATUTS[key].code === selectedFilter);
      setFilteredSignalements(
        signalements.filter(s => s.idStatut === parseInt(idStatut))
      );
    }
  }, [selectedFilter, signalements]);

  // Ouvrir le modal de modification
  const handleEdit = (signalement) => {
    setSelectedSignalement(signalement);
    setShowModal(true);
  };

  // Mettre à jour l'avancement d'un signalement
  const handleUpdateAvancement = async (signalementId, newStatut, pourcentage) => {
    if (updatingAvancement) return; // Éviter les doubles clics

    setUpdatingAvancement(signalementId);
    try {
      await axios.put(`${API_BASE_URL}/api/manager/signalements/${signalementId}/avancement`, {
        statut: newStatut,
        pourcentage: pourcentage
      });
      setSuccessMessage(`Avancement mis à jour: ${pourcentage}% (${newStatut})`);
      setTimeout(() => setSuccessMessage(''), 3000);
      loadData(); // Recharger les données
    } catch (err) {
      console.error('Erreur mise à jour avancement:', err);
      setError('Erreur lors de la mise à jour de l\'avancement');
      setTimeout(() => setError(null), 3000);
    } finally {
      setUpdatingAvancement(null);
    }
  };

  // Sauvegarder les modifications
  const handleSave = async (id, updateData) => {
    try {
      await axios.put(`${API_BASE_URL}/api/manager/signalements/${id}`, updateData);
      setShowModal(false);
      setSuccessMessage('Signalement mis à jour avec succès !');
      setTimeout(() => setSuccessMessage(''), 3000);
      loadData(); // Recharger les données
    } catch (err) {
      console.error('Erreur mise à jour:', err);
      throw new Error('Erreur lors de la mise à jour');
    }
  };

  // Fermer le modal
  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedSignalement(null);
  };

  // Calculer le total
  const totalSignalements = signalements.length;

  return (
    <div className="signalements-page">
      {/* Stats header */}
      <div className="page-top-bar">
        <span className="total-badge">
          Total: {totalSignalements} signalement{totalSignalements > 1 ? 's' : ''}
        </span>
        <button className="refresh-btn" onClick={loadData}>
          Actualiser
        </button>
      </div>

      {/* Message de succès */}
      {successMessage && (
        <div className="success-message">
          {successMessage}
        </div>
      )}

      {/* Statistiques */}
      <div className="stats-container">
        <div className="stat-card stat-attente">
          <div className="stat-icon"></div>
          <div className="stat-info">
            <span className="stat-value">{statistiques.EN_ATTENTE || 0}</span>
            <span className="stat-label">En attente</span>
          </div>
        </div>
        <div className="stat-card stat-encours">
          <div className="stat-icon"></div>
          <div className="stat-info">
            <span className="stat-value">{statistiques.EN_COURS || 0}</span>
            <span className="stat-label">En cours</span>
          </div>
        </div>
        <div className="stat-card stat-traite">
          <div className="stat-icon"></div>
          <div className="stat-info">
            <span className="stat-value">{statistiques.TRAITE || 0}</span>
            <span className="stat-label">Traités</span>
          </div>
        </div>
        <div className="stat-card stat-rejete">
          <div className="stat-icon"></div>
          <div className="stat-info">
            <span className="stat-value">{statistiques.REJETE || 0}</span>
            <span className="stat-label">Rejetés</span>
          </div>
        </div>
      </div>

      {/* Filtres */}
      <div className="filters-container">
        <span className="filter-label">Filtrer par statut :</span>
        <div className="filter-buttons">
          {['TOUS', 'EN_ATTENTE', 'EN_COURS', 'TRAITE', 'REJETE'].map(filter => {
            const statutInfo = filter !== 'TOUS' ? STATUTS[Object.keys(STATUTS).find(k => STATUTS[k].code === filter)] : null;
            return (
              <button
                key={filter}
                className={`filter-btn ${selectedFilter === filter ? 'active' : ''}`}
                onClick={() => setSelectedFilter(filter)}
                style={selectedFilter === filter && statutInfo ?
                  { backgroundColor: statutInfo.color, borderColor: statutInfo.color } :
                  {}
                }
              >
                {filter === 'TOUS' ? 'Tous' : statutInfo?.libelle}
              </button>
            );
          })}
        </div>
      </div>

      {/* Contenu principal */}
      <main className="signalements-content">
        {loading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>Chargement des signalements...</p>
          </div>
        ) : error ? (
          <div className="error-container">
            <p>{error}</p>
            <button onClick={loadData} className="retry-btn">
              Réessayer
            </button>
          </div>
        ) : filteredSignalements.length === 0 ? (
          <div className="empty-container">
            <p>Aucun signalement {selectedFilter !== 'TOUS' ? 'avec ce statut' : ''}</p>
          </div>
        ) : (
          <div className="signalements-list">
            {filteredSignalements.map(signalement => {
              const statusInfo = getStatusInfo(signalement.idStatut);
              const avancement = signalement.avancementPourcentage ?? 0;
              const avancementInfo = getAvancementInfo(avancement);
              return (
                <div key={signalement.id} className="signalement-card">
                  <div className="signalement-header">
                    <span className="signalement-id">#{signalement.id}</span>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: avancementInfo.color }}
                    >
                      {avancementInfo.icon} {avancement}%
                    </span>
                  </div>

                  {/* Barre de progression */}
                  <div className="avancement-section">
                    <div className="progress-bar-container">
                      <div
                        className="progress-bar-fill"
                        style={{
                          width: `${avancement}%`,
                          backgroundColor: avancementInfo.color
                        }}
                      />
                      <span className="progress-text">{avancement}% - {avancementInfo.libelle}</span>
                    </div>

                    {/* Boutons d'avancement */}
                    <div className="avancement-buttons">
                      <button
                        className={`avancement-btn nouveau ${avancement === 0 ? 'active' : ''}`}
                        onClick={() => handleUpdateAvancement(signalement.id, 'nouveau', 0)}
                        disabled={updatingAvancement === signalement.id || avancement === 0}
                        title="Nouveau (0%)"
                      >
                        0%
                      </button>
                      <button
                        className={`avancement-btn en-cours ${avancement === 50 ? 'active' : ''}`}
                        onClick={() => handleUpdateAvancement(signalement.id, 'en_cours', 50)}
                        disabled={updatingAvancement === signalement.id || avancement === 50}
                        title="En cours (50%)"
                      >
                        50%
                      </button>
                      <button
                        className={`avancement-btn termine ${avancement === 100 ? 'active' : ''}`}
                        onClick={() => handleUpdateAvancement(signalement.id, 'termine', 100)}
                        disabled={updatingAvancement === signalement.id || avancement === 100}
                        title="Terminé (100%)"
                      >
                        100%
                      </button>
                    </div>

                    {/* Dates d'avancement */}
                    <div className="avancement-dates">
                      <span className="date-item">
                        Créé: {formatDate(signalement.dateCreationFirebase || signalement.dateSignalement)}
                      </span>
                      {signalement.dateDebutTravaux && (
                        <span className="date-item">
                          Début: {formatDate(signalement.dateDebutTravaux)}
                        </span>
                      )}
                      {signalement.dateFinTravaux && (
                        <span className="date-item">
                          Fin: {formatDate(signalement.dateFinTravaux)}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="signalement-body">
                    <div className="signalement-info">
                      <div className="info-row">
                        <span className="info-label">Localisation:</span>
                        <span className="info-value">
                          {signalement.latitude?.toFixed(5)}, {signalement.longitude?.toFixed(5)}
                        </span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">Problème:</span>
                        <span className="info-value">{signalement.probleme || signalement.problemeNom || '-'}</span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">Date:</span>
                        <span className="info-value">{formatDate(signalement.dateSignalement)}</span>
                      </div>
                    </div>

                    <div className="signalement-details">
                      <div className="detail-item">
                        <span className="detail-label">Surface</span>
                        <span className="detail-value">{formatNumber(signalement.surface)} m²</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Budget estimé</span>
                        <span className="detail-value">
                          {signalement.budgetEstime ? `${formatNumber(signalement.budgetEstime)} Ar` : `${formatNumber(signalement.budgetCalcule)} Ar (calculé)`}
                        </span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Entreprise</span>
                        <span className="detail-value">
                          {signalement.entrepriseNom || '-'}
                        </span>
                      </div>
                    </div>

                    {signalement.commentaires && (
                      <div className="signalement-comment">
                        <span className="comment-label">Commentaire:</span>
                        <span className="comment-text">{signalement.commentaires}</span>
                      </div>
                    )}
                  </div>

                  <div className="signalement-footer">
                    {signalement.dateModification && (
                      <span className="last-modified">
                        Modifié le {formatDate(signalement.dateModification)}
                      </span>
                    )}
                    <button
                      className="edit-btn"
                      onClick={() => handleEdit(signalement)}
                    >
                      Modifier
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Modal de modification */}
      {showModal && selectedSignalement && (
        <SignalementDetailModal
          signalement={selectedSignalement}
          entreprises={entreprises}
          onSave={handleSave}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
};

export default SignalementsPage;
