// frontend/src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import MapView from './MapView';
import './index.css';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <MapView />
  </React.StrictMode>
);
