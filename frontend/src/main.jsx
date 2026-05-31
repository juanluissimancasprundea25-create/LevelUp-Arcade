import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider } from './lib/auth.jsx';
import './styles/index.css';

// Limpiamos el splash boot cuando React monta.
queueMicrotask(() => {
  const splash = document.querySelector('.boot-splash');
  if (splash) splash.classList.add('hidden');
  setTimeout(() => splash?.remove(), 500);
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
