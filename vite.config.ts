import { defineConfig, loadEnv, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import type { IncomingMessage, ServerResponse } from 'node:http';

const ALLOWED_API_ROUTES = new Set([
  'health',
  'analyze-soil',
  'identify-crop',
  'plant-health',
  'npk-extract',
  'npk-regional',
  'advisor',
]);

function readJsonBody(req: IncomingMessage): Promise<unknown> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    let totalBytes = 0;
    req.on('data', (chunk: Buffer) => {
      totalBytes += chunk.length;
      if (totalBytes > 6 * 1024 * 1024) {
        reject(new Error('Payload too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8');
      if (!raw.trim()) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch {
        resolve(raw);
      }
    });
    req.on('error', reject);
  });
}

function localApiDevPlugin(): Plugin {
  return {
    name: 'farmerbuddy-local-api',
    configureServer(server) {
      const env = loadEnv(server.config.mode, process.cwd(), '');
      if (env.GEMINI_API_KEY && !process.env.GEMINI_API_KEY) {
        process.env.GEMINI_API_KEY = env.GEMINI_API_KEY;
      }

      server.middlewares.use('/api', async (req, res, next) => {
        const pathname = (req.url || '').split('?')[0].replace(/^\/+/, '');
        if (!ALLOWED_API_ROUTES.has(pathname)) {
          next();
          return;
        }

        const sendJson = (status: number, payload: unknown) => {
          if (res.writableEnded) return;
          res.statusCode = status;
          res.setHeader('Content-Type', 'application/json; charset=utf-8');
          res.end(JSON.stringify(payload));
        };

        try {
          const body = await readJsonBody(req);
          const mod = (await server.ssrLoadModule(`/api/${pathname}.ts`)) as {
            default?: (vReq: unknown, vRes: unknown) => Promise<unknown> | unknown;
          };
          if (typeof mod.default !== 'function') {
            sendJson(500, { error: `Handler /api/${pathname} is missing a default export.` });
            return;
          }

          interface DevApiResponse extends ServerResponse {
            status: (code: number) => DevApiResponse;
            json: (payload: unknown) => DevApiResponse;
          }
          const vercelReq = Object.assign(req, { body });
          const vercelRes: DevApiResponse = Object.assign(res, {
            status(code: number): DevApiResponse {
              res.statusCode = code;
              return vercelRes;
            },
            json(payload: unknown): DevApiResponse {
              if (!res.writableEnded) {
                res.setHeader('Content-Type', 'application/json; charset=utf-8');
                res.end(JSON.stringify(payload));
              }
              return vercelRes;
            },
          });

          await mod.default(vercelReq, vercelRes);
        } catch (err) {
          const msg = err instanceof Error ? err.message : 'Local API error';
          sendJson(msg === 'Payload too large' ? 413 : 500, { error: msg });
        }
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), localApiDevPlugin()],
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
