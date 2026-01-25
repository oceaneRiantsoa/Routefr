// frontend/src/index.js
import React, { useState } from 'react';
import ReactDOM from 'react-dom/client';
import MapView from './MapView';
import BlockedUsersPage from './components/BlockedUsersPage';
import SignalementsPage from './components/SignalementsPage';
import './index.css';

// Application principale avec navigation simple
const App = () => {
  const [currentPage, setCurrentPage] = useState('map');

  // Navigation
  const goToBlockedUsers = () => setCurrentPage('blocked-users');
  const goToSignalements = () => setCurrentPage('signalements');
  const goToMap = () => setCurrentPage('map');

  return (
    <>
      {currentPage === 'map' && (
        <MapView 
          onManagerClick={goToBlockedUsers} 
          onSignalementsClick={goToSignalements}
        />
      )}
      {currentPage === 'blocked-users' && <BlockedUsersPage onBack={goToMap} />}
      {currentPage === 'signalements' && <SignalementsPage onBack={goToMap} />}
    </>
  );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
