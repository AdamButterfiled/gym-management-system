<template>
  <a-modal
    :open="visible"
    width="920px"
    class="advanced-filter-modal"
    wrapClassName="advanced-filter-modal"
    :mask="false"
    :destroyOnClose="false"
    @cancel="handleCancel"
    @ok="handleApply"
  >
    <template #title>
      <div class="advanced-filter-title-row">
        <span class="advanced-filter-title-text">高级筛选</span>
        <StandardButton type="text" icon="reload" class="head-reset" @click="handleClear">
          清空规则
        </StandardButton>
      </div>
    </template>

    <div class="advanced-filter-shell">
      <div class="rule-list">
        <template v-for="(rule, index) in localRules" :key="rule.id">
          <div v-if="index > 0" class="rule-connector" :aria-label="`当前使用 ${logicLabels[localLogic]} 连接`">
            <span class="rule-connector-line" />
            <span class="rule-connector-chip">{{ logicChips[localLogic] }}</span>
            <span class="rule-connector-line" />
          </div>

          <div class="rule-row">
            <span class="rule-index">{{ String(index + 1).padStart(2, '0') }}</span>

            <a-select
              :value="rule.fieldKey"
              class="workspace-filter-control rule-control rule-control--field"
              show-search
              :menuItemSelectedIcon="renderSelectedItemIcon"
              placeholder="搜索选择字段"
              :options="fieldOptions"
              option-filter-prop="label"
              popup-class-name="advanced-filter-dropdown"
              :getPopupContainer="resolvePopupContainer"
              @change="handleFieldChange(rule.id, $event as string)"
            />

            <a-select
              :value="rule.operator"
              class="workspace-filter-control rule-control rule-control--operator"
              :disabled="!resolveField(rule)"
              :menuItemSelectedIcon="renderSelectedItemIcon"
              placeholder="选择操作"
              :options="operatorOptionsForField(resolveField(rule))"
              popup-class-name="advanced-filter-dropdown"
              :getPopupContainer="resolvePopupContainer"
              @change="handleOperatorChange(rule.id, $event as string)"
            />

            <div class="rule-value">
              <template v-if="resolveField(rule)?.controlType === 'text'">
                <a-auto-complete
                  v-if="hasTextSuggestions(resolveField(rule))"
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                />
                <a-input
                  v-else
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                />
              </template>

              <template v-else-if="isSelectControl(resolveField(rule))">
                <a-select
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                  @search="handleRemoteSearch(rule.id, $event)"
                />
              </template>

              <template v-else-if="resolveField(rule)?.controlType === 'date'">
                <a-date-picker
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                />
              </template>

              <template v-else-if="resolveField(rule)?.controlType === 'date-range'">
                <a-range-picker
                  v-bind="resolveValueProps(rule)"
                  @calendarChange="handleRangeChange(rule.id, $event)"
                />
              </template>

              <template v-else-if="resolveField(rule)?.controlType === 'number'">
                <a-input-number
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                />
              </template>

              <template v-else-if="resolveField(rule)?.controlType === 'number-range'">
                <div class="range-number-input">
                  <a-input-number
                    :value="rule.value"
                    class="workspace-filter-control range-number-field"
                    placeholder="开始值"
                    @update:value="updateRuleValue(rule.id, [$event, rule.valueTo])"
                  />
                  <span class="range-separator">至</span>
                  <a-input-number
                    :value="rule.valueTo"
                    class="workspace-filter-control range-number-field"
                    placeholder="结束值"
                    @update:value="updateRuleValue(rule.id, [rule.value, $event])"
                  />
                </div>
              </template>

              <template v-else>
                <a-input
                  v-bind="resolveValueProps(rule)"
                  @update:value="updateRuleValue(rule.id, $event)"
                />
              </template>
            </div>

            <StandardButton
              type="icon"
              size="icon"
              icon="delete"
              danger
              class="rule-delete-button"
              aria-label="删除条件"
              @click="removeRule(rule.id)"
            />
          </div>
        </template>
      </div>

      <div class="append-bar">
        <div ref="appendMenuRef" class="append-menu-wrap">
          <button
            type="button"
            class="append-trigger"
            :class="{ 'is-open': appendMenuOpen }"
            aria-label="添加筛选条件"
            aria-haspopup="menu"
            aria-controls="append-logic-menu"
            :aria-expanded="appendMenuOpen"
            @click="toggleAppendMenu"
          >
            <Plus class="append-trigger-icon" />
          </button>

          <Transition name="append-menu-transition">
            <div
              v-show="appendMenuOpen"
              id="append-logic-menu"
              class="append-menu"
              role="menu"
              aria-label="选择条件连接方式"
            >
              <button
                type="button"
                class="append-menu-item"
                role="menuitem"
                @click="appendRuleWithLogic('AND')"
              >
                <span class="append-menu-icon-shell">
                  <BetweenHorizontalEnd class="append-menu-icon" />
                </span>
                <span class="append-menu-copy">
                  <strong>且</strong>
                  <small>同时满足</small>
                </span>
                <Check v-if="localLogic === 'AND'" class="append-menu-check" />
              </button>

              <button
                type="button"
                class="append-menu-item"
                role="menuitem"
                @click="appendRuleWithLogic('OR')"
              >
                <span class="append-menu-icon-shell">
                  <Split class="append-menu-icon" />
                </span>
                <span class="append-menu-copy">
                  <strong>或</strong>
                  <small>满足其一</small>
                </span>
                <Check v-if="localLogic === 'OR'" class="append-menu-check" />
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="advanced-filter-footer">
        <StandardButton type="default" class="footer-action" @click="handleCancel">
          取消
        </StandardButton>
        <StandardButton type="primary" class="footer-action" @click="handleApply">
          确定
        </StandardButton>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import dayjs, { isDayjs } from 'dayjs';
import { BetweenHorizontalEnd, Check, Plus, Split } from 'lucide-vue-next';
import StandardButton from '@/components/common/StandardButton.vue';
import { fetchDictOptions, fetchRemoteApiOptions, fetchRemoteFormOptions } from '@/api/formConfig';
import type { FilterLogic, FormFieldConfig, FormFilterRule, FormOptionItem } from '@/types/formConfig';
import {
  cloneRules,
  createEmptyFilterRule,
  getFieldByKey,
  getFieldSourceType,
  normalizeRuleForField,
  normalizeStaticOptions,
  operatorOptionsForField,
  resolveFilterLogic,
  updateRuleOperator,
} from '@/utils/formConfig';

const props = withDefaults(defineProps<{
  visible: boolean;
  fields: FormFieldConfig[];
  logic?: FilterLogic;
  rules?: FormFilterRule[];
  textSuggestions?: Record<string, FormOptionItem[]>;
}>(), {
  logic: 'AND',
  rules: () => [],
  textSuggestions: () => ({}),
});

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'apply', payload: { logic: FilterLogic; rules: FormFilterRule[] }): void;
}>();

const localLogic = ref<FilterLogic>('AND');
const localRules = ref<FormFilterRule[]>([]);
const fieldOptionsCache = reactive<Record<string, FormOptionItem[]>>({});
const loadingKeys = reactive<Record<string, boolean>>({});
const appendMenuOpen = ref(false);
const appendMenuRef = ref<HTMLElement | null>(null);

const logicLabels: Record<FilterLogic, string> = {
  AND: '全部满足',
  OR: '任一满足',
};

const logicChips: Record<FilterLogic, string> = {
  AND: '且',
  OR: '或',
};

const fieldOptions = computed(() =>
  props.fields
    .filter((field) => field.filterEnabled)
    .map((field) => ({ label: field.label, value: field.fieldKey }))
);

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return;
    }
    localLogic.value = props.logic || resolveFilterLogic(null);
    localRules.value = cloneRules(props.rules || []);
    if (!localRules.value.length) {
      localRules.value = [createEmptyFilterRule()];
    }
    appendMenuOpen.value = false;
    preloadOptions();
  },
  { immediate: true }
);

watch(
  () => props.fields,
  () => {
    preloadOptions();
  },
  { deep: true }
);

function resolveField(rule: FormFilterRule) {
  return getFieldByKey(props.fields, rule.fieldKey);
}

function addRule() {
  localRules.value.push(createEmptyFilterRule());
}

function toggleAppendMenu() {
  appendMenuOpen.value = !appendMenuOpen.value;
}

function closeAppendMenu() {
  appendMenuOpen.value = false;
}

function appendRuleWithLogic(logic: FilterLogic) {
  localLogic.value = logic;
  addRule();
  closeAppendMenu();
}

function removeRule(ruleId: string) {
  localRules.value = localRules.value.filter((rule) => rule.id !== ruleId);
  if (!localRules.value.length) {
    addRule();
  }
}

function handleFieldChange(ruleId: string, fieldKey: string) {
  const field = getFieldByKey(props.fields, fieldKey);
  localRules.value = localRules.value.map((rule) =>
    rule.id === ruleId ? normalizeRuleForField({ ...rule, fieldKey }, field) : rule
  );
  if (field) {
    ensureOptions(field);
  }
}

function handleOperatorChange(ruleId: string, operator: string) {
  localRules.value = localRules.value.map((rule) => {
    if (rule.id !== ruleId) {
      return rule;
    }
    const field = resolveField(rule);
    return updateRuleOperator({ ...rule, operator }, field);
  });
}

function updateRuleValue(ruleId: string, value: unknown) {
  localRules.value = localRules.value.map((rule) => {
    if (rule.id !== ruleId) {
      return rule;
    }
    return setRuleValue(rule, value);
  });
}

function handleRangeChange(ruleId: string, value: unknown) {
  updateRuleValue(ruleId, value);
}

async function handleRemoteSearch(ruleId: string, keyword: string) {
  const rule = localRules.value.find((item) => item.id === ruleId);
  const field = rule ? resolveField(rule) : null;
  if (!field || !keyword) {
    return;
  }
  await ensureOptions(field, keyword);
}

async function preloadOptions() {
  await Promise.all(
    props.fields
      .filter((field) => field.filterEnabled)
      .map((field) => ensureOptions(field))
  );
}

async function ensureOptions(field: FormFieldConfig, keyword = '') {
  const cacheKey = `${field.fieldKey}:${keyword}`;
  if (fieldOptionsCache[cacheKey]) {
    return fieldOptionsCache[cacheKey];
  }

  const sourceType = getFieldSourceType(field);
  if (!sourceType) {
    return [];
  }

  loadingKeys[cacheKey] = true;
  try {
    let options: FormOptionItem[] = [];
    if (sourceType === 'static') {
      options = normalizeStaticOptions(field.optionSourceConfig?.options);
    } else if (sourceType === 'dict') {
      const dictType = String(field.optionSourceConfig?.dictType || '');
      options = dictType ? await fetchDictOptions(dictType) : [];
    } else if (sourceType === 'remote-api') {
      options = await fetchRemoteApiOptions({
        keyword,
        optionSourceType: sourceType,
        optionSourceConfig: field.optionSourceConfig,
      });
    } else if (sourceType === 'remote-form') {
      options = await fetchRemoteFormOptions({
        keyword,
        optionSourceType: sourceType,
        optionSourceConfig: field.optionSourceConfig,
      });
    }
    fieldOptionsCache[cacheKey] = options;
    if (!keyword) {
      fieldOptionsCache[field.fieldKey] = options;
    }
    return options;
  } finally {
    loadingKeys[cacheKey] = false;
  }
}

function resolveValueProps(rule: FormFilterRule) {
  const field = resolveField(rule);
  if (!field) {
    return {
      value: rule.value as string,
      disabled: true,
      class: 'workspace-filter-control rule-control rule-control--value',
      placeholder: '先选择字段',
    };
  }

  const common = {
    disabled: false,
    class: 'workspace-filter-control rule-control rule-control--value',
    placeholder: field.placeholder || `请输入${field.label}`,
    menuItemSelectedIcon: renderSelectedItemIcon,
  };

  switch (field.controlType) {
    case 'text':
      if (hasTextSuggestions(field)) {
        return {
          ...common,
          value: (rule.value as string) || '',
          options: props.textSuggestions[field.fieldKey] || [],
          showArrow: true,
          filterOption: (input: string, option: { label?: string; value?: string | number | boolean }) =>
            String(option?.label ?? option?.value ?? '').toLowerCase().includes(input.trim().toLowerCase()),
          allowClear: true,
          popupClassName: 'advanced-filter-dropdown',
          getPopupContainer: resolvePopupContainer,
        };
      }

      return {
        ...common,
        value: (rule.value as string) || '',
      };
    case 'select':
    case 'remote-api':
    case 'remote-form':
      return {
        ...common,
        value: rule.value,
        mode: rule.operator === 'in' || field.allowMultiple ? 'multiple' : undefined,
        showSearch: true,
        filterOption: field.controlType === 'select',
        options: fieldOptionsCache[field.fieldKey] || [],
        loading: loadingKeys[field.fieldKey],
        allowClear: true,
        popupClassName: 'advanced-filter-dropdown',
        getPopupContainer: resolvePopupContainer,
      };
    case 'date':
      return {
        ...common,
        value: toDayjs(rule.value),
        style: 'width: 100%',
        valueFormat: undefined,
        popupClassName: 'advanced-filter-picker-dropdown',
        getPopupContainer: resolvePopupContainer,
      };
    case 'date-range':
      return {
        ...common,
        value: toDayjsRange(rule.value, rule.valueTo),
        style: 'width: 100%',
        popupClassName: 'advanced-filter-picker-dropdown',
        getPopupContainer: resolvePopupContainer,
      };
    case 'number':
      return {
        ...common,
        value: rule.value as number | undefined,
        style: 'width: 100%',
      };
    case 'number-range':
      return {
        value: [rule.value, rule.valueTo],
      };
    default:
      return {
        ...common,
        value: (rule.value as string) || '',
      };
  }
}

function handleApply() {
  emit('apply', {
    logic: localLogic.value,
    rules: localRules.value,
  });
}

function handleCancel() {
  emit('update:visible', false);
}

function handleClear() {
  localLogic.value = props.logic || 'AND';
  localRules.value = [createEmptyFilterRule()];
  closeAppendMenu();
}

function handleDocumentPointerDown(event: MouseEvent) {
  if (!appendMenuOpen.value) {
    return;
  }

  const target = event.target as Node | null;
  if (!target || !appendMenuRef.value?.contains(target)) {
    closeAppendMenu();
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeAppendMenu();
  }
}

function setRuleValue(rule: FormFilterRule, value: unknown) {
  const field = resolveField(rule);
  if (!field) {
    return rule;
  }
  if (field.controlType === 'date-range' || field.controlType === 'number-range') {
    const [left, right] = Array.isArray(value) ? value : [undefined, undefined];
    return {
      ...rule,
      value: left,
      valueTo: right,
    };
  }
  return {
    ...rule,
    value,
  };
}

function toDayjs(value: unknown) {
  if (!value) {
    return undefined;
  }
  if (isDayjs(value)) {
    return value;
  }
  const parsed = dayjs(value as string);
  return parsed.isValid() ? parsed : undefined;
}

function toDayjsRange(value: unknown, valueTo: unknown) {
  const left = toDayjs(value);
  const right = toDayjs(valueTo);
  return left || right ? [left, right] : undefined;
}

function isSelectControl(field?: FormFieldConfig | null) {
  return field?.controlType === 'select' || field?.controlType === 'remote-api' || field?.controlType === 'remote-form';
}

function hasTextSuggestions(field?: FormFieldConfig | null) {
  if (!field || field.controlType !== 'text') {
    return false;
  }

  return (props.textSuggestions[field.fieldKey] || []).length > 0;
}

function renderSelectedItemIcon(props?: { isSelected?: boolean }) {
  return props?.isSelected
    ? h(Check, { class: 'advanced-filter-selected-icon', 'aria-hidden': 'true' })
    : null;
}

function resolvePopupContainer() {
  return document.body;
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown);
  document.removeEventListener('keydown', handleDocumentKeydown);
});
</script>

<style scoped>
.advanced-filter-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.advanced-filter-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  width: 100%;
  padding-right: 64px;
}

.advanced-filter-title-text {
  color: var(--mono-text);
  font-size: 18px;
  font-weight: 400 !important;
  line-height: 1.35;
  letter-spacing: 0;
}

.head-reset {
  min-width: 0 !important;
  height: 32px !important;
  padding: 0 10px !important;
  border-radius: var(--mono-radius-pill) !important;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rule-connector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 12px 0;
}

.rule-connector-line {
  flex: 1;
  height: 1px;
  background: var(--mono-line);
}

.rule-connector-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-pill);
  background: var(--mono-bg-elevated);
  color: var(--mono-text);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.rule-row {
  display: grid;
  grid-template-columns: 56px minmax(180px, 1fr) minmax(150px, 0.82fr) minmax(230px, 1.08fr) 48px;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: var(--mono-radius-md);
  background: #ffffff;
}

.rule-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  width: 48px;
  min-width: 48px;
  height: 48px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-pill);
  background: var(--mono-surface-subtle);
  color: var(--mono-text-secondary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.rule-control {
  width: 100%;
}

.rule-value {
  min-width: 0;
  display: flex;
  overflow: hidden;
}

.rule-value :deep(.workspace-filter-control) {
  flex: 1 1 auto;
  min-width: 0;
}

.rule-delete-button {
  width: 48px !important;
  min-width: 48px !important;
  height: 48px !important;
  padding: 0 !important;
  border-radius: 50% !important;
  aspect-ratio: 1 / 1;
  flex: 0 0 48px;
}

.append-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 10px;
}

.append-menu-wrap {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.append-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  min-width: 52px;
  height: 52px;
  padding: 0;
  border: 1px solid #111111;
  border-radius: 999px;
  background: #111111;
  color: #ffffff;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.append-trigger:hover {
  border-color: #000000;
  background: #000000;
}

.append-trigger:focus-visible {
  outline: 2px solid rgba(17, 17, 17, 0.22);
  outline-offset: 2px;
}

.append-trigger-icon,
.append-menu-check {
  width: 18px;
  height: 18px;
}

.append-trigger-icon {
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.append-trigger.is-open .append-trigger-icon {
  transform: rotate(45deg);
}

.append-menu {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  z-index: 24;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-width: min(220px, calc(100vw - 64px));
  padding: 8px;
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-md);
  background: #ffffff;
  transform: translateX(-50%);
}

.append-menu-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 16px;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  color: var(--mono-text);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.append-menu-item:hover {
  border-color: var(--mono-line);
  background: var(--mono-surface-subtle);
}

.append-menu-code {
  display: none;
}

.append-menu-icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--mono-line);
  border-radius: var(--page-controlled-input-radius, var(--mono-radius-pill));
  background: var(--mono-surface-subtle);
  color: var(--mono-text);
}

.append-menu-icon {
  width: 18px;
  height: 18px;
}

.append-menu-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.append-menu-copy strong {
  color: var(--mono-text);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
}

.append-menu-copy small {
  color: var(--mono-text-tertiary);
  font-size: 12px;
  line-height: 1.35;
}

.append-menu-check {
  color: #111111;
}

.append-menu-transition-enter-active,
.append-menu-transition-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

.append-menu-transition-enter-from,
.append-menu-transition-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px);
}

.append-menu-transition-enter-to,
.append-menu-transition-leave-from {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.advanced-filter-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.footer-action {
  min-width: 88px !important;
}

:deep(.rule-control--value.ant-select),
:deep(.rule-control--value.ant-picker),
:deep(.rule-control--value.ant-input),
:deep(.rule-control--value.ant-input-number),
:deep(.rule-control--value.ant-select .ant-select-selector) {
  width: 100%;
}

.range-number-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  flex: 1 1 100%;
  width: 100%;
  min-width: 0;
  gap: 8px;
  align-items: center;
}

:deep(.range-number-field.ant-input-number) {
  width: 100%;
  min-width: 0;
}

:deep(.range-number-field .ant-input-number-input) {
  min-width: 0;
}

:deep(.range-separator) {
  color: var(--mono-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

:deep(.advanced-filter-modal .ant-modal-content) {
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-xl);
  background: #ffffff;
  box-shadow: none !important;
}

:deep(.advanced-filter-modal .ant-modal-header) {
  padding-top: 14px;
  padding-bottom: 0;
  background: #ffffff;
}

:deep(.advanced-filter-modal .ant-modal-title) {
  width: 100%;
  font-size: 18px;
  font-weight: 400 !important;
}

:deep(.advanced-filter-modal .ant-modal-body) {
  padding-top: 20px;
  padding-bottom: 20px;
}

:deep(.advanced-filter-modal .ant-modal-footer) {
  padding-top: 0;
  border-top: 1px solid var(--mono-line);
}

:deep(.advanced-filter-modal .ant-modal-close) {
  border-radius: var(--mono-radius-sm);
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selector),
:deep(.advanced-filter-modal .workspace-filter-control.ant-picker),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-number),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper) {
  min-height: 44px;
  border: 1px solid rgba(17, 17, 17, 0.22) !important;
  border-radius: var(--page-controlled-input-radius) !important;
  background: #ffffff !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selector) {
  padding: 0 14px !important;
  border: 1px solid rgba(17, 17, 17, 0.22) !important;
  background: #ffffff !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select:hover .ant-select-selector) {
  border-color: rgba(17, 17, 17, 0.3) !important;
  background: #ffffff !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-picker:hover),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-number:hover),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input:hover),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper:hover) {
  border-color: rgba(17, 17, 17, 0.3) !important;
  background: #ffffff !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select.ant-select-multiple .ant-select-selector) {
  height: auto;
  min-height: 44px;
  padding: 6px !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select-focused .ant-select-selector) {
  border-color: rgba(17, 17, 17, 0.38) !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-picker-focused),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-number-focused),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper-focused),
:deep(.advanced-filter-modal .workspace-filter-control.ant-input:focus) {
  border-color: rgba(17, 17, 17, 0.38) !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

:deep(.advanced-filter-modal .workspace-filter-control.ant-select-multiple .ant-select-selection-item) {
  border: none !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

:deep(.advanced-filter-modal .ant-select-dropdown),
:deep(.advanced-filter-modal .ant-picker-dropdown) {
  border: 1px solid rgba(17, 17, 17, 0.22) !important;
  border-radius: var(--mono-radius-md) !important;
  background: #ffffff !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
}

:deep(.advanced-filter-modal .ant-select-item-option-active:not(.ant-select-item-option-disabled)),
:deep(.advanced-filter-modal .ant-picker-cell-in-view.ant-picker-cell-today .ant-picker-cell-inner::before) {
  background: var(--mono-select-option-bg-hover) !important;
}

:deep(.advanced-filter-modal .ant-select-item-option-selected:not(.ant-select-item-option-disabled)) {
  background: transparent !important;
  color: var(--mono-control-text) !important;
  box-shadow: none !important;
  font-weight: 400 !important;
}

:deep(.advanced-filter-modal .ant-select-item-option-content) {
  flex: 1 1 auto;
  min-width: 0;
}

:deep(.advanced-filter-modal .ant-select-item-option-selected .ant-select-item-option-state) {
  color: var(--mono-control-text) !important;
  opacity: 1 !important;
}

:deep(.advanced-filter-modal .advanced-filter-selected-icon) {
  width: 11px;
  height: 11px;
  color: var(--mono-control-text);
  opacity: 0.9;
  stroke-width: 2.15;
}

@media (max-width: 900px) {
  .advanced-filter-title-row {
    gap: 16px;
    padding-right: 52px;
  }

  .rule-row {
    grid-template-columns: 1fr;
  }

  .rule-index {
    width: 48px;
  }

  .rule-delete-button {
    justify-self: flex-end;
  }

  .append-bar {
    align-items: center;
  }

  .append-trigger {
    width: auto;
    max-width: 100%;
  }
}
</style>

<style>
.advanced-filter-modal {
  --advanced-filter-control-height: 46px;
  --advanced-filter-control-radius: 23px;
  --advanced-filter-control-border: rgba(15, 23, 42, 0.16);
  --advanced-filter-control-border-hover: rgba(15, 23, 42, 0.24);
  --advanced-filter-control-border-focus: rgba(15, 23, 42, 0.34);
  --advanced-filter-control-bg: #ffffff;
  --advanced-filter-control-bg-disabled: #fafafa;
  --advanced-filter-control-text: #111827;
  --advanced-filter-control-muted: #8d96a3;
}

.advanced-filter-modal .ant-modal-close {
  top: 24px;
  right: 24px;
  width: 36px;
  height: 36px;
  border-radius: 18px;
}

.advanced-filter-modal .ant-modal-header {
  padding: 14px 18px 0;
  background: #ffffff;
}

.advanced-filter-modal .ant-modal-title {
  width: 100%;
  font-size: 18px;
  font-weight: 400 !important;
  line-height: 1.35;
}

.advanced-filter-modal .ant-modal-body {
  padding-top: 20px;
}

.advanced-filter-modal .workspace-filter-control,
.advanced-filter-modal .workspace-filter-control.ant-select,
.advanced-filter-modal .workspace-filter-control.ant-picker,
.advanced-filter-modal .workspace-filter-control.ant-input,
.advanced-filter-modal .workspace-filter-control.ant-input-number,
.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper,
.advanced-filter-modal .workspace-filter-control.ant-auto-complete {
  width: 100%;
  min-width: 0;
}

.advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selector,
.advanced-filter-modal .workspace-filter-control.ant-picker,
.advanced-filter-modal .workspace-filter-control.ant-input,
.advanced-filter-modal .workspace-filter-control.ant-input-number,
.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper {
  height: var(--advanced-filter-control-height) !important;
  min-height: var(--advanced-filter-control-height) !important;
  border: 1px solid var(--advanced-filter-control-border) !important;
  border-radius: var(--advanced-filter-control-radius) !important;
  background: var(--advanced-filter-control-bg) !important;
  color: var(--advanced-filter-control-text) !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selector {
  display: flex;
  align-items: center;
  padding: 0 16px !important;
}

.advanced-filter-modal .workspace-filter-control.ant-input {
  padding: 0 16px !important;
  line-height: calc(var(--advanced-filter-control-height) - 2px) !important;
}

.advanced-filter-modal .workspace-filter-control.ant-picker {
  padding: 0 16px !important;
}

.advanced-filter-modal .workspace-filter-control.ant-input-number .ant-input-number-input {
  height: calc(var(--advanced-filter-control-height) - 2px) !important;
  padding: 0 16px !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select-single .ant-select-selector .ant-select-selection-search-input {
  height: calc(var(--advanced-filter-control-height) - 2px) !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select-single .ant-select-selector .ant-select-selection-item,
.advanced-filter-modal .workspace-filter-control.ant-select-single .ant-select-selector .ant-select-selection-placeholder {
  line-height: calc(var(--advanced-filter-control-height) - 2px) !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selection-placeholder,
.advanced-filter-modal .workspace-filter-control.ant-input::placeholder,
.advanced-filter-modal .workspace-filter-control.ant-input-number .ant-input-number-input::placeholder {
  color: var(--advanced-filter-control-muted) !important;
  font-weight: 500;
  opacity: 1;
}

.advanced-filter-modal .workspace-filter-control.ant-select:hover .ant-select-selector,
.advanced-filter-modal .workspace-filter-control.ant-picker:hover,
.advanced-filter-modal .workspace-filter-control.ant-input:hover,
.advanced-filter-modal .workspace-filter-control.ant-input-number:hover,
.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper:hover {
  border-color: var(--advanced-filter-control-border-hover) !important;
  background: #ffffff !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select-focused .ant-select-selector,
.advanced-filter-modal .workspace-filter-control.ant-picker-focused,
.advanced-filter-modal .workspace-filter-control.ant-input:focus,
.advanced-filter-modal .workspace-filter-control.ant-input-focused,
.advanced-filter-modal .workspace-filter-control.ant-input-number-focused,
.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper-focused {
  border-color: var(--advanced-filter-control-border-focus) !important;
  background: #ffffff !important;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.04) !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select-disabled .ant-select-selector,
.advanced-filter-modal .workspace-filter-control.ant-picker-disabled,
.advanced-filter-modal .workspace-filter-control.ant-input[disabled],
.advanced-filter-modal .workspace-filter-control.ant-input-number-disabled,
.advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper-disabled {
  border-color: rgba(15, 23, 42, 0.1) !important;
  background: var(--advanced-filter-control-bg-disabled) !important;
  color: var(--advanced-filter-control-muted) !important;
  cursor: not-allowed;
  opacity: 1;
}

.advanced-filter-modal .workspace-filter-control.ant-select-disabled .ant-select-selection-placeholder,
.advanced-filter-modal .workspace-filter-control.ant-input[disabled]::placeholder {
  color: var(--advanced-filter-control-muted) !important;
  opacity: 1;
}

.advanced-filter-modal .workspace-filter-control.ant-select-multiple .ant-select-selector {
  height: auto !important;
  min-height: var(--advanced-filter-control-height) !important;
  padding: 6px 12px !important;
}

.advanced-filter-modal .workspace-filter-control.ant-select-multiple .ant-select-selection-item {
  border: 1px solid rgba(15, 23, 42, 0.1) !important;
  border-radius: 999px !important;
  background: #f6f7f8 !important;
  color: var(--advanced-filter-control-text) !important;
  box-shadow: none !important;
}

.advanced-filter-modal .range-number-input {
  align-items: center;
}

.advanced-filter-modal .rule-delete-button {
  width: 48px !important;
  min-width: 48px !important;
  max-width: 48px !important;
  height: 48px !important;
  min-height: 48px !important;
  max-height: 48px !important;
  padding: 0 !important;
  border: 1px solid rgba(15, 23, 42, 0.12) !important;
  border-radius: 50% !important;
  background: #f8f8f9 !important;
  color: #9aa3af !important;
  box-shadow: none !important;
}

.advanced-filter-modal .rule-delete-button:hover {
  border-color: rgba(15, 23, 42, 0.18) !important;
  background: #f2f3f5 !important;
  color: #667085 !important;
}

.advanced-filter-modal .rule-delete-button svg {
  width: 18px;
  height: 18px;
}

.advanced-filter-dropdown,
.advanced-filter-picker-dropdown {
  border: 1px solid rgba(15, 23, 42, 0.14) !important;
  border-radius: 14px !important;
  background: #ffffff !important;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08) !important;
  backdrop-filter: none !important;
}

.advanced-filter-dropdown .ant-select-item-option-active:not(.ant-select-item-option-disabled) {
  background: #f6f7f8 !important;
}

.advanced-filter-dropdown .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
  background: transparent !important;
  color: var(--advanced-filter-control-text, #111827) !important;
  font-weight: 400 !important;
}

.advanced-filter-dropdown .ant-select-item-option-selected .ant-select-item-option-state {
  color: var(--advanced-filter-control-text, #111827) !important;
  opacity: 1 !important;
}

.advanced-filter-dropdown .advanced-filter-selected-icon {
  width: 11px;
  height: 11px;
  color: var(--advanced-filter-control-text, #111827);
  opacity: 0.9;
  stroke-width: 2.15;
}

html.dark .advanced-filter-modal {
  --advanced-filter-control-border: rgba(255, 255, 255, 0.16);
  --advanced-filter-control-border-hover: rgba(255, 255, 255, 0.24);
  --advanced-filter-control-border-focus: rgba(255, 255, 255, 0.34);
  --advanced-filter-control-bg: #1f1f1f;
  --advanced-filter-control-bg-disabled: #222222;
  --advanced-filter-control-text: #ffffff;
  --advanced-filter-control-muted: rgba(255, 255, 255, 0.78);
}

html.dark .advanced-filter-modal .ant-modal-content,
html.dark .advanced-filter-modal .ant-modal-header {
  border-color: rgba(255, 255, 255, 0.1) !important;
  background: #1f1f1f !important;
  box-shadow: none !important;
}

html.dark .advanced-filter-modal .ant-modal-title,
html.dark .advanced-filter-modal .advanced-filter-title-text {
  color: rgba(255, 255, 255, 0.94) !important;
}

html.dark .advanced-filter-modal .ant-modal-footer {
  border-top-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .advanced-filter-modal .rule-row,
html.dark .advanced-filter-modal .rule-index,
html.dark .advanced-filter-modal .append-trigger,
html.dark .advanced-filter-modal .rule-delete-button,
html.dark .advanced-filter-modal .workspace-filter-control.ant-select .ant-select-selector,
html.dark .advanced-filter-modal .workspace-filter-control.ant-picker,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-number,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper {
  border-color: rgba(255, 255, 255, 0.1) !important;
  background: #1f1f1f !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.92) !important;
}

html.dark .advanced-filter-modal .workspace-filter-control.ant-select:hover .ant-select-selector,
html.dark .advanced-filter-modal .workspace-filter-control.ant-picker:hover,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input:hover,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-number:hover,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper:hover,
html.dark .advanced-filter-modal .workspace-filter-control.ant-select-focused .ant-select-selector,
html.dark .advanced-filter-modal .workspace-filter-control.ant-picker-focused,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input:focus,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-focused,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-number-focused,
html.dark .advanced-filter-modal .workspace-filter-control.ant-input-affix-wrapper-focused {
  border-color: rgba(255, 255, 255, 0.18) !important;
  background: #242424 !important;
  box-shadow: none !important;
}

html.dark .advanced-filter-modal .workspace-filter-control.ant-select-multiple .ant-select-selection-item {
  border-color: rgba(255, 255, 255, 0.1) !important;
  background: rgba(255, 255, 255, 0.06) !important;
  color: rgba(255, 255, 255, 0.92) !important;
}

html.dark .advanced-filter-modal .rule-delete-button:hover,
html.dark .advanced-filter-modal .append-trigger:hover {
  border-color: rgba(255, 255, 255, 0.18) !important;
  background: #2a2a2a !important;
  color: rgba(255, 255, 255, 0.92) !important;
}

html.dark .advanced-filter-dropdown,
html.dark .advanced-filter-picker-dropdown {
  border-color: rgba(255, 255, 255, 0.12) !important;
  background: #1f1f1f !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.92) !important;
}

html.dark .advanced-filter-dropdown .ant-select-item,
html.dark .advanced-filter-dropdown .ant-select-item-option-content,
html.dark .advanced-filter-picker-dropdown .ant-picker-cell {
  color: rgba(255, 255, 255, 0.82) !important;
}

html.dark .advanced-filter-dropdown .ant-select-item-option-active:not(.ant-select-item-option-disabled),
html.dark .advanced-filter-dropdown .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.94) !important;
}

html.dark .advanced-filter-dropdown .ant-select-item-option-selected .ant-select-item-option-state,
html.dark .advanced-filter-dropdown .advanced-filter-selected-icon {
  color: rgba(255, 255, 255, 0.94) !important;
}
</style>
