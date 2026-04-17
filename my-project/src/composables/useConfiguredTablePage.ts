import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import type { PageResult } from '@/types';
import { fetchRuntimeFormConfig, queryConfiguredTable } from '@/api/formConfig';
import type { FilterLogic, FormConfigTarget, FormFieldConfig, FormFilterRule, FormPageConfig } from '@/types/formConfig';
import {
  buildConfiguredColumns,
  cloneRules,
  compileFilterRules,
  isConfiguredFieldVisible,
  resolveFilterLogic,
  textSuggestionsByField,
} from '@/utils/formConfig';
import { findFormConfigManifestPage } from '@/utils/formConfigManifest';
import {
  getQuickSearchConfig,
  normalizePageConfig,
  resolvePrimaryFormTarget,
  resolveTableTarget,
  resolveTarget,
} from '@/utils/formConfigDesigner';

type RuleSource =
  | FormFilterRule[]
  | { value: FormFilterRule[] }
  | (() => FormFilterRule[]);

interface UseConfiguredTablePageOptions<T> {
  routePath: string;
  pageSize?: number;
  transformRecords?: (records: T[]) => T[];
  fixedRules?: RuleSource;
  tableTargetKey?: string;
  primaryFormTargetKey?: string;
}

export function useConfiguredTablePage<T extends Record<string, unknown>>(options: UseConfiguredTablePageOptions<T>) {
  const runtimeConfig = ref<FormPageConfig | null>(null);
  const keyword = ref('');
  const filterLogic = ref<FilterLogic>('AND');
  const filterRules = ref<FormFilterRule[]>([]);
  const filterModalVisible = ref(false);
  const tableData = ref<T[]>([]);
  const loading = ref(false);
  const configLoading = ref(false);

  const pagination = reactive({
    current: 1,
    pageSize: options.pageSize || 10,
    total: 0,
    showSizeChanger: false,
    showQuickJumper: false,
    showTotal: (total: number) => `共 ${Number(total || 0).toLocaleString('zh-CN')} 项`,
  });

  const tableTarget = computed<FormConfigTarget | null>(() => resolveTableTarget(runtimeConfig.value, options.tableTargetKey));
  const primaryFormTarget = computed<FormConfigTarget | null>(() =>
    resolvePrimaryFormTarget(runtimeConfig.value, options.primaryFormTargetKey)
  );
  const tableFields = computed<FormFieldConfig[]>(() => tableTarget.value?.fields || []);
  const fields = computed<FormFieldConfig[]>(() => tableFields.value.length ? tableFields.value : runtimeConfig.value?.fields || []);
  const primaryFormFields = computed<FormFieldConfig[]>(() => primaryFormTarget.value?.fields || []);
  const filterableFields = computed(() => fields.value.filter((field) => field.filterEnabled));
  const quickSearchConfig = computed(() => getQuickSearchConfig(runtimeConfig.value));
  const quickSearchEnabled = computed(() => (quickSearchConfig.value.fields?.length || 0) > 0);
  const formInputFollowSystemRadius = computed(() => Boolean(runtimeConfig.value?.formInputFollowSystemRadius));
  const quickSearchPlaceholder = computed(() => quickSearchConfig.value.placeholder || '请输入关键词');
  const activeFilterCount = computed(() => compileFilterRules(filterRules.value, fields.value).length);
  const textSuggestions = computed(() => textSuggestionsByField(tableData.value, fields.value));
  const visibleFieldKeys = computed(() =>
    new Set(fields.value.filter((field) => isConfiguredFieldVisible(fields.value, field.fieldKey)).map((field) => field.fieldKey))
  );
  const fixedRules = computed<FormFilterRule[]>(() => {
    const source = options.fixedRules;
    if (!source) {
      return [];
    }
    if (typeof source === 'function') {
      return cloneRules(source() || []);
    }
    if (Array.isArray(source)) {
      return cloneRules(source);
    }
    return cloneRules(source.value || []);
  });

  async function ensureConfig() {
    if (runtimeConfig.value) {
      return runtimeConfig.value;
    }
    configLoading.value = true;
    try {
      const config = await fetchRuntimeFormConfig(options.routePath);
      const manifest = findFormConfigManifestPage(options.routePath, config.menuBinding?.componentPath);
      runtimeConfig.value = normalizePageConfig(config, manifest);
      filterLogic.value = resolveFilterLogic(runtimeConfig.value);
      return runtimeConfig.value;
    } finally {
      configLoading.value = false;
    }
  }

  async function loadData() {
    const config = await ensureConfig();
    loading.value = true;
    try {
      const page = await queryConfiguredTable<T>({
        pageKey: config.pageKey,
        routePath: config.routePath,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: keyword.value.trim() || undefined,
        filterLogic: filterLogic.value,
        filterRules: [
          ...compileFilterRules(fixedRules.value, fields.value),
          ...compileFilterRules(filterRules.value, fields.value),
        ],
      });
      const records = options.transformRecords ? options.transformRecords(page.records) : page.records;
      tableData.value = records;
      pagination.total = page.total;
      return page as PageResult<T>;
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    pagination.current = 1;
    return loadData();
  }

  function handleReset() {
    keyword.value = '';
    filterRules.value = [];
    filterLogic.value = resolveFilterLogic(runtimeConfig.value);
    pagination.current = 1;
    return loadData();
  }

  function handleTableChange(pag: { current: number; pageSize: number }) {
    pagination.current = pag.current;
    pagination.pageSize = pag.pageSize;
    return loadData();
  }

  function applyAdvancedFilters(payload: { logic: FilterLogic; rules: FormFilterRule[] }) {
    filterLogic.value = payload.logic;
    filterRules.value = cloneRules(payload.rules);
    filterModalVisible.value = false;
    pagination.current = 1;
    return loadData();
  }

  function buildColumns<C extends Record<string, unknown>>(columns: C[]) {
    return buildConfiguredColumns(columns, tableFields.value);
  }

  function isFieldVisible(fieldKey: string, fallback = false) {
    return visibleFieldKeys.value.has(fieldKey) || (!visibleFieldKeys.value.size && fallback);
  }

  function isAnyFieldVisible(fieldKeys: string[], fallback = false) {
    return fieldKeys.some((fieldKey) => isFieldVisible(fieldKey, fallback));
  }

  function getTargetFields(targetKey?: string) {
    const target = resolveTarget(runtimeConfig.value, targetKey);
    return target?.fields || [];
  }

  function isTargetFieldVisible(targetKey: string, fieldKey: string, fallback = false) {
    const targetFields = getTargetFields(targetKey);
    if (!targetFields.length) {
      return fallback;
    }
    return isConfiguredFieldVisible(targetFields, fieldKey, fallback);
  }

  function isAnyTargetFieldVisible(targetKey: string, fieldKeys: string[], fallback = false) {
    return fieldKeys.some((fieldKey) => isTargetFieldVisible(targetKey, fieldKey, fallback));
  }

  function isPrimaryFormFieldVisible(fieldKey: string, fallback = false) {
    if (!primaryFormFields.value.length) {
      return fallback;
    }
    return isConfiguredFieldVisible(primaryFormFields.value, fieldKey, fallback);
  }

  function isAnyPrimaryFormFieldVisible(fieldKeys: string[], fallback = false) {
    return fieldKeys.some((fieldKey) => isPrimaryFormFieldVisible(fieldKey, fallback));
  }

  watch(formInputFollowSystemRadius, (linked) => {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.classList.toggle('page-form-input-radius-linked', linked);
  }, { immediate: true });

  onBeforeUnmount(() => {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.classList.remove('page-form-input-radius-linked');
  });

  return {
    runtimeConfig,
    tableTarget,
    primaryFormTarget,
    tableFields,
    fields,
    primaryFormFields,
    filterableFields,
    quickSearchEnabled,
    quickSearchConfig,
    formInputFollowSystemRadius,
    quickSearchPlaceholder,
    isFieldVisible,
    isAnyFieldVisible,
    getTargetFields,
    isTargetFieldVisible,
    isAnyTargetFieldVisible,
    isPrimaryFormFieldVisible,
    isAnyPrimaryFormFieldVisible,
    keyword,
    filterLogic,
    filterRules,
    filterModalVisible,
    activeFilterCount,
    tableData,
    pagination,
    loading,
    configLoading,
    textSuggestions,
    ensureConfig,
    loadData,
    handleSearch,
    handleReset,
    handleTableChange,
    applyAdvancedFilters,
    buildColumns,
  };
}
