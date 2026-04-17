import type {
  FilterLogic,
  FormConfigManifestPage,
  FormConfigManifestTarget,
  FormConfigManifestTargetField,
  FormConfigTarget,
  FormConfigTargetType,
  FormFieldConfig,
  FormFieldLayout,
  FormPageConfig,
  FormPageMenuBinding,
  FormPageQuickSearch,
} from '@/types/formConfig';

export const MAIN_TABLE_TARGET_KEY = 'main-table';
export const MAIN_FORM_TARGET_KEY = 'main-form';

const FORM_GRID_COLUMNS = 24;
const DEFAULT_FORM_SPAN = 12;
const DEFAULT_FORM_HEIGHT = 1;
const COMPACT_FIELD_KEY_PATTERN = /(age|gender|sex|status|state|sort|order|score|rating|level|capacity|count|qty|amount|balance|salary|price|fee|hourly|stock|limit)$/i;
const WIDE_FIELD_KEY_PATTERN = /(description|remark|memo|note|content|intro|summary|detail|address|reason|message|json|config|schema|body)$/i;

export function clonePageConfig<T extends FormPageConfig>(config: T): T {
  return JSON.parse(JSON.stringify(config)) as T;
}

export function cloneTarget<T extends FormConfigTarget>(target: T): T {
  return JSON.parse(JSON.stringify(target)) as T;
}

export function cloneField<T extends FormFieldConfig>(field: T): T {
  return JSON.parse(JSON.stringify(field)) as T;
}

export function getQuickSearchConfig(config?: FormPageConfig | null): FormPageQuickSearch {
  const fields = config?.quickSearch?.fields?.length
    ? [...config.quickSearch.fields]
    : [...(config?.quickSearchFields || [])];
  return {
    enabled: config?.quickSearch?.enabled ?? fields.length > 0,
    placeholder: config?.quickSearch?.placeholder ?? config?.quickSearchPlaceholder ?? '',
    fields,
    defaultLogic: (config?.quickSearch?.defaultLogic ?? config?.defaultFilterLogic ?? 'AND') as FilterLogic,
  };
}

export function normalizePageConfig(config: FormPageConfig, manifest?: FormConfigManifestPage | null): FormPageConfig {
  const draft = clonePageConfig(config);
  draft.version = 2;
  draft.quickSearch = getQuickSearchConfig(draft);
  draft.quickSearchPlaceholder = draft.quickSearch.placeholder || '';
  draft.quickSearchFields = [...draft.quickSearch.fields];
  draft.defaultFilterLogic = draft.quickSearch.defaultLogic || 'AND';
  draft.targets = normalizeTargets(draft, manifest);
  draft.fields = toLegacyTableFields(resolveTableTarget(draft));
  draft.menuBinding = normalizeMenuBinding(draft.menuBinding, manifest);
  draft.runtimeIssues = draft.runtimeIssues || [];
  return draft;
}

export function createDraftFromManifest(input: {
  manifest?: FormConfigManifestPage | null;
  existing?: FormPageConfig | null;
  menuBinding?: FormPageMenuBinding;
  pageKey?: string;
  routePath?: string;
  pageTitle?: string;
}): FormPageConfig {
  const manifest = input.manifest || null;
  const existing = input.existing ? clonePageConfig(input.existing) : null;
  const baseConfig: FormPageConfig = existing || {
    version: 2,
    pageKey: input.pageKey || manifest?.pageKey || '',
    routePath: input.routePath || manifest?.routePath || '',
    pageTitle: input.pageTitle || manifest?.pageTitle || '',
    enabled: true,
    formInputFollowSystemRadius: false,
    quickSearch: {
      enabled: Boolean(manifest?.quickSearchDetected),
      placeholder: '',
      fields: [],
      defaultLogic: 'AND',
    },
    quickSearchPlaceholder: '',
    quickSearchFields: [],
    defaultFilterLogic: 'AND',
    fields: [],
    targets: [],
    runtimeIssues: [],
  };

  if (!baseConfig.pageKey) {
    baseConfig.pageKey = input.pageKey || manifest?.pageKey || slugFromPath(baseConfig.routePath || baseConfig.pageTitle);
  }
  if (!baseConfig.routePath) {
    baseConfig.routePath = input.routePath || manifest?.routePath || '';
  }
  if (!baseConfig.pageTitle) {
    baseConfig.pageTitle = input.pageTitle || manifest?.pageTitle || baseConfig.pageKey;
  }
  if (!baseConfig.menuBinding) {
    baseConfig.menuBinding = normalizeMenuBinding(input.menuBinding, manifest);
  } else if (input.menuBinding) {
    baseConfig.menuBinding = normalizeMenuBinding({ ...baseConfig.menuBinding, ...input.menuBinding }, manifest);
  }
  return normalizePageConfig(baseConfig, manifest);
}

export function normalizeTargets(config: FormPageConfig, manifest?: FormConfigManifestPage | null): FormConfigTarget[] {
  const manifestTargets = manifest?.targets || [];
  const savedTargets = Array.isArray(config.targets) ? config.targets : [];
  const legacyFieldMap = new Map((config.fields || []).map((field) => [field.fieldKey, cloneField(field)]));
  const targetMap = new Map(savedTargets.map((target) => [target.targetKey, cloneTarget(target)]));
  const normalized: FormConfigTarget[] = [];

  if (manifestTargets.length) {
    manifestTargets.forEach((manifestTarget, targetIndex) => {
      const saved = targetMap.get(manifestTarget.targetKey);
      normalized.push(normalizeTarget(saved, manifestTarget, legacyFieldMap, targetIndex));
    });
  } else if (savedTargets.length) {
    savedTargets.forEach((target, targetIndex) => {
      normalized.push(normalizeTarget(target, null, legacyFieldMap, targetIndex));
    });
  } else {
    normalized.push(createFallbackTableTarget(config.fields || []));
  }

  const knownTargetKeys = new Set(normalized.map((target) => target.targetKey));
  savedTargets.forEach((target, targetIndex) => {
    if (!knownTargetKeys.has(target.targetKey)) {
      normalized.push(normalizeTarget(target, null, legacyFieldMap, manifestTargets.length + targetIndex));
    }
  });

  const assignedFields = new Set(normalized.flatMap((target) => target.fields.map((field) => field.fieldKey)));
  const orphanFields = [...legacyFieldMap.values()].filter((field) => !assignedFields.has(field.fieldKey));
  if (!manifestTargets.length && orphanFields.length) {
    const tableTarget = normalized.find((target) => target.targetType === 'table') || normalized[0];
    orphanFields.forEach((field, index) => {
      tableTarget.fields.push(
        normalizeField(
          field,
          tableTarget.targetType,
          tableTarget.fields.length + index,
          undefined
        )
      );
    });
  }

  const normalizedTargets = normalized
    .map((target, index) => ({
      ...target,
      order: target.order ?? index,
      fields: sortTargetFields(target.fields, target.targetType),
    }))
    .sort((left, right) => (left.order ?? 0) - (right.order ?? 0));

  const primaryTableTarget = normalizedTargets.find((target) => target.targetType === 'table') || null;
  return normalizedTargets.map((target) => applySmartFormLayout(target, primaryTableTarget));
}

export function resolveTableTarget(config?: FormPageConfig | null, targetKey?: string) {
  if (!config?.targets?.length) {
    return null;
  }
  if (targetKey) {
    return config.targets.find((target) => target.targetKey === targetKey) || null;
  }
  return (
    config.targets.find((target) => target.targetType === 'table' && target.enabled !== false) ||
    config.targets.find((target) => target.targetType === 'table') ||
    null
  );
}

export function resolvePrimaryFormTarget(config?: FormPageConfig | null, targetKey?: string) {
  if (!config?.targets?.length) {
    return null;
  }
  if (targetKey) {
    return config.targets.find((target) => target.targetKey === targetKey) || null;
  }
  return (
    config.targets.find((target) => target.targetType === 'modal-form' && target.enabled !== false) ||
    config.targets.find((target) => target.targetType === 'form' && target.enabled !== false) ||
    config.targets.find((target) => target.targetType === 'modal-form') ||
    config.targets.find((target) => target.targetType === 'form') ||
    null
  );
}

export function resolveTarget(config?: FormPageConfig | null, targetKey?: string) {
  if (!config?.targets?.length || !targetKey) {
    return null;
  }
  return config.targets.find((target) => target.targetKey === targetKey) || null;
}

export function getTargetFields(config?: FormPageConfig | null, targetKey?: string, fallbackTargetType?: FormConfigTargetType) {
  const target =
    resolveTarget(config, targetKey) ||
    (fallbackTargetType === 'table'
      ? resolveTableTarget(config)
      : fallbackTargetType
        ? resolvePrimaryFormTarget(config, targetKey)
        : null);
  return target?.fields || [];
}

export function sortTargetFields(fields: FormFieldConfig[], targetType: FormConfigTargetType) {
  return [...fields].sort((left, right) => {
    if (targetType === 'table') {
      const leftOrder = left.order ?? left.columnOrder ?? 0;
      const rightOrder = right.order ?? right.columnOrder ?? 0;
      if (leftOrder === rightOrder) {
        return left.fieldKey.localeCompare(right.fieldKey);
      }
      return leftOrder - rightOrder;
    }
    const leftLayout = normalizeLayout(left.layout, left.order ?? left.columnOrder ?? 0);
    const rightLayout = normalizeLayout(right.layout, right.order ?? right.columnOrder ?? 0);
    if (leftLayout.y !== rightLayout.y) {
      return leftLayout.y - rightLayout.y;
    }
    if (leftLayout.x !== rightLayout.x) {
      return leftLayout.x - rightLayout.x;
    }
    return (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0);
  });
}

export function syncFormTargetWithTable(tableTarget: FormConfigTarget | null, formTarget: FormConfigTarget | null) {
  if (!tableTarget || !formTarget) {
    return null;
  }
  const orderedFields = orderFieldsByTable(formTarget.fields, tableTarget);
  formTarget.fields = buildSmartFormLayout(orderedFields);
  return formTarget;
}

export function toLegacyTableFields(target: FormConfigTarget | null): FormFieldConfig[] {
  if (!target) {
    return [];
  }
  return sortTargetFields(target.fields, 'table').map((field, index) => ({
    ...cloneField(field),
    visible: field.visible ?? field.columnVisible ?? true,
    order: field.order ?? field.columnOrder ?? index,
    columnVisible: field.visible ?? field.columnVisible ?? true,
    columnOrder: field.order ?? field.columnOrder ?? index,
  }));
}

export function upsertTargetField(target: FormConfigTarget, nextField: FormFieldConfig) {
  const next = normalizeField(nextField, target.targetType, target.fields.length, undefined);
  const fieldIndex = target.fields.findIndex((field) => field.fieldKey === next.fieldKey);
  if (fieldIndex >= 0) {
    target.fields.splice(fieldIndex, 1, next);
  } else {
    target.fields.push(next);
  }
  target.fields = sortTargetFields(target.fields, target.targetType);
}

function normalizeTarget(
  target: FormConfigTarget | null,
  manifestTarget: FormConfigManifestTarget | null,
  legacyFieldMap: Map<string, FormFieldConfig>,
  targetIndex: number
): FormConfigTarget {
  const targetType = target?.targetType || manifestTarget?.targetType || 'table';
  const targetKey = target?.targetKey || manifestTarget?.targetKey || fallbackTargetKey(targetType, targetIndex);
  const manifestFieldMap = new Map((manifestTarget?.fields || []).map((field) => [field.fieldKey, field]));
  const existingFields = Array.isArray(target?.fields) ? target!.fields : [];
  const existingFieldMap = new Map(existingFields.map((field) => [field.fieldKey, field]));
  const mergedFields: FormFieldConfig[] = [];

  if (manifestTarget?.fields?.length) {
    manifestTarget.fields.forEach((field, fieldIndex) => {
      mergedFields.push(
        normalizeField(
          existingFieldMap.get(field.fieldKey) || legacyFieldMap.get(field.fieldKey) || manifestFieldToField(field, targetType),
          targetType,
          fieldIndex,
          field
        )
      );
    });
  } else if (existingFields.length) {
    existingFields.forEach((field, fieldIndex) => {
      mergedFields.push(normalizeField(field, targetType, fieldIndex, manifestFieldMap.get(field.fieldKey)));
    });
  }

  if (!manifestTarget?.fields?.length) {
    existingFields.forEach((field) => {
      if (!mergedFields.some((item) => item.fieldKey === field.fieldKey)) {
        mergedFields.push(normalizeField(field, targetType, mergedFields.length, undefined));
      }
    });
  }

  return {
    targetKey,
    targetType,
    title: target?.title || manifestTarget?.title || targetKey,
    enabled: target?.enabled ?? true,
    order: target?.order ?? manifestTarget?.order ?? targetIndex,
    sourceSignature: {
      ...(manifestTarget?.sourceSignature || {}),
      ...(target?.sourceSignature || {}),
    },
    fields: sortTargetFields(mergedFields, targetType),
    runtimeIssues: target?.runtimeIssues || [],
  };
}

function applySmartFormLayout(target: FormConfigTarget, tableTarget: FormConfigTarget | null): FormConfigTarget {
  if (target.targetType === 'table' || !shouldUseSmartDefaultLayout(target)) {
    return target;
  }
  const orderedFields = tableTarget ? orderFieldsByTable(target.fields, tableTarget) : sortTargetFields(target.fields, target.targetType);
  return {
    ...target,
    fields: buildSmartFormLayout(orderedFields),
  };
}

function manifestFieldToField(field: FormConfigManifestTargetField, targetType: FormConfigTargetType): FormFieldConfig {
  return {
    fieldKey: field.fieldKey,
    label: field.label,
    queryKey: field.queryKey || field.fieldKey,
    visible: true,
    order: 0,
    columnVisible: true,
    columnOrder: 0,
    columnWidth: field.columnWidth,
    controlType: field.controlType || 'text',
    filterEnabled: false,
    quickSearchEnabled: false,
    operatorSet: field.controlType === 'text' ? ['contains', 'equals'] : ['equals'],
    defaultOperator: field.controlType === 'text' ? 'contains' : 'equals',
    defaultMatchMode: field.controlType === 'text' ? 'contains' : 'exact',
    allowMatchModeToggle: field.controlType === 'text',
    allowMultiple: false,
    optionSourceType: undefined,
    optionSourceConfig: {},
    placeholder: field.placeholder || (field.label ? `请输入${field.label}` : ''),
    previewSchema: field.previewSchema,
    hiddenButFilterable: false,
    layout: normalizeLayout(field.layout, 0, targetType),
  };
}

function normalizeField(
  field: FormFieldConfig,
  targetType: FormConfigTargetType,
  fieldIndex: number,
  manifestField?: FormConfigManifestTargetField
): FormFieldConfig {
  const fieldKey = field.fieldKey || manifestField?.fieldKey || field.queryKey || field.label || `field-${fieldIndex + 1}`;
  const visible = field.visible ?? field.columnVisible ?? true;
  const order = field.order ?? field.columnOrder ?? fieldIndex;
  const layout = normalizeLayout(field.layout || manifestField?.layout, order, targetType);
  const columnWidth = field.columnWidth ?? manifestField?.columnWidth;
  const label = field.label || manifestField?.label || fieldKey;
  const queryKey = field.queryKey || manifestField?.queryKey || fieldKey;
  const controlType = field.controlType || manifestField?.controlType || 'text';

  return {
    ...cloneField(field),
    fieldKey,
    label,
    queryKey,
    controlType,
    visible,
    order,
    columnVisible: visible,
    columnOrder: order,
    columnWidth,
    allowMatchModeToggle: field.allowMatchModeToggle ?? controlType === 'text',
    allowMultiple: field.allowMultiple ?? false,
    filterEnabled: field.filterEnabled ?? false,
    quickSearchEnabled: field.quickSearchEnabled ?? false,
    operatorSet:
      field.operatorSet && field.operatorSet.length
        ? field.operatorSet
        : controlType === 'text'
          ? ['contains', 'equals']
          : controlType === 'date-range' || controlType === 'number-range'
            ? ['between']
            : ['equals'],
    defaultOperator:
      field.defaultOperator ||
      (controlType === 'text'
        ? 'contains'
        : controlType === 'date-range' || controlType === 'number-range'
          ? 'between'
          : 'equals'),
    defaultMatchMode: field.defaultMatchMode || (controlType === 'text' ? 'contains' : 'exact'),
    optionSourceConfig: field.optionSourceConfig || {},
    placeholder: field.placeholder ?? manifestField?.placeholder ?? (label ? `请输入${label}` : ''),
    previewSchema: field.previewSchema || manifestField?.previewSchema,
    hiddenButFilterable: field.hiddenButFilterable ?? false,
    layout,
    runtimeIssues: field.runtimeIssues || [],
  };
}

function normalizeLayout(
  layout: Partial<FormFieldLayout> | undefined,
  fieldIndex: number,
  targetType: FormConfigTargetType = 'form'
): FormFieldLayout {
  if (layout && typeof layout.x === 'number' && typeof layout.y === 'number' && typeof layout.w === 'number' && typeof layout.h === 'number') {
    return {
      x: clamp(layout.x, 0, 23),
      y: Math.max(0, layout.y),
      w: clamp(layout.w, 1, 24),
      h: Math.max(1, layout.h),
    };
  }
  if (targetType === 'table') {
    return { x: 0, y: fieldIndex, w: FORM_GRID_COLUMNS, h: 1 };
  }
  return defaultLayoutForIndex(fieldIndex);
}

function defaultLayoutForIndex(fieldIndex: number, width = DEFAULT_FORM_SPAN): FormFieldLayout {
  const normalizedWidth = clamp(width, 1, FORM_GRID_COLUMNS);
  const perRow = Math.max(1, Math.floor(FORM_GRID_COLUMNS / normalizedWidth));
  const x = (fieldIndex % perRow) * normalizedWidth;
  const y = Math.floor(fieldIndex / perRow);
  return { x, y, w: normalizedWidth, h: DEFAULT_FORM_HEIGHT };
}

function shouldUseSmartDefaultLayout(target: FormConfigTarget) {
  if (target.targetType === 'table' || target.fields.length < 2) {
    return false;
  }
  const sortedFields = sortTargetFields(target.fields, target.targetType);
  const layouts = sortedFields.map((field, index) => normalizeLayout(field.layout, index, target.targetType));

  const isLegacySingleColumn = layouts.every(
    (layout, index) => layout.x === 0 && layout.y === index && layout.w === FORM_GRID_COLUMNS && layout.h === 1
  );
  const matchesDefaultTwoColumn = layouts.every((layout, index) => isSameLayout(layout, defaultLayoutForIndex(index)));

  return isLegacySingleColumn || matchesDefaultTwoColumn;
}

function orderFieldsByTable(fields: FormFieldConfig[], tableTarget: FormConfigTarget) {
  const tableOrderMap = new Map(
    sortTargetFields(tableTarget.fields, 'table').map((field, index) => [field.fieldKey, index])
  );
  const matched = fields.filter((field) => tableOrderMap.has(field.fieldKey));
  const unmatched = fields.filter((field) => !tableOrderMap.has(field.fieldKey));

  matched.sort((left, right) => (tableOrderMap.get(left.fieldKey) || 0) - (tableOrderMap.get(right.fieldKey) || 0));
  unmatched.sort((left, right) => (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0));

  return [...matched, ...unmatched];
}

function buildSmartFormLayout(fields: FormFieldConfig[]) {
  const nextFields = [...fields];
  const normalizedFields: FormFieldConfig[] = [];

  let cursor = 0;
  let row = 0;
  let rowHeight = DEFAULT_FORM_HEIGHT;

  nextFields.forEach((field, index) => {
    const width = getSmartFieldSpan(field);
    const height = getSmartFieldHeight(field);

    if (cursor + width > FORM_GRID_COLUMNS) {
      row += rowHeight;
      cursor = 0;
      rowHeight = DEFAULT_FORM_HEIGHT;
    }

    const nextLayout: FormFieldLayout = {
      x: cursor,
      y: row,
      w: width,
      h: height,
    };

    normalizedFields.push({
      ...cloneField(field),
      order: index,
      columnOrder: index,
      layout: preserveFieldHeight(field.layout, nextLayout),
    });

    cursor += width;
    rowHeight = Math.max(rowHeight, nextLayout.h);

    if (cursor >= FORM_GRID_COLUMNS) {
      row += rowHeight;
      cursor = 0;
      rowHeight = DEFAULT_FORM_HEIGHT;
    }
  });

  return normalizedFields;
}

function getSmartFieldSpan(field: FormFieldConfig) {
  if (isWideField(field)) {
    return FORM_GRID_COLUMNS;
  }
  if (isCompactField(field)) {
    return 8;
  }
  return DEFAULT_FORM_SPAN;
}

function getSmartFieldHeight(field: FormFieldConfig) {
  const previewRows = field.previewSchema?.rows || 0;
  const currentHeight = typeof field.layout?.h === 'number' ? Math.max(1, field.layout.h) : DEFAULT_FORM_HEIGHT;
  if (previewRows >= 3 || isWideField(field)) {
    return Math.max(currentHeight, 2);
  }
  return currentHeight;
}

function isCompactField(field: FormFieldConfig) {
  const key = field.fieldKey || '';
  const label = field.label || '';
  const placeholder = field.placeholder || field.previewSchema?.placeholder || '';
  const controlType = `${field.controlType || ''}`.toLowerCase();

  if (controlType === 'number-range' || controlType === 'date-range') {
    return false;
  }
  if (COMPACT_FIELD_KEY_PATTERN.test(key)) {
    return true;
  }
  return label.length <= 3 && placeholder.length <= 8 && (controlType === 'number' || controlType === 'select');
}

function isWideField(field: FormFieldConfig) {
  const key = field.fieldKey || '';
  const label = field.label || '';
  const controlType = `${field.controlType || ''}`.toLowerCase();
  const component = `${field.previewSchema?.component || ''}`.toLowerCase();
  const previewRows = field.previewSchema?.rows || 0;

  return (
    WIDE_FIELD_KEY_PATTERN.test(key) ||
    WIDE_FIELD_KEY_PATTERN.test(label) ||
    controlType.includes('textarea') ||
    component.includes('textarea') ||
    component.includes('text-area') ||
    previewRows >= 3
  );
}

function isSameLayout(left: FormFieldLayout, right: FormFieldLayout) {
  return left.x === right.x && left.y === right.y && left.w === right.w && left.h === right.h;
}

function preserveFieldHeight(layout: Partial<FormFieldLayout> | undefined, fallback: FormFieldLayout): FormFieldLayout {
  if (!layout) {
    return fallback;
  }
  return {
    ...fallback,
    h: typeof layout.h === 'number' ? Math.max(layout.h, fallback.h) : fallback.h,
  };
}

function createFallbackTableTarget(fields: FormFieldConfig[]): FormConfigTarget {
  return {
    targetKey: MAIN_TABLE_TARGET_KEY,
    targetType: 'table',
    title: '主表格',
    enabled: true,
    order: 0,
    fields: fields.map((field, index) => normalizeField(field, 'table', index, undefined)),
    runtimeIssues: [],
  };
}

function fallbackTargetKey(targetType: FormConfigTargetType, targetIndex: number) {
  if (targetType === 'table') {
    return targetIndex === 0 ? MAIN_TABLE_TARGET_KEY : `table-${targetIndex + 1}`;
  }
  if (targetType === 'modal-form') {
    return targetIndex === 0 ? MAIN_FORM_TARGET_KEY : `modal-form-${targetIndex + 1}`;
  }
  return `form-${targetIndex + 1}`;
}

function normalizeMenuBinding(
  menuBinding?: FormPageMenuBinding | null,
  manifest?: FormConfigManifestPage | null
): FormPageMenuBinding | undefined {
  const merged = {
    ...(menuBinding || {}),
  };
  if (manifest?.componentPath && !merged.componentPath) {
    merged.componentPath = manifest.componentPath;
  }
  if (manifest?.routeName && !merged.routeName) {
    merged.routeName = manifest.routeName;
  }
  if (!Object.keys(merged).length) {
    return undefined;
  }
  return merged;
}

function slugFromPath(value?: string) {
  if (!value) {
    return '';
  }
  return value
    .replace(/^\/+/, '')
    .replace(/\/+/g, '-')
    .replace(/[^a-zA-Z0-9-_]/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}
