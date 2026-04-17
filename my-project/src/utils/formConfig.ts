import dayjs, { isDayjs } from 'dayjs';
import type {
  CompiledFilterRule,
  FilterLogic,
  FormFieldConfig,
  FormFilterRule,
  FormOptionItem,
  FormPageConfig,
  OptionSourceType,
  SourceFieldItem,
} from '@/types/formConfig';
import { getQuickSearchConfig } from '@/utils/formConfigDesigner';

const MATCH_MODE_LABELS: Record<string, string> = {
  contains: '模糊匹配',
  exact: '精确匹配',
};

const OPERATOR_LABELS: Record<string, string> = {
  contains: '模糊匹配',
  equals: '等于',
  gt: '大于',
  lt: '小于',
  in: '属于',
  on: '指定日期',
  between: '区间',
};

export function createRuleId() {
  return `rule-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function createEmptyFilterRule(field?: FormFieldConfig | null): FormFilterRule {
  if (!field) {
    return { id: createRuleId() };
  }
  return normalizeRuleForField({ id: createRuleId(), fieldKey: field.fieldKey }, field);
}

export function getFieldByKey(fields: FormFieldConfig[], fieldKey?: string) {
  if (!fieldKey) {
    return null;
  }
  return fields.find((field) => field.fieldKey === fieldKey) || null;
}

export function getFieldSourceType(field?: FormFieldConfig | null) {
  return (field?.optionSourceType ||
    field?.optionSourceConfig?.optionSourceType ||
    (field?.controlType === 'select' ? 'static' : undefined)) as OptionSourceType | undefined;
}

export function defaultOperator(field?: FormFieldConfig | null, fallbackMatchMode?: string) {
  if (field?.defaultOperator) {
    return field.defaultOperator;
  }
  if (fallbackMatchMode === 'exact') {
    return 'equals';
  }
  switch (field?.controlType) {
    case 'text':
      return 'contains';
    case 'date':
      return 'on';
    case 'date-range':
    case 'number-range':
      return 'between';
    case 'number':
      return 'equals';
    case 'select':
    case 'remote-api':
    case 'remote-form':
      return field.allowMultiple ? 'in' : 'equals';
    default:
      return 'equals';
  }
}

export function defaultMatchMode(field?: FormFieldConfig | null) {
  if (field?.defaultMatchMode) {
    return field.defaultMatchMode;
  }
  return field?.controlType === 'text' ? 'contains' : 'exact';
}

export function normalizeRuleForField(rule: FormFilterRule, field?: FormFieldConfig | null): FormFilterRule {
  if (!field) {
    return { ...rule, value: undefined, valueTo: undefined };
  }
  const operator = defaultOperator(field, defaultMatchMode(field));
  const nextRule: FormFilterRule = {
    id: rule.id || createRuleId(),
    fieldKey: field.fieldKey,
    queryKey: field.queryKey,
    controlType: field.controlType || 'text',
    operator,
    matchMode: defaultMatchMode(field),
    value: undefined,
    valueTo: undefined,
  };

  if (field.controlType === 'select' || field.controlType === 'remote-api' || field.controlType === 'remote-form') {
    nextRule.operator = field.allowMultiple ? 'in' : 'equals';
    nextRule.matchMode = 'exact';
    nextRule.value = field.allowMultiple ? [] : undefined;
  }

  if (field.controlType === 'text' && nextRule.matchMode === 'exact') {
    nextRule.operator = 'equals';
  }

  if (field.controlType === 'date-range' || field.controlType === 'number-range') {
    nextRule.operator = 'between';
    nextRule.value = undefined;
    nextRule.valueTo = undefined;
  }

  return nextRule;
}

export function updateRuleOperator(rule: FormFilterRule, field?: FormFieldConfig | null) {
  if (!field) {
    return rule;
  }

  if (field.controlType === 'text') {
    if (rule.operator === 'equals') {
      return { ...rule, matchMode: 'exact' };
    }
    return { ...rule, operator: 'contains', matchMode: 'contains' };
  }

  return rule;
}

export function isRuleFilled(rule: FormFilterRule, field?: FormFieldConfig | null) {
  if (!rule.fieldKey || !field) {
    return false;
  }

  if (field.controlType === 'date-range' || field.controlType === 'number-range') {
    return hasValue(rule.value) || hasValue(rule.valueTo);
  }

  return hasValue(rule.value);
}

export function compileFilterRule(rule: FormFilterRule, field?: FormFieldConfig | null): CompiledFilterRule | null {
  if (!field || !rule.fieldKey || !isRuleFilled(rule, field)) {
    return null;
  }

  const operator = rule.operator || defaultOperator(field, rule.matchMode);
  const compiled: CompiledFilterRule = {
    fieldKey: field.fieldKey,
    queryKey: field.queryKey,
    controlType: field.controlType || 'text',
    operator,
    matchMode: rule.matchMode || defaultMatchMode(field),
  };

  if (field.controlType === 'date-range') {
    const [left, right] = normalizeRangeValues(rule.value);
    compiled.value = formatDateValue(left);
    compiled.valueTo = formatDateValue(right ?? rule.valueTo);
    return compiled;
  }

  if (field.controlType === 'number-range') {
    const [left, right] = normalizeRangeValues(rule.value);
    compiled.value = normalizeScalarValue(left);
    compiled.valueTo = normalizeScalarValue(right ?? rule.valueTo);
    return compiled;
  }

  if (field.controlType === 'date') {
    compiled.value = formatDateValue(rule.value);
    return compiled;
  }

  if (field.controlType === 'select' || field.controlType === 'remote-api' || field.controlType === 'remote-form') {
    compiled.value = operator === 'in' ? normalizeMultiValue(rule.value) : normalizeScalarValue(rule.value);
    return compiled;
  }

  compiled.value = normalizeScalarValue(rule.value);
  compiled.valueTo = normalizeScalarValue(rule.valueTo);
  return compiled;
}

export function compileFilterRules(rules: FormFilterRule[], fields: FormFieldConfig[]) {
  return rules
    .map((rule) => compileFilterRule(rule, getFieldByKey(fields, rule.fieldKey)))
    .filter((rule): rule is CompiledFilterRule => Boolean(rule));
}

export function buildConfiguredColumns<T extends Record<string, unknown>>(columns: T[], fields: FormFieldConfig[]) {
  const fieldMap = new Map(fields.map((field, index) => [field.fieldKey, { field, index }]));
  const decorated = columns.map((column, index) => {
    const columnRecord = column as Record<string, unknown>;
    const key = String(columnRecord.key ?? columnRecord.dataIndex ?? '');
    const configKey = String(columnRecord.configKey ?? key);
    const matched = fieldMap.get(configKey);
    const alwaysVisible = Boolean(columnRecord.alwaysVisible) || key === 'action' || key === 'operation';
    const visible = matched ? matched.field.visible ?? matched.field.columnVisible ?? true : true;
    const order = key === 'action' || key === 'operation'
      ? Number.MAX_SAFE_INTEGER
      : matched?.field.order ?? matched?.field.columnOrder ?? index;
    const hidden = alwaysVisible ? false : visible === false;
    const width = matched?.field.columnWidth;
    const merged = { ...column } as Record<string, unknown>;
    if (width) {
      merged.width = width;
    }
    return {
      hidden,
      index,
      alwaysVisible,
      matched: Boolean(matched),
      order,
      column: merged as T,
    };
  });

  return decorated
    .filter((item) => !item.hidden)
    .sort((left, right) => {
      if (left.order === right.order) {
        return left.index - right.index;
      }
      return left.order - right.order;
    })
    .map((item) => item.column);
}

export function isConfiguredFieldVisible(fields: FormFieldConfig[], fieldKey?: string, fallback = false) {
  const field = getFieldByKey(fields, fieldKey);
  if (!field) {
    return fallback;
  }
  return (field.visible ?? field.columnVisible ?? true) !== false;
}

export function operatorOptionsForField(field?: FormFieldConfig | null) {
  const operators = field?.operatorSet?.length ? field.operatorSet : [defaultOperator(field)];
  return operators.map((operator) => ({
    label: OPERATOR_LABELS[operator] || operator,
    value: operator,
  }));
}

export function matchModeOptions() {
  return Object.entries(MATCH_MODE_LABELS).map(([value, label]) => ({ value, label }));
}

export function normalizeStaticOptions(raw: unknown) {
  if (!Array.isArray(raw)) {
    return [] as FormOptionItem[];
  }
  return raw.map((item) => {
    const option = item as Record<string, unknown>;
    return {
      label: String(option.label ?? option.value ?? ''),
      value: (option.value ?? option.label ?? '') as string | number | boolean,
      raw: option,
    };
  });
}

export function cloneRules(rules: FormFilterRule[]) {
  return rules.map((rule) => ({
    ...rule,
    value: cloneValue(rule.value),
    valueTo: cloneValue(rule.valueTo),
  }));
}

export function textSuggestionsByField<T extends Record<string, unknown>>(records: T[], fields: FormFieldConfig[]) {
  const suggestionMap: Record<string, FormOptionItem[]> = {};
  const textFields = fields.filter((field) => field.controlType === 'text');
  textFields.forEach((field) => {
    const values = new Set<string>();
    records.forEach((record) => {
      const rawValue = record[field.fieldKey] ?? record[field.queryKey as keyof T];
      if (rawValue == null) {
        return;
      }
      const value = String(rawValue).trim();
      if (value) {
        values.add(value);
      }
    });
    suggestionMap[field.fieldKey] = Array.from(values)
      .slice(0, 20)
      .map((value) => ({ label: value, value }));
  });
  return suggestionMap;
}

export function smartSourceFieldMatch(sourceFields: SourceFieldItem[]) {
  const preferredValue = ['id', 'code', 'value', 'userId'];
  const preferredLabel = ['name', 'title', 'label', 'username', 'nickname', 'realName'];

  const valueField = pickPreferredField(sourceFields, preferredValue);
  const labelField = pickPreferredField(sourceFields, preferredLabel);
  const searchFields = Array.from(
    new Set(
      [labelField, ...preferredLabel.map((candidate) => pickPreferredField(sourceFields, [candidate]))]
        .filter((value): value is string => Boolean(value))
    )
  ).slice(0, 4);

  return {
    valueField: valueField || labelField || sourceFields[0]?.fieldKey,
    labelField: labelField || valueField || sourceFields[0]?.fieldKey,
    searchFields,
    joinField: valueField || sourceFields[0]?.fieldKey,
  };
}

export function resolveFilterLogic(config?: FormPageConfig | null): FilterLogic {
  return getQuickSearchConfig(config).defaultLogic === 'OR' ? 'OR' : 'AND';
}

function pickPreferredField(sourceFields: SourceFieldItem[], candidates: string[]) {
  const map = new Map(sourceFields.map((field) => [field.fieldKey, field]));
  for (const candidate of candidates) {
    if (map.has(candidate)) {
      return candidate;
    }
  }
  return undefined;
}

function cloneValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(cloneValue);
  }
  return value;
}

function normalizeRangeValues(value: unknown) {
  if (Array.isArray(value)) {
    return value as [unknown, unknown];
  }
  return [value, undefined] as [unknown, unknown];
}

function formatDateValue(value: unknown) {
  if (!hasValue(value)) {
    return undefined;
  }
  if (typeof value === 'string') {
    return value;
  }
  if (isDayjs(value)) {
    return value.format('YYYY-MM-DD');
  }
  const parsed = dayjs(value as dayjs.ConfigType);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : String(value);
}

function normalizeMultiValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.filter((item) => hasValue(item)).map(normalizeScalarValue);
  }
  return hasValue(value) ? [normalizeScalarValue(value)] : [];
}

function normalizeScalarValue(value: unknown) {
  if (!hasValue(value)) {
    return undefined;
  }
  if (isDayjs(value)) {
    return value.format('YYYY-MM-DD');
  }
  return value;
}

function hasValue(value: unknown) {
  if (value == null) {
    return false;
  }
  if (Array.isArray(value)) {
    return value.some(hasValue);
  }
  if (typeof value === 'string') {
    return value.trim().length > 0;
  }
  return true;
}
