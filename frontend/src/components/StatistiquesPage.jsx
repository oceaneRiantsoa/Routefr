// frontend/src/components/StatistiquesPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './StatistiquesPage.css';

const API_BASE_URL = 'http://localhost:8086';

const StatistiquesPage = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadStatistiques();
  }, []);

  const loadStatistiques = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`${API_BASE_URL}/api/manager/signalements/statistiques/completes`);
      setStats(response.data);
    } catch (err) {
      console.error('Erreur chargement statistiques:', err);
      setError('Erreur lors du chargement des statistiques');
    } finally {
      setLoading(false);
    }
  };

  // Formater un nombre
  const formatNumber = (num) => {
    if (num === null || num === undefined) return '0';
    return new Intl.NumberFormat('fr-FR').format(num);
  };

  // Rendu de la barre de progression pour les délais
  const renderDelaiBar = (type, delai, maxDelai) => {
    const percentage = maxDelai > 0 ? (delai / maxDelai) * 100 : 0;
    return (
      <div className="delai-item" key={type}>
        <div className="delai-info">
          <span className="delai-type">{type}</span>
          <span className="delai-value">{delai} jours</span>
        </div>
        <div className="delai-bar-container">
          <div
            className="delai-bar"
            style={{ width: `${Math.min(percentage, 100)}%` }}
          />
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="statistiques-page">
        <header className="stats-header">
          <button className="back-button" onClick={() => navigate('/manager')}>
            ← Retour au Manager
          </button>
          <h1>Statistiques</h1>
        </header>
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Chargement des statistiques...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="statistiques-page">
        <header className="stats-header">
          <button className="back-button" onClick={() => navigate('/manager')}>
            ← Retour au Manager
          </button>
          <h1>Statistiques</h1>
        </header>
        <div className="error-container">
          <p className="error-message">{error}</p>
          <button onClick={loadStatistiques} className="retry-button">
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  // Calculer le délai max pour les barres
  const delaisParType = stats?.delaiParType || {};
  const maxDelai = Math.max(...Object.values(delaisParType), 1);

  return (
    <div className="statistiques-page">
      {/* Top bar */}
      <div className="page-top-bar">
        <span className="page-subtitle">Analyse des performances et délais</span>
        <button className="refresh-btn" onClick={loadStatistiques}>
          Actualiser
        </button>
      </div>

      {/* Cartes de compteurs */}
      <section className="counters-section">
        <div className="counter-card total">
          <div className="counter-icon"></div>
          <div className="counter-info">
            <span className="counter-value">{formatNumber(stats?.nombreTotal)}</span>
            <span className="counter-label">Total Signalements</span>
          </div>
        </div>

        <div className="counter-card nouveau">
          <div className="counter-icon"></div>
          <div className="counter-info">
            <span className="counter-value">{formatNumber(stats?.nombreNouveau)}</span>
            <span className="counter-label">Nouveau (0%)</span>
            <span className="counter-percentage">{stats?.pourcentageNouveau || 0}%</span>
          </div>
          <div className="progress-ring">
            <svg viewBox="0 0 36 36">
              <path
                className="progress-ring-bg"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
              <path
                className="progress-ring-fill nouveau"
                strokeDasharray={`${stats?.pourcentageNouveau || 0}, 100`}
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
            </svg>
          </div>
        </div>

        <div className="counter-card en-cours">
          <div className="counter-icon"></div>
          <div className="counter-info">
            <span className="counter-value">{formatNumber(stats?.nombreEnCours)}</span>
            <span className="counter-label">En cours (50%)</span>
            <span className="counter-percentage">{stats?.pourcentageEnCours || 0}%</span>
          </div>
          <div className="progress-ring">
            <svg viewBox="0 0 36 36">
              <path
                className="progress-ring-bg"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
              <path
                className="progress-ring-fill en-cours"
                strokeDasharray={`${stats?.pourcentageEnCours || 0}, 100`}
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
            </svg>
          </div>
        </div>

        <div className="counter-card termine">
          <div className="counter-icon"></div>
          <div className="counter-info">
            <span className="counter-value">{formatNumber(stats?.nombreTermine)}</span>
            <span className="counter-label">Terminé (100%)</span>
            <span className="counter-percentage">{stats?.pourcentageTermine || 0}%</span>
          </div>
          <div className="progress-ring">
            <svg viewBox="0 0 36 36">
              <path
                className="progress-ring-bg"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
              <path
                className="progress-ring-fill termine"
                strokeDasharray={`${stats?.pourcentageTermine || 0}, 100`}
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
            </svg>
          </div>
        </div>
      </section>

      {/* Délais de traitement */}
      <section className="delais-section">
        <h2>Délais de traitement moyens</h2>

        <div className="delais-cards">
          <div className="delai-card">
            <div className="delai-card-icon"></div>
            <div className="delai-card-content">
              <span className="delai-card-value">{stats?.delaiMoyenTraitement || 0}</span>
              <span className="delai-card-unit">jours</span>
              <span className="delai-card-label">Délai moyen total</span>
              <span className="delai-card-desc">De la création à la fin</span>
            </div>
          </div>

          <div className="delai-card">
            <div className="delai-card-icon"></div>
            <div className="delai-card-content">
              <span className="delai-card-value">{stats?.delaiMoyenDebutTravaux || 0}</span>
              <span className="delai-card-unit">jours</span>
              <span className="delai-card-label">Délai avant travaux</span>
              <span className="delai-card-desc">De la création au début</span>
            </div>
          </div>

          <div className="delai-card">
            <div className="delai-card-icon"></div>
            <div className="delai-card-content">
              <span className="delai-card-value">{stats?.delaiMoyenFinTravaux || 0}</span>
              <span className="delai-card-unit">jours</span>
              <span className="delai-card-label">Durée des travaux</span>
              <span className="delai-card-desc">Du début à la fin</span>
            </div>
          </div>
        </div>
      </section>

      {/* Délais par type de problème */}
      <section className="delais-type-section">
        <h2>Délai de traitement par type de signalement</h2>

        {Object.keys(delaisParType).length > 0 ? (
          <div className="delais-list">
            {Object.entries(delaisParType).map(([type, delai]) =>
              renderDelaiBar(type, delai, maxDelai)
            )}
          </div>
        ) : (
          <div className="no-data">
            <p>📭 Aucune donnée de délai disponible</p>
            <p className="no-data-hint">Les délais seront calculés une fois que des signalements auront été traités</p>
          </div>
        )}
      </section>

      {/* Légende */}
      <section className="legend-section">
        <h3>Légende des avancements</h3>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-icon" style={{background: '#f39c12'}}></span>
            <span className="legend-text"><strong>Nouveau (0%)</strong> - Signalement reçu, non traité</span>
          </div>
          <div className="legend-item">
            <span className="legend-icon" style={{background: '#3498db'}}></span>
            <span className="legend-text"><strong>En cours (50%)</strong> - Travaux en cours de réalisation</span>
          </div>
          <div className="legend-item">
            <span className="legend-icon" style={{background: '#27ae60'}}></span>
            <span className="legend-text"><strong>Terminé (100%)</strong> - Travaux achevés</span>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="stats-footer">
        <p>Dernière mise à jour : {new Date().toLocaleString('fr-FR')}</p>
      </footer>
    </div>
  );
};

export default StatistiquesPage;
