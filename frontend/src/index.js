import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import * as serviceWorkerRegistration from './serviceWorkerRegistration';
import './i18n'; // i18nextの初期化

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);

// WorkBox (PWA) を有効化します
// これにより、src/service-worker.js が読み込まれます
serviceWorkerRegistration.register();
