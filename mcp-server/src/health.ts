import http from 'http';
import { config } from './config/index.js';
import { logger } from './utils/logger.js';

/**
 * Starts a lightweight HTTP server for health checking.
 * This is useful when the MCP Server runs in stdio transport mode, 
 * but a Docker container or orchestrator still needs a way to verify it is alive.
 */
export function startHealthCheckServer(): http.Server {
  const server = http.createServer((req, res) => {
    if (req.url === '/health' && req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'UP', transport: config.transport }));
      return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  });

  const port = config.port;
  
  server.listen(port, () => {
    logger.info(`Health check server listening on port ${port}`);
  });

  server.on('error', (err) => {
    logger.error('Health check server encountered an error', { error: err });
  });

  return server;
}
