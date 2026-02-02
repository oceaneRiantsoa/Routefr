// frontend/src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MapView from './MapView';
import ManagerPage from './components/ManagerPage';
import BlockedUsersPage from './components/BlockedUsersPage';
import SignalementsPage from './components/SignalementsPage';
import SyncPage from './components/SyncPage';
import './index.css';

// Application principale avec React Router
const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Page publique - Carte des signalements */}
        <Route path="/" element={<MapView />} />
        
        {/* Pages Manager */}
        <Route path="/manager" element={<ManagerPage />} />
        <Route path="/manager/signalements" element={<SignalementsPage />} />
        <Route path="/manager/users" element={<BlockedUsersPage />} />
        <Route path="/manager/sync" element={<SyncPage />} />
        
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
