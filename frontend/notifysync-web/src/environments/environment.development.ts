// API runs on port 5080 on the same host the dashboard is opened from.
// This makes the app work whether you browse via localhost, 127.0.0.1, or a LAN IP.
const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  apiUrl: `http://${host}:5080/api`,
  hubUrl: `http://${host}:5080/hubs/notifications`,
};
