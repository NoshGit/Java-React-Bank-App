// Bootstrap's CSS must be the first stylesheet evaluated so index.css/App.css --
// which contain this app's real color/component overrides -- always win the
// cascade regardless of JS module import order (App.tsx's own transitive
// ./App.css import would otherwise load before this one, since ES modules
// evaluate depth-first in import order and `import { App }` below appeared
// before this line originally, silently burying every override).
import 'bootstrap/dist/css/bootstrap.min.css';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import { ThemeProvider } from './theme/ThemeContext';
import { AccountsProvider } from './state/AccountsContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <ThemeProvider>
        <AccountsProvider>
          <App />
        </AccountsProvider>
      </ThemeProvider>
    </AuthProvider>
  </React.StrictMode>
);