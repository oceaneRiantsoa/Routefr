// frontend/src/services/authService.js
/**
 * Service d'authentification pour le Manager
 * Utilise l'API REST /api/auth pour la connexion/déconnexion
 */

const API_BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8086';

// Clés de stockage
const SESSION_TOKEN_KEY = 'manager_session_token';
const USER_INFO_KEY = 'manager_user_info';

/**
 * Connexion utilisateur
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise<{success: boolean, message: string, user?: object}>}
 */
export const login = async (email, password) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      // Stocker le token de session et les infos utilisateur
      localStorage.setItem(SESSION_TOKEN_KEY, data.sessionToken);
      localStorage.setItem(USER_INFO_KEY, JSON.stringify({
        uid: data.uid,
        email: data.email,
        displayName: data.displayName || email.split('@')[0],
        role: data.role || 'MANAGER'
      }));

      return {
        success: true,
        message: 'Connexion réussie',
        user: {
          uid: data.uid,
          email: data.email,
          sessionToken: data.sessionToken
        }
      };
    } else {
      return {
        success: false,
        message: data.message || 'Identifiants incorrects'
      };
    }
  } catch (error) {
    console.error('Erreur de connexion:', error);
    return {
      success: false,
      message: 'Erreur de connexion au serveur'
    };
  }
};

/**
 * Inscription d'un nouvel utilisateur (pour créer le compte manager)
 * @param {string} email 
 * @param {string} password 
 * @param {string} displayName 
 * @returns {Promise<{success: boolean, message: string}>}
 */
export const register = async (email, password, displayName) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password, displayName }),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      return {
        success: true,
        message: 'Compte créé avec succès',
        user: data.user
      };
    } else {
      return {
        success: false,
        message: data.message || 'Erreur lors de la création du compte'
      };
    }
  } catch (error) {
    console.error('Erreur d\'inscription:', error);
    return {
      success: false,
      message: 'Erreur de connexion au serveur'
    };
  }
};

/**
 * Déconnexion
 */
export const logout = async () => {
  try {
    const sessionToken = getSessionToken();
    
    if (sessionToken) {
      await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        headers: {
          'Session-Token': sessionToken,
        },
      });
    }
  } catch (error) {
    console.error('Erreur lors de la déconnexion:', error);
  } finally {
    // Toujours supprimer les données locales
    localStorage.removeItem(SESSION_TOKEN_KEY);
    localStorage.removeItem(USER_INFO_KEY);
  }
};

/**
 * Vérifier si la session est valide
 * @returns {Promise<boolean>}
 */
export const verifySession = async () => {
  try {
    const sessionToken = getSessionToken();
    
    if (!sessionToken) {
      return false;
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/verify-session`, {
      method: 'POST',
      headers: {
        'Session-Token': sessionToken,
      },
    });

    const data = await response.json();
    return response.ok && data.success;
  } catch (error) {
    console.error('Erreur de vérification de session:', error);
    return false;
  }
};

/**
 * Prolonger la session
 * @returns {Promise<boolean>}
 */
export const refreshSession = async () => {
  try {
    const sessionToken = getSessionToken();
    
    if (!sessionToken) {
      return false;
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/refresh-session`, {
      method: 'POST',
      headers: {
        'Session-Token': sessionToken,
      },
    });

    return response.ok;
  } catch (error) {
    console.error('Erreur de rafraîchissement de session:', error);
    return false;
  }
};

/**
 * Récupérer le token de session stocké
 * @returns {string|null}
 */
export const getSessionToken = () => {
  return localStorage.getItem(SESSION_TOKEN_KEY);
};

/**
 * Récupérer les informations de l'utilisateur connecté
 * @returns {object|null}
 */
export const getCurrentUser = () => {
  const userInfo = localStorage.getItem(USER_INFO_KEY);
  return userInfo ? JSON.parse(userInfo) : null;
};

/**
 * Vérifier si l'utilisateur est connecté (vérification locale)
 * @returns {boolean}
 */
export const isAuthenticated = () => {
  return !!getSessionToken();
};

export default {
  login,
  register,
  logout,
  verifySession,
  refreshSession,
  getSessionToken,
  getCurrentUser,
  isAuthenticated
};
