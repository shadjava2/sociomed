import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['/assets/senat-logo.png', '/vite.svg'],
      manifest: {
        name: 'Prise en charge médicale — Sénat',
        short_name: 'PEC Sénat',
        description: 'Application gouvernementale de prise en charge médicale',
        theme_color: '#800020',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait-primary',
        scope: '/',
        start_url: '/',
        id: '/',
        icons: [
          { src: '/assets/senat-logo.png', sizes: '192x192', type: 'image/png', purpose: 'any maskable' },
          { src: '/assets/senat-logo.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
        ],
        categories: ['government', 'medical', 'productivity'],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        runtimeCaching: [
          { urlPattern: /^https:\/\/.*\/api\/.*/i, handler: 'NetworkFirst', options: { networkTimeoutSeconds: 10, cacheName: 'api-cache', expiration: { maxEntries: 50, maxAgeSeconds: 300 } } },
        ],
      },
      devOptions: { enabled: true },
    }),
  ],
  server: {
    port: 5173,
    host: true,
  },
  optimizeDeps: {
    exclude: ['lucide-react'],
    include: ['react', 'react-dom', 'react-router-dom', 'axios'],
  },
  build: {
    target: 'es2020',
    minify: 'esbuild',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          ui: ['lucide-react'],
        },
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
    chunkSizeWarningLimit: 600,
  },
});
