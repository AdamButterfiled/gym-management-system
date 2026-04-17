<template>
  <div class="standard-modal-wrapper">
    <a-modal
      :open="visible"
      :title="title"
      :width="width"
      :bodyStyle="mergedBodyStyle"
      :mask="mask"
      :maskStyle="mergedMaskStyle"
      :wrapClassName="mergedWrapClassName"
      centered
      v-bind="$attrs"
      @update:open="val => emit('update:visible', val)"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <slot />
      <template #footer v-if="$slots.footer">
        <slot name="footer" />
      </template>
      <template #footer v-else>
        <div class="standard-modal-footer">
          <StandardButton type="default" @click="handleCancel">
            {{ cancelText }}
          </StandardButton>
          <StandardButton :type="okButtonType" @click="handleOk">
            {{ okText }}
          </StandardButton>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue';
import StandardButton from '@/components/common/StandardButton.vue';

const attrs = useAttrs();

defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '',
  },
  width: {
    type: [String, Number],
    default: '600px',
  },
  mask: {
    type: Boolean,
    default: true,
  },
  okText: {
    type: String,
    default: '确定',
  },
  cancelText: {
    type: String,
    default: '取消',
  },
  okButtonType: {
    type: String,
    default: 'primary',
  },
});

const emit = defineEmits(['update:visible', 'ok', 'cancel']);

const mergedWrapClassName = computed(() => {
  const customWrapClassName = attrs.wrapClassName || attrs['wrap-class-name'];
  return ['mono-modal', 'standard-modal-shell', customWrapClassName].filter(Boolean).join(' ');
});

const mergedBodyStyle = computed(() => ({
  maxHeight: 'calc(100vh - 220px)',
  overflowY: 'auto',
  ...(((attrs.bodyStyle || attrs['body-style']) as Record<string, unknown> | undefined) || {}),
}));

const mergedMaskStyle = computed(() => ({
  background: 'rgba(248, 248, 248, 0.82)',
  backdropFilter: 'none',
  WebkitBackdropFilter: 'none',
  ...(((attrs.maskStyle || attrs['mask-style']) as Record<string, unknown> | undefined) || {}),
}));

const handleOk = () => {
  emit('ok');
};

const handleCancel = () => {
  emit('update:visible', false);
  emit('cancel');
};
</script>

<style scoped>
.standard-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

:deep(.standard-modal-shell .ant-modal) {
  padding-bottom: 24px;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-content) {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  box-shadow: none;
  overflow: hidden;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-header) {
  padding: 28px 28px 0;
  background: transparent;
  border-bottom: none;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-title) {
  color: #111111;
  font-size: 20px;
  font-weight: 400;
  line-height: 1.3;
  letter-spacing: 0;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-body) {
  padding: 20px 28px 0;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-footer) {
  padding: 24px 28px 28px;
  border-top: none;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-close) {
  top: 22px;
  right: 22px;
  width: 32px;
  height: 32px;
  border-radius: var(--mono-radius-sm);
  color: rgba(17, 17, 17, 0.42);
  transition: background-color 0.2s ease, color 0.2s ease;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-close:hover) {
  background: rgba(17, 17, 17, 0.04);
  color: rgba(17, 17, 17, 0.72);
}

:deep(.standard-modal-shell .ant-modal .ant-modal-close-x) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.standard-modal-shell .ant-modal .ant-modal-close .ant-modal-close-icon) {
  font-size: 16px;
}

:deep(.standard-modal-shell .ant-form-item .ant-row) {
  align-items: center !important;
}

:deep(.standard-modal-footer .std-button) {
  min-width: 108px;
  height: 42px;
  padding: 0 20px;
  border-radius: var(--mono-radius-sm);
  font-weight: 600;
}

@media (max-width: 768px) {
  :deep(.standard-modal-shell .ant-modal) {
    max-width: calc(100vw - 24px);
    margin: 12px auto;
  }

  :deep(.standard-modal-shell .ant-modal .ant-modal-header) {
    padding: 24px 20px 0;
  }

  :deep(.standard-modal-shell .ant-modal .ant-modal-body) {
    padding: 18px 20px 0;
  }

  :deep(.standard-modal-shell .ant-modal .ant-modal-footer) {
    padding: 20px 20px 22px;
  }

  .standard-modal-footer {
    gap: 10px;
  }

  :deep(.standard-modal-footer .std-button) {
    flex: 1 1 0;
    min-width: 0;
  }
}
</style>
