// frontend/src/index.js
import React, { useState } from 'react';
import ReactDOM from 'react-dom/client';
import MapView from './MapView';
import BlockedUsersPage from './components/BlockedUsersPage';
import './index.css';

// Application principale avec navigation simple
const App = () => {
  const [currentPage, setCurrentPage] = useState('map');

  // Navigation vers la page Manager
  const goToBlockedUsers = () => setCurrentPage('blocked-users');
  const goToMap = () => setCurrentPage('map');

  return (
    <>
      {currentPage === 'map' && <MapView onManagerClick={goToBlockedUsers} />}
      {currentPage === 'blocked-users' && <BlockedUsersPage onBack={goToMap} />}
    </>
  );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
