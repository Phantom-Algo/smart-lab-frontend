import { fileURLToPath } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const webglDir = fileURLToPath(new URL('../../3D_Lab_Simulation_Environment', import.meta.url))

const mimeMap: Record<string, string> = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.wasm': 'application/wasm',
  '.data': 'application/octet-stream',
  '.css': 'text/css',
  '.ico': 'image/x-icon',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.json': 'application/json',
}

export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'serve-webgl-raw',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const url = req.url || ''
          if (!url.startsWith('/3D_Lab/') && url !== '/3D_Lab') {
            return next()
          }

          let filePath = path.join(webglDir, url)
          if (url === '/3D_Lab' || url === '/3D_Lab/') {
            filePath = path.join(webglDir, '3D_Lab', 'index.html')
          }

          if (!fs.existsSync(filePath)) {
            return next()
          }

          const ext = path.extname(filePath)
          res.setHeader('Content-Type', mimeMap[ext] || 'application/octet-stream')
          res.setHeader('Cross-Origin-Opener-Policy', 'same-origin')
          res.setHeader('Cross-Origin-Embedder-Policy', 'require-corp')
          const content = fs.readFileSync(filePath)
          res.end(content)
        })
      },
    },
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        changeOrigin: true,
        ws: true,
      }
    }
  }
})
