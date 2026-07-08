import { config } from '../config/index.js';

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const levels: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

const currentLevel = levels[config.logLevel as LogLevel] || levels.info;

function formatMessage(level: LogLevel, message: string, meta?: any) {
  const timestamp = new Date().toISOString();
  const metaString = meta ? ` | ${JSON.stringify(meta)}` : '';
  return `[${timestamp}] [${level.toUpperCase()}] ${message}${metaString}`;
}

export const logger = {
  debug: (message: string, meta?: any) => {
    if (levels.debug >= currentLevel) {
      console.debug(formatMessage('debug', message, meta));
    }
  },
  info: (message: string, meta?: any) => {
    if (levels.info >= currentLevel) {
      console.info(formatMessage('info', message, meta));
    }
  },
  warn: (message: string, meta?: any) => {
    if (levels.warn >= currentLevel) {
      console.warn(formatMessage('warn', message, meta));
    }
  },
  error: (message: string, meta?: any) => {
    if (levels.error >= currentLevel) {
      console.error(formatMessage('error', message, meta));
    }
  },
};
