// frontend/src/components/ManagerPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './ManagerPage.css';

const API_BASE_URL = 'http://localhost:8086';

const ManagerPage = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const [statsRes, signalementsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/api/manager/signalements/statistiques`),
        axios.get(`${API_BASE_URL}/api/manager/signalements`)
      ]);

      const apiStats = statsRes.data;
      const mappedStats = {
        total: (apiStats.EN_ATTENTE || 0) + (apiStats.EN_COURS || 0) + (apiStats.TRAITE || 0) + (apiStats.REJETE || 0),
        enAttente: apiStats.EN_ATTENTE || 0,
        enCours: apiStats.EN_COURS || 0,
        traites: apiStats.TRAITE || 0,
        rejetes: apiStats.REJETE || 0,
        recentSignalements: signalementsRes.data.slice(0, 5)
      };

      setStats(mappedStats);
    } catch (err) {
      console.error('Erreur chargement dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatNumber = (num) => {
    if (num === null || num === undefined) return '0';
    return new Intl.NumberFormat('fr-FR').format(num);
  };

  const getStatusLabel = (idStatut) => {
    switch(idStatut) {
      case 30: return 'Traité';
      case 20: return 'En cours';
      default: return 'En attente';
    }
  };

  const getStatusClass = (idStatut) => {
    switch(idStatut) {
      case 30: return 'status-success';
      case 20: return 'status-info';
      default: return 'status-warning';
    }
  };

  return (
    <div className="dashboard">
      {/* Stats Cards */}
      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-value">{loading ? '-' : formatNumber(stats?.total || 0)}</div>
          <div className="stat-label">Total signalements</div>
        </div>
        <div className="stat-card">
          <div className="stat-value text-warning">{loading ? '-' : formatNumber(stats?.enAttente || 0)}</div>
          <div className="stat-label">En attente</div>
        </div>
        <div className="stat-card">
          <div className="stat-value text-info">{loading ? '-' : formatNumber(stats?.enCours || 0)}</div>
          <div className="stat-label">En cours</div>
        </div>
        <div className="stat-card">
          <div className="stat-value text-success">{loading ? '-' : formatNumber(stats?.traites || 0)}</div>
          <div className="stat-label">Traités</div>
        </div>
      </div>

      {/* Quick Links */}
      <div className="card">
        <div className="card-header">
          <h2>Accès rapide</h2>
        </div>
        <div className="card-body">
          <div className="quick-links">
            <button className="quick-link" onClick={() => navigate('/manager/sync')}>
              Synchronisation
            </button>
            <button className="quick-link" onClick={() => navigate('/manager/signalements')}>
              Signalements
            </button>
            <button className="quick-link" onClick={() => navigate('/manager/statistiques')}>
              Statistiques
            </button>
            <button className="quick-link" onClick={() => navigate('/manager/users')}>
              Utilisateurs
            </button>
            <button className="quick-link quick-link-secondary" onClick={() => navigate('/')}>
              Voir la carte
            </button>
          </div>
        </div>
      </div>

      {/* Recent Activity & Progress */}
      <div className="row-2">
        <div className="card">
          <div className="card-header">
            <h2>Activité récente</h2>
            <button className="btn-link" onClick={() => navigate('/manager/signalements')}>
              Voir tout
            </button>
          </div>
          <div className="card-body">
            {loading ? (
              <p className="text-muted">Chargement...</p>
            ) : stats?.recentSignalements?.length > 0 ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Problème</th>
                    <th>Statut</th>
                    <th>Avancement</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.recentSignalements.map((sig) => (
                    <tr key={sig.id}>
                      <td>#{sig.id}</td>
                      <td>{sig.problemeNom || sig.probleme || '-'}</td>
                      <td>
                        <span className={`badge ${getStatusClass(sig.idStatut)}`}>
                          {getStatusLabel(sig.idStatut)}
                        </span>
                      </td>
                      <td>{sig.avancementPourcentage != null ? `${sig.avancementPourcentage}%` : '0%'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="text-muted">Aucun signalement récent</p>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2>Répartition</h2>
          </div>
          <div className="card-body">
            <div className="progress-list">
              <div className="progress-item">
                <div className="progress-label">
                  <span>Traités</span>
                  <span>{stats ? Math.round((stats.traites / stats.total) * 100) || 0 : 0}%</span>
                </div>
                <div className="progress-bar">
                  <div className="progress-fill bg-success" style={{ width: `${stats ? Math.round((stats.traites / stats.total) * 100) || 0 : 0}%` }}></div>
                </div>
              </div>
              <div className="progress-item">
                <div className="progress-label">
                  <span>En cours</span>
                  <span>{stats ? Math.round((stats.enCours / stats.total) * 100) || 0 : 0}%</span>
                </div>
                <div className="progress-bar">
                  <div className="progress-fill bg-info" style={{ width: `${stats ? Math.round((stats.enCours / stats.total) * 100) || 0 : 0}%` }}></div>
                </div>
              </div>
              <div className="progress-item">
                <div className="progress-label">
                  <span>En attente</span>
                  <span>{stats ? Math.round((stats.enAttente / stats.total) * 100) || 0 : 0}%</span>
                </div>
                <div className="progress-bar">
                  <div className="progress-fill bg-warning" style={{ width: `${stats ? Math.round((stats.enAttente / stats.total) * 100) || 0 : 0}%` }}></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ManagerPage;
