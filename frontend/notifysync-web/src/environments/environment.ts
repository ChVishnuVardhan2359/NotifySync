// Production build. The dashboard and API are served on separate domains,
// so the API base URL is absolute (and CORS on the API must allow this origin).
export const environment = {
  production: true,
  apiUrl: 'https://fs-notify.api.platform.kb.snovasys.com/api',
  hubUrl: 'https://fs-notify.api.platform.kb.snovasys.com/hubs/notifications',
};
