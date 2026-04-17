import request from '@/request';
import type { PageResult } from '@/types';
import type {
  FormOptionItem,
  FormPageConfig,
  RemoteOptionsRequest,
  SourceFieldItem,
  SourcePageItem,
  TableQueryPayload,
} from '@/types/formConfig';

export async function fetchRuntimeFormConfig(routePath: string) {
  const res = await request.get('/form-config/runtime', { params: { routePath } });
  return res.data as FormPageConfig;
}

export async function fetchFormConfigPages() {
  const res = await request.get('/form-config/pages');
  return res.data as FormPageConfig[];
}

export async function fetchFormConfigPage(pageKey: string) {
  const res = await request.get(`/form-config/page/${pageKey}`);
  return res.data as FormPageConfig;
}

export async function createFormConfigPage(config: FormPageConfig) {
  const res = await request.post('/form-config/page', config);
  return res.data as FormPageConfig;
}

export async function updateFormConfigPage(pageKey: string, config: FormPageConfig) {
  const res = await request.put(`/form-config/page/${pageKey}`, config);
  return res.data as FormPageConfig;
}

export async function queryConfiguredTable<T = Record<string, unknown>>(payload: TableQueryPayload) {
  const res = await request.post('/form-config/query', payload);
  return res.data as PageResult<T>;
}

export async function fetchSourcePages() {
  const res = await request.get('/form-config/source-pages');
  return res.data as SourcePageItem[];
}

export async function fetchSourceFields(pageKey: string) {
  const res = await request.get('/form-config/source-fields', { params: { pageKey } });
  return res.data as SourceFieldItem[];
}

export async function fetchDictOptions(dictType: string) {
  const res = await request.get('/dict/type', { params: { type: dictType } });
  return (res.data || []).map((item: Record<string, unknown>) => ({
    label: String(item.dictLabel ?? item.label ?? item.value ?? ''),
    value: (item.dictValue ?? item.value ?? item.dictLabel ?? '') as string | number | boolean,
    raw: item,
  })) as FormOptionItem[];
}

export async function fetchRemoteApiOptions(payload: RemoteOptionsRequest) {
  const res = await request.post('/form-config/remote-options/api', payload);
  return res.data as FormOptionItem[];
}

export async function fetchRemoteFormOptions(payload: RemoteOptionsRequest) {
  const res = await request.post('/form-config/remote-options/form', payload);
  return res.data as FormOptionItem[];
}
