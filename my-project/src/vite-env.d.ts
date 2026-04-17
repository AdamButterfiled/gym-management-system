/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VUE_APP_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'virtual:form-config-manifest' {
  import type { FormConfigManifestPage } from '@/types/formConfig';

  const manifest: FormConfigManifestPage[];
  export default manifest;
}
