import initialManifestPages from 'virtual:form-config-manifest';
import type { FormConfigManifestPage } from '@/types/formConfig';

let manifestPagesCache: FormConfigManifestPage[] = [...initialManifestPages];

export function listFormConfigManifestPages(): FormConfigManifestPage[] {
  return [...manifestPagesCache];
}

export async function reloadFormConfigManifestPages(): Promise<FormConfigManifestPage[]> {
  if (!import.meta.env.DEV) {
    manifestPagesCache = [...initialManifestPages];
    return listFormConfigManifestPages();
  }
  const manifestModule = await import(
    /* @vite-ignore */ `virtual:form-config-manifest?ts=${Date.now()}`
  );
  manifestPagesCache = Array.isArray(manifestModule.default) ? [...manifestModule.default] : [];
  return listFormConfigManifestPages();
}

export function findFormConfigManifestPage(routePath?: string | null, componentPath?: string | null) {
  const manifestPages = manifestPagesCache;
  if (routePath) {
    const matchedByRoute = manifestPages.find((page) => page.routePath === routePath);
    if (matchedByRoute) {
      return matchedByRoute;
    }
  }
  if (componentPath) {
    return manifestPages.find((page) => page.componentPath === componentPath) || null;
  }
  return null;
}

export function findFormConfigManifestPageByKey(pageKey?: string | null) {
  if (!pageKey) {
    return null;
  }
  return manifestPagesCache.find((page) => page.pageKey === pageKey) || null;
}
