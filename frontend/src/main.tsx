import { createRoot } from 'react-dom/client';
import { CssBaseline } from '@mui/material';
import './index.css';
import './i18n/config';
import App from './app/App';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found');
}

createRoot(rootElement).render(
  <>
    <CssBaseline />
    <App />
  </>
);
