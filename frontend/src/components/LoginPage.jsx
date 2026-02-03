// frontend/src/components/LoginPage.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../services/authService';
import './LoginPage.css';

const LoginPage = () => {
  const navigate = useNavigate();
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    displayName: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (isRegisterMode) {
        // Mode inscription
        const result = await register(formData.email, formData.password, formData.displayName);
        if (result.success) {
          setSuccess('Compte créé avec succès ! Vous pouvez maintenant vous connecter.');
          setIsRegisterMode(false);
          setFormData(prev => ({ ...prev, password: '' }));
        } else {
          setError(result.message);
        }
      } else {
        // Mode connexion
        const result = await login(formData.email, formData.password);
        if (result.success) {
          navigate('/manager');
        } else {
          setError(result.message);
        }
      }
    } catch (err) {
      setError('Une erreur est survenue. Veuillez réessayer.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>🛣️ Route Signalement</h1>
          <h2>{isRegisterMode ? 'Créer un compte Manager' : 'Connexion Manager'}</h2>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {isRegisterMode && (
            <div className="form-group">
              <label htmlFor="displayName">Nom complet</label>
              <input
                type="text"
                id="displayName"
                name="displayName"
                value={formData.displayName}
                onChange={handleChange}
                placeholder="Votre nom"
                required={isRegisterMode}
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="manager@routefr.com"
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mot de passe</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              required
              minLength={6}
              autoComplete={isRegisterMode ? 'new-password' : 'current-password'}
            />
          </div>

          {error && (
            <div className="alert alert-error">
              ⚠️ {error}
            </div>
          )}

          {success && (
            <div className="alert alert-success">
              ✅ {success}
            </div>
          )}

          <button 
            type="submit" 
            className="login-button"
            disabled={loading}
          >
            {loading ? (
              <span className="loading-spinner">⏳ Chargement...</span>
            ) : (
              isRegisterMode ? '📝 Créer le compte' : '🔐 Se connecter'
            )}
          </button>
        </form>

        <div className="login-footer">
          <button 
            type="button"
            className="toggle-mode-button"
            onClick={() => {
              setIsRegisterMode(!isRegisterMode);
              setError(null);
              setSuccess(null);
            }}
          >
            {isRegisterMode 
              ? '← Retour à la connexion' 
              : 'Créer un compte Manager →'}
          </button>

          <button 
            type="button"
            className="back-home-button"
            onClick={() => navigate('/')}
          >
            🏠 Retour à la carte
          </button>
        </div>

        <div className="login-info">
          <p><strong>Compte par défaut :</strong></p>
          <p>📧 manager@routefr.com</p>
          <p>🔑 Manager123!</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
