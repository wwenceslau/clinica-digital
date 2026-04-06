import { createRoot } from 'react-dom/client';
import { CssBaseline } from '@mui/material';
import './index.css';

function App() {
  return <h1>Clinica Digital</h1>;
}

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
