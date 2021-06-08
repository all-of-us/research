const winston = require('winston');
const { createLogger, format } = winston;

const timeNow = () => {
  return new Date().toLocaleString('en-US', {
    timeZone: 'America/New_York',
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// Log to Console.
const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: format.combine(
    format.prettyPrint(),
    format.splat(),
    format.timestamp({ format: timeNow }),
    format.printf((info) => {
      return `${info.level.toUpperCase()}: [${info.timestamp}] - ${info.message}`;
    })
  ),
  transports: [new winston.transports.Console({ handleExceptions: true })],
  exitOnError: false
});

module.exports = { logger };
