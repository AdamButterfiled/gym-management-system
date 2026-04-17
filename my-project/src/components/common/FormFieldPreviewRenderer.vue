<template>
  <a-form-item :label="field.label" :extra="schema?.extra || undefined" class="form-field-preview-renderer__item">
    <div
      v-if="schema?.wrapperClassName || schema?.wrapperStyleText"
      :class="schema?.wrapperClassName || undefined"
      :style="wrapperStyle"
      class="form-field-preview-renderer__wrapper"
    >
      <component :is="resolvedRenderer" />
    </div>
    <component :is="resolvedRenderer" v-else />
  </a-form-item>
</template>

<script setup lang="ts">
import { computed, h, resolveComponent } from 'vue';
import {
  AppstoreOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  FileOutlined,
  FolderOutlined,
  FormOutlined,
  HomeOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue';
import StandardInput from '@/components/common/StandardInput.vue';
import type { FormFieldConfig, FormFieldPreviewOption, FormFieldPreviewSchema } from '@/types/formConfig';

const props = defineProps<{
  field: FormFieldConfig;
  height?: number;
}>();

const schema = computed<FormFieldPreviewSchema | undefined>(() => props.field.previewSchema);
const AInput = resolveComponent('a-input');
const ASelect = resolveComponent('a-select');
const ASelectOption = resolveComponent('a-select-option');
const ATreeSelect = resolveComponent('a-tree-select');
const AInputNumber = resolveComponent('a-input-number');
const ARadioGroup = resolveComponent('a-radio-group');
const ARadio = resolveComponent('a-radio');
const ARadioButton = resolveComponent('a-radio-button');
const ACheckboxGroup = resolveComponent('a-checkbox-group');
const ACheckbox = resolveComponent('a-checkbox');
const ASwitch = resolveComponent('a-switch');
const ADatePicker = resolveComponent('a-date-picker');
const ATimePicker = resolveComponent('a-time-picker');
const ARangePicker = resolveComponent('a-range-picker');
const ATextarea = resolveComponent('a-textarea');

const previewIconMap: Record<string, unknown> = {
  AppstoreOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  FileOutlined,
  FolderOutlined,
  FormOutlined,
  HomeOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WarningOutlined,
};

const placeholderText = computed(() => {
  if (schema.value?.placeholder) {
    return schema.value.placeholder;
  }
  if (schema.value?.component === 'a-select' || schema.value?.component === 'a-tree-select') {
    return `请选择${props.field.label || props.field.fieldKey}`;
  }
  return props.field.placeholder || `请输入${props.field.label || props.field.fieldKey}`;
});

const wrapperStyle = computed(() => toStyleObject(schema.value?.wrapperStyleText));

const resolvedOptions = computed<FormFieldPreviewOption[]>(() => {
  if (schema.value?.options?.length) {
    return schema.value.options;
  }
  if (schema.value?.dynamicOptionsHint) {
    return [
      { label: `${props.field.label} A`, value: 'a', dynamic: true },
      { label: `${props.field.label} B`, value: 'b', dynamic: true },
    ];
  }
  return [];
});

const treeOptions = computed(() => [
  {
    title: `${props.field.label} A`,
    value: 'a',
    key: 'a',
    children: [
      {
        title: `${props.field.label} B`,
        value: 'b',
        key: 'b',
      },
    ],
  },
]);

const controlStyle = computed(() => {
  const styleObject = toStyleObject(schema.value?.styleText);
  const widthSensitive = new Set(['a-select', 'a-tree-select', 'a-input-number', 'a-date-picker', 'a-time-picker', 'a-range-picker']);
  if (schema.value?.component && widthSensitive.has(schema.value.component) && !styleObject.width) {
    styleObject.width = '100%';
  }
  return styleObject;
});

const textareaRows = computed(() => {
  if (schema.value?.rows) {
    return schema.value.rows;
  }
  return Math.max(2, (props.height || props.field.layout?.h || 1) * 2);
});

const switchChecked = computed(() => Boolean(schema.value?.checkedChildren));

const resolvedRenderer = computed(() => {
  const previewSchema = schema.value;
  const componentName = previewSchema?.component || props.field.controlType || 'a-input';

  if (componentName === 'StandardInput') {
    return () =>
      h(StandardInput, {
        value: '',
        placeholder: placeholderText.value,
        class: previewSchema?.className || undefined,
        variant: (previewSchema?.variant as 'default' | 'grey' | 'glass' | undefined) || 'default',
        noMargin: previewSchema?.noMargin ?? true,
        allowClear: previewSchema?.allowClear,
        width: typeof controlStyle.value.width === 'string' ? controlStyle.value.width : '100%',
      });
  }

  if (componentName === 'a-select') {
    return () =>
      h(
        ASelect,
        {
          placeholder: placeholderText.value,
          class: previewSchema?.className || undefined,
          style: controlStyle.value,
          showSearch: previewSchema?.showSearch,
          allowClear: previewSchema?.allowClear,
        },
        () =>
          resolvedOptions.value.map((option, index) =>
              h(
                ASelectOption,
                { key: `${option.value ?? index}`, value: option.value ?? index },
                () => {
                  const icon = option.icon ? previewIconMap[option.icon] : null;
                  if (icon) {
                    return [h(icon as never, { style: 'margin-right: 8px;' }), option.label];
                  }
                  return option.label;
              },
            ),
          ),
      );
  }

  if (componentName === 'a-tree-select') {
    return () =>
      h(ATreeSelect, {
        placeholder: placeholderText.value,
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
        allowClear: previewSchema?.allowClear,
        showSearch: previewSchema?.showSearch,
        treeDefaultExpandAll: previewSchema?.treeDefaultExpandAll,
        treeData: treeOptions.value,
        fieldNames: { label: 'title', value: 'value', children: 'children' },
      });
  }

  if (componentName === 'a-input-number') {
    return () =>
      h(AInputNumber, {
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
        placeholder: placeholderText.value,
        min: previewSchema?.min,
        max: previewSchema?.max,
        step: previewSchema?.step,
        controls: previewSchema?.controls,
      });
  }

  if (componentName === 'a-radio-group') {
    return () =>
      h(
        ARadioGroup,
        { class: previewSchema?.className || undefined },
        () =>
          resolvedOptions.value.map((option, index) =>
            previewSchema?.radioStyle === 'button'
              ? h(ARadioButton, { key: `${option.value ?? index}`, value: option.value ?? index }, () => option.label)
              : h(ARadio, { key: `${option.value ?? index}`, value: option.value ?? index }, () => option.label),
          ),
      );
  }

  if (componentName === 'a-checkbox-group') {
    return () =>
      h(
        ACheckboxGroup,
        { class: previewSchema?.className || undefined },
        () => resolvedOptions.value.map((option, index) => h(ACheckbox, { key: `${option.value ?? index}`, value: option.value ?? index }, () => option.label)),
      );
  }

  if (componentName === 'a-switch') {
    return () =>
      h(ASwitch, {
        class: previewSchema?.className || undefined,
        checked: switchChecked.value,
        checkedChildren: previewSchema?.checkedChildren || undefined,
        unCheckedChildren: previewSchema?.uncheckedChildren || undefined,
      });
  }

  if (componentName === 'a-date-picker') {
    return () =>
      h(ADatePicker, {
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
        placeholder: placeholderText.value,
        showTime: previewSchema?.showTime,
        valueFormat: previewSchema?.valueFormat || undefined,
      });
  }

  if (componentName === 'a-time-picker') {
    return () =>
      h(ATimePicker, {
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
        placeholder: placeholderText.value,
        valueFormat: previewSchema?.valueFormat || undefined,
      });
  }

  if (componentName === 'a-range-picker') {
    return () =>
      h(ARangePicker, {
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
      });
  }

  if (componentName === 'a-textarea') {
    return () =>
      h(ATextarea, {
        class: previewSchema?.className || undefined,
        style: controlStyle.value,
        placeholder: placeholderText.value,
        rows: textareaRows.value,
      });
  }

  return () =>
    h(AInput, {
      class: previewSchema?.className || undefined,
      style: controlStyle.value,
      placeholder: placeholderText.value,
    });
});

function toStyleObject(styleText?: string) {
  const styleObject: Record<string, string> = {};
  String(styleText || '')
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((declaration) => {
      const [rawKey, ...valueParts] = declaration.split(':');
      if (!rawKey || !valueParts.length) {
        return;
      }
      const key = rawKey
        .trim()
        .replace(/-([a-z])/g, (_, letter: string) => letter.toUpperCase());
      styleObject[key] = valueParts.join(':').trim();
    });
  return styleObject;
}
</script>

<style scoped>
.form-field-preview-renderer__wrapper {
  width: 100%;
}
</style>
