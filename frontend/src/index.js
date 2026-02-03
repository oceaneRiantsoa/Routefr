// frontend/src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MapView from './MapView';
import ManagerPage from './components/ManagerPage';
import BlockedUsersPage from './components/BlockedUsersPage';
import SignalementsPage from './components/SignalementsPage';
import SyncPage from './components/SyncPage';
import LoginPage from './components/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import './index.css';

// Application principale avec React Router
const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Page publique - Carte des signalements */}
        <Route path="/" element={<MapView />} />
        
        {/* Page de connexion Manager */}
        <Route path="/manager/login" element={<LoginPage />} />
        
        {/* Pages Manager protégées */}
        <Route path="/manager" element={
          <ProtectedRoute>
            <ManagerPage />
          </ProtectedRoute>
        } />
        <Route path="/manager/signalements" element={
          <ProtectedRoute>
            <SignalementsPage />
          </ProtectedRoute>
        } />
        <Route path="/manager/users" element={
          <ProtectedRoute>
            <BlockedUsersPage />
          </ProtectedRoute>
        } />
        <Route path="/manager/sync" element={
          <ProtectedRoute>
            <SyncPage />
          </ProtectedRoute>
        } />
        
        {/* Redirection par défaut */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
