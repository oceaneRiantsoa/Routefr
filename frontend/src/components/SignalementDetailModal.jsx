// frontend/src/components/SignalementDetailModal.jsx
import React, { useState } from 'react';
import './SignalementDetailModal.css';

// Formatage des nombres pour l'affichage
const formatNumber = (num) => {
  if (num === null || num === undefined) return '';
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

// Mapping des statuts
const STATUTS = [
  { id: 10, code: 'EN_ATTENTE', libelle: 'En attente', color: '#f39c12', icon: '' },
  { id: 20, code: 'EN_COURS', libelle: 'En cours', color: '#3498db', icon: '' },
  { id: 30, code: 'TRAITE', libelle: 'Traité', color: '#27ae60', icon: '' },
  { id: 40, code: 'REJETE', libelle: 'Rejeté', color: '#e74c3c', icon: '' }
];

const SignalementDetailModal = ({ signalement, entreprises = [], onSave, onClose }) => {
  const [formData, setFormData] = useState({
    surface: signalement.surface || '',
    budgetEstime: signalement.budgetEstime || '',
    idEntreprise: signalement.idEntreprise || '',
    notesManager: signalement.notesManager || '',
    idStatut: signalement.idStatut || 10,
    niveauReparation: signalement.niveauReparation || 1
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleStatutChange = (idStatut) => {
    setFormData(prev => ({
      ...prev,
      idStatut: idStatut
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      // Préparer les données avec les bons types
      const dataToSend = {
        surface: formData.surface ? parseFloat(formData.surface) : null,
        budgetEstime: formData.budgetEstime ? parseFloat(formData.budgetEstime) : null,
        idEntreprise: formData.idEntreprise ? parseInt(formData.idEntreprise) : null,
        notesManager: formData.notesManager || null,
        idStatut: parseInt(formData.idStatut),
        niveauReparation: parseInt(formData.niveauReparation) || 1
      };
      
      await onSave(signalement.id, dataToSend);
    } catch (err) {
      setError(err.message || 'Erreur lors de la sauvegarde');
    } finally {
      setSaving(false);
    }
  };

  // Calculer le budget suggéré (surface * cout_par_m2 * niveau)
  const budgetSuggere = signalement.coutParM2 && formData.surface && formData.niveauReparation
    ? parseFloat(formData.surface) * signalement.coutParM2 * parseInt(formData.niveauReparation)
    : null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Modifier Signalement #{signalement.id}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Informations en lecture seule */}
          <div className="readonly-section">
            <h3>Informations du signalement</h3>
            <div className="readonly-grid">
              <div className="readonly-item">
                <span className="readonly-label">Localisation</span>
                <span className="readonly-value">
                  {signalement.latitude?.toFixed(5)}, {signalement.longitude?.toFixed(5)}
                </span>
              </div>
              <div className="readonly-item">
                <span className="readonly-label">Problème</span>
                <span className="readonly-value">{signalement.probleme || '-'}</span>
              </div>
              <div className="readonly-item">
                <span className="readonly-label">Date du signalement</span>
                <span className="readonly-value">{formatDate(signalement.dateSignalement)}</span>
              </div>
              <div className="readonly-item">
                <span className="readonly-label">Budget calculé</span>
                <span className="readonly-value">{formatNumber(signalement.budgetCalcule)} Ar</span>
              </div>
              {signalement.commentaires && (
                <div className="readonly-item full-width">
                  <span className="readonly-label">Commentaire original</span>
                  <span className="readonly-value">{signalement.commentaires}</span>
                </div>
              )}
            </div>
            
            {/* Section Photos */}
            {signalement.photos && signalement.photos.length > 0 && (
              <div className="photos-section">
                <h4>Photos ({signalement.photos.length})</h4>
                <div className="photos-grid">
                  {signalement.photos.map((photo, index) => (
                    <div key={index} className="photo-item">
                      <img 
                        src={photo} 
                        alt={`Photo ${index + 1}`}
                        onClick={() => window.open(photo, '_blank')}
                        title="Cliquer pour agrandir"
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Champs éditables */}
          <div className="editable-section">
            <h3>Informations Manager</h3>
            
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="surface">Surface (m²)</label>
                <input
                  type="number"
                  id="surface"
                  name="surface"
                  value={formData.surface}
                  onChange={handleChange}
                  placeholder="Ex: 25.5"
                  step="0.01"
                  min="0"
                />
              </div>

              <div className="form-group">
                <label htmlFor="niveauReparation">
                  Niveau de réparation (1-10)
                  <span className="niveau-info" title="1 = réparation légère, 10 = reconstruction complète">ℹ️</span>
                </label>
                <div className="niveau-selector">
                  <input
                    type="range"
                    id="niveauReparation"
                    name="niveauReparation"
                    value={formData.niveauReparation}
                    onChange={handleChange}
                    min="1"
                    max="10"
                    step="1"
                  />
                  <span className="niveau-value">{formData.niveauReparation}</span>
                </div>
                <div className="niveau-labels">
                  <span>Léger</span>
                  <span>Sévère</span>
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="budgetEstime">
                  Budget estimé (Ar)
                  {budgetSuggere && (
                    <span className="budget-suggestion">
                      Calculé: {formatNumber(budgetSuggere)} Ar
                    </span>
                  )}
                </label>
                <input
                  type="number"
                  id="budgetEstime"
                  name="budgetEstime"
                  value={formData.budgetEstime}
                  onChange={handleChange}
                  placeholder="Ex: 500000"
                  step="1"
                  min="0"
                />
                {signalement.coutParM2 && formData.surface && (
                  <div className="budget-formula-hint">
                    Formule: {formatNumber(signalement.coutParM2)} Ar/m² × {formData.surface} m² × niveau {formData.niveauReparation}
                  </div>
                )}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="idEntreprise">Entreprise assignée</label>
              <select
                id="idEntreprise"
                name="idEntreprise"
                value={formData.idEntreprise}
                onChange={handleChange}
              >
                <option value="">-- Sélectionner une entreprise --</option>
                {entreprises.map(e => (
                  <option key={e.id} value={e.id}>
                    {e.nomEntreprise} {e.localisation ? `(${e.localisation})` : ''}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="notesManager">Notes du Manager</label>
              <textarea
                id="notesManager"
                name="notesManager"
                value={formData.notesManager}
                onChange={handleChange}
                placeholder="Ajoutez vos notes ici..."
                rows="3"
              />
            </div>

            <div className="form-group">
              <label>Statut</label>
              <div className="status-options">
                {STATUTS.map(statut => (
                  <label
                    key={statut.id}
                    className={`status-option ${formData.idStatut === statut.id ? 'selected' : ''}`}
                    style={formData.idStatut === statut.id ? 
                      { borderColor: statut.color, backgroundColor: `${statut.color}20` } : 
                      {}
                    }
                  >
                    <input
                      type="radio"
                      name="idStatut"
                      value={statut.id}
                      checked={formData.idStatut === statut.id}
                      onChange={() => handleStatutChange(statut.id)}
                    />
                    <span className="status-icon">{statut.icon}</span>
                    <span className="status-text">{statut.libelle}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* Message d'erreur */}
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          {/* Boutons d'action */}
          <div className="modal-actions">
            <button 
              type="button" 
              className="cancel-btn"
              onClick={onClose}
              disabled={saving}
            >
              Annuler
            </button>
            <button 
              type="submit" 
              className="save-btn"
              disabled={saving}
            >
              {saving ? 'Sauvegarde...' : '💾 Sauvegarder'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SignalementDetailModal;
