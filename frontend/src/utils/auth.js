const TOKEN_KEY = 'bus_token';
const USER_ID_KEY = 'bus_user_id';
const USER_NAME_KEY = 'bus_user_name';
const USER_ROLE_KEY = 'bus_user_role';
const AUTH_EXPIRY_KEY = 'bus_auth_expiry';
const AUTH_TIMEOUT_MS = 10 * 60 * 1000;

let authTimeoutId = null;

function getActiveStorage() {
  if (localStorage.getItem(TOKEN_KEY)) {
    return localStorage;
  }

  if (sessionStorage.getItem(TOKEN_KEY)) {
    return sessionStorage;
  }

  return localStorage;
}

function clearAuthTimer() {
  if (authTimeoutId) {
    window.clearTimeout(authTimeoutId);
    authTimeoutId = null;
  }
}

function scheduleAuthExpiry(expiryAt) {
  clearAuthTimer();

  const delay = expiryAt - Date.now();
  if (delay <= 0) {
    clearAuth();
    return;
  }

  authTimeoutId = window.setTimeout(() => {
    clearAuth();
  }, delay);
}

export function saveAuth(authData, options = {}) {
  const { rememberMe = false } = options;
  const storage = rememberMe ? localStorage : sessionStorage;

  clearAuth();

  storage.setItem(TOKEN_KEY, authData.token);
  storage.setItem(USER_ID_KEY, String(authData.userId));
  storage.setItem(USER_NAME_KEY, authData.name || '');
  storage.setItem(USER_ROLE_KEY, authData.role || 'USER');

  if (rememberMe) {
    const expiryAt = Date.now() + AUTH_TIMEOUT_MS;
    storage.setItem(AUTH_EXPIRY_KEY, String(expiryAt));
    scheduleAuthExpiry(expiryAt);
  } else {
    storage.removeItem(AUTH_EXPIRY_KEY);
  }
}

export function clearAuth() {
  clearAuthTimer();
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(USER_NAME_KEY);
  localStorage.removeItem(USER_ROLE_KEY);
  localStorage.removeItem(AUTH_EXPIRY_KEY);

  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_ID_KEY);
  sessionStorage.removeItem(USER_NAME_KEY);
  sessionStorage.removeItem(USER_ROLE_KEY);
  sessionStorage.removeItem(AUTH_EXPIRY_KEY);
}

export function getToken() {
  const storage = getActiveStorage();
  const token = storage.getItem(TOKEN_KEY);

  if (!token) {
    return null;
  }

  const expiryValue = storage.getItem(AUTH_EXPIRY_KEY);
  if (expiryValue) {
    const expiryAt = Number(expiryValue);
    if (Number.isFinite(expiryAt) && Date.now() >= expiryAt) {
      clearAuth();
      return null;
    }

    if (storage === localStorage) {
      scheduleAuthExpiry(expiryAt);
    }
  }

  return token;
}

export function getUserId() {
  const storage = getActiveStorage();
  const value = storage.getItem(USER_ID_KEY);
  return value || null;
}

export function getUserName() {
  const storage = getActiveStorage();
  return storage.getItem(USER_NAME_KEY) || '';
}

export function getUserRole() {
  const storage = getActiveStorage();
  return storage.getItem(USER_ROLE_KEY) || 'USER';
}

export function isAdminRole(role = getUserRole()) {
  const normalizedRole = String(role || '').toUpperCase();
  return normalizedRole === 'ADMIN' || normalizedRole === 'ROLE_ADMIN';
}

export function isAuthenticated() {
  return Boolean(getToken());
}
