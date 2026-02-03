// frontend/src/components/ProtectedRoute.jsx
import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { isAuthenticated, verifySession, logout } from '../services/authService';

/**
 * Composant pour protéger les routes Manager
 * Redirige vers /manager/login si non authentifié
 */
const ProtectedRoute = ({ children }) => {
  const location = useLocation();
  const [isChecking, setIsChecking] = useState(true);
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    const checkAuth = async () => {
      // Vérification rapide locale
      if (!isAuthenticated()) {
        setIsValid(false);
        setIsChecking(false);
        return;
      }

      // Vérification serveur de la session
      try {
        const valid = await verifySession();
        if (!valid) {
          // Session expirée ou invalide, déconnecter
          await logout();
        }
        setIsValid(valid);
      } catch (error) {
        console.error('Erreur de vérification:', error);
        setIsValid(false);
      } finally {
        setIsChecking(false);
      }
    };

    checkAuth();
  }, [location.pathname]);

  // Afficher un loader pendant la vérification
  if (isChecking) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        color: 'white',
        fontSize: '1.2rem'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '3rem', marginBottom: '20px' }}>🔐</div>
          <div>Vérification de la session...</div>
        </div>
      </div>
    );
  }

  // Rediriger vers login si non authentifié
  if (!isValid) {
    return <Navigate to="/manager/login" state={{ from: location }} replace />;
  }

  // Afficher le contenu protégé
  return children;
};

export default ProtectedRoute;
