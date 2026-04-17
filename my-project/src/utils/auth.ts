export interface StoredUser {
  token?: string | null;
  role?: string;
  [key: string]: unknown;
}

const USER_STORAGE_KEY = 'user';

const canUseStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';

export const clearAuthStorage = () => {
  if (!canUseStorage()) {
    return;
  }

  localStorage.removeItem(USER_STORAGE_KEY);
  localStorage.removeItem('isLoggedIn');
  localStorage.removeItem('token');
};

export const getStoredUser = (): StoredUser | null => {
  if (!canUseStorage()) {
    return null;
  }

  const rawUser = localStorage.getItem(USER_STORAGE_KEY);
  if (!rawUser) {
    return null;
  }

  try {
    const parsedUser = JSON.parse(rawUser) as StoredUser | null;
    if (!parsedUser || typeof parsedUser !== 'object') {
      clearAuthStorage();
      return null;
    }
    return parsedUser;
  } catch {
    clearAuthStorage();
    return null;
  }
};

export const hasValidToken = (user: StoredUser | null = getStoredUser()) =>
  Boolean(user && typeof user.token === 'string' && user.token.trim());

export const getLoginRedirectPath = () => {
  if (typeof window === 'undefined') {
    return '/login';
  }

  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (window.location.pathname === '/login') {
    return '/login';
  }

  return `/login?redirect=${encodeURIComponent(currentPath)}`;
};
