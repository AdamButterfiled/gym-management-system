<template>
  <div class="batch-delete-wrapper">
    <a-tooltip v-if="!active" title="批量删除管理">
      <StandardButton
        type="ghost"
        size="icon"
        icon="delete"
        class="trash-btn"
        aria-label="批量删除管理"
        @click="toggleMode"
      />
    </a-tooltip>

    <template v-else>
      <StandardButton
        type="ghost"
        size="sm"
        class="cancel-delete-btn"
        @click="toggleMode"
      >
        取消
      </StandardButton>

      <a-popconfirm
        :title="popconfirmTitle || '确定删除选中项吗?'"
        @confirm="handleConfirm"
        :disabled="count === 0"
        okText="删除"
        cancelText="取消"
        :okButtonProps="{ shape: 'round', size: 'small', type: 'primary' }"
        :cancelButtonProps="{ shape: 'round', size: 'small' }"
      >
        <StandardButton
          type="primary"
          size="sm"
          icon="delete"
          class="confirm-delete-btn"
          :disabled="count === 0"
        >
          <span>全部删除</span>
        </StandardButton>
      </a-popconfirm>
    </template>
  </div>
</template>

<script setup lang="ts">
import StandardButton from '@/components/common/StandardButton.vue';

const props = defineProps<{
  active: boolean;
  count: number;
  popconfirmTitle?: string;
}>();

const emit = defineEmits<{
  (e: 'update:active', value: boolean): void;
  (e: 'delete'): void;
}>();

const toggleMode = () => {
  emit('update:active', !props.active);
};

const handleConfirm = () => {
  emit('delete');
};
</script>

<style scoped>
.batch-delete-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 42px;
}

.trash-btn,
.cancel-delete-btn,
.confirm-delete-btn {
  height: 42px;
  border: 1px solid var(--mono-control-border);
  border-radius: var(--mono-radius-pill);
  background: var(--mono-control-bg) !important;
  color: var(--mono-control-text-muted) !important;
  box-shadow: none !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.trash-btn {
  width: 42px;
}

.trash-btn:hover,
.cancel-delete-btn:hover {
  border-color: var(--mono-control-border-strong);
  background: var(--mono-control-bg-hover) !important;
  color: var(--mono-control-text) !important;
}

.cancel-delete-btn {
  padding: 0 14px;
}

.confirm-delete-btn {
  min-width: 122px;
  padding: 0 18px;
  border-color: var(--shad-primary-bg) !important;
  background: var(--shad-primary-bg) !important;
  color: var(--shad-primary-foreground) !important;
  gap: 8px;
}

.confirm-delete-btn:hover {
  border-color: var(--shad-primary-hover) !important;
  background: var(--shad-primary-hover) !important;
  color: var(--shad-primary-foreground) !important;
}

.confirm-delete-btn[disabled] {
  border-color: var(--mono-control-border) !important;
  background: var(--mono-control-bg-disabled) !important;
  color: var(--mono-control-text-disabled) !important;
}
</style>
