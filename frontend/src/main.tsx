import { StrictMode, Suspense } from 'react';
import { createRoot } from 'react-dom/client';
import { ErrorBoundary } from './components/ErrorBoundary';
import { SkeletonLayout } from './components/SkeletonLayout';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <Suspense fallback={<SkeletonLayout />}>
        <App />
      </Suspense>
    </ErrorBoundary>
  </StrictMode>
);
