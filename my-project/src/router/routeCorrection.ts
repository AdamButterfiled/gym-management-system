import type { Router } from 'vue-router';

const STATIC_ROUTE_PATHS = ['/login', '/404', '/dashboard'];
const DYNAMIC_ROUTE_PATTERN = /[:*]/;

export const normalizeRoutePath = (path: string) => {
  if (!path) {
    return '/';
  }

  const [pathname] = path.split(/[?#]/);
  if (!pathname) {
    return '/';
  }

  const normalized = pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname;
  return normalized.startsWith('/') ? normalized : `/${normalized}`;
};

const toComparableKey = (path: string) =>
  normalizeRoutePath(path)
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

const levenshteinDistance = (source: string, target: string) => {
  if (source === target) {
    return 0;
  }

  const rows = source.length + 1;
  const cols = target.length + 1;
  const matrix = Array.from({ length: rows }, () => Array(cols).fill(0));

  for (let row = 0; row < rows; row += 1) {
    matrix[row][0] = row;
  }

  for (let col = 0; col < cols; col += 1) {
    matrix[0][col] = col;
  }

  for (let row = 1; row < rows; row += 1) {
    for (let col = 1; col < cols; col += 1) {
      const cost = source[row - 1] === target[col - 1] ? 0 : 1;
      matrix[row][col] = Math.min(
        matrix[row - 1][col] + 1,
        matrix[row][col - 1] + 1,
        matrix[row - 1][col - 1] + cost
      );
    }
  }

  return matrix[source.length][target.length];
};

const scoreCandidate = (targetPath: string, candidatePath: string) => {
  const normalizedTarget = normalizeRoutePath(targetPath);
  const normalizedCandidate = normalizeRoutePath(candidatePath);
  const targetKey = toComparableKey(normalizedTarget);
  const candidateKey = toComparableKey(normalizedCandidate);

  if (!targetKey || !candidateKey) {
    return 0;
  }

  if (normalizedTarget === normalizedCandidate) {
    return 1;
  }

  if (targetKey === candidateKey) {
    return 0.99;
  }

  if (targetKey.startsWith(candidateKey) || candidateKey.startsWith(targetKey)) {
    const longer = Math.max(targetKey.length, candidateKey.length);
    const shorter = Math.min(targetKey.length, candidateKey.length);
    return 0.9 - (longer - shorter) / Math.max(longer * 4, 1);
  }

  const distance = levenshteinDistance(targetKey, candidateKey);
  return 1 - distance / Math.max(targetKey.length, candidateKey.length);
};

const shouldKeepCandidate = (path: string) => {
  if (!path || path === '/') {
    return false;
  }

  return !DYNAMIC_ROUTE_PATTERN.test(path);
};

export const collectRouteCandidates = (router: Router, menuList: Array<{ path?: string }> = []) => {
  const candidates = new Set<string>();

  const addCandidate = (path?: string) => {
    if (!path || !shouldKeepCandidate(path)) {
      return;
    }
    candidates.add(normalizeRoutePath(path));
  };

  STATIC_ROUTE_PATHS.forEach(addCandidate);
  menuList.forEach(menu => addCandidate(menu.path));
  router.getRoutes().forEach(route => addCandidate(route.path));

  return Array.from(candidates);
};

export const getSmartRedirectTarget = (path: string, candidates: string[]) => {
  const normalizedTarget = normalizeRoutePath(path);
  let bestMatch = '';
  let bestScore = 0;

  candidates.forEach(candidate => {
    const score = scoreCandidate(normalizedTarget, candidate);
    if (score > bestScore) {
      bestScore = score;
      bestMatch = candidate;
    }
  });

  if (!bestMatch || bestMatch === normalizedTarget) {
    return null;
  }

  return bestScore >= 0.72 ? bestMatch : null;
};

export const isRouteFallback = (route: { name: unknown; matched: Array<{ name: unknown }> }) =>
  route.name === 'RouteFallback' || route.matched.some(record => record.name === 'RouteFallback');
