<template>
  <div class="standard-modal-wrapper">
    <a-modal
      :open="visible"
      :title="title"
      :width="width"
      wrapClassName="glass-modal"
      centered
      :maskStyle="{ backgroundColor: 'rgba(0, 0, 0, 0.15)' }"
      v-bind="$attrs"
      @update:open="val => emit('update:visible', val)"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <slot />
      <template #footer v-if="$slots.footer">
        <slot name="footer" />
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable no-undef */

defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  width: {
    type: [String, Number],
    default: '600px'
  }
});

const emit = defineEmits(['update:visible', 'ok', 'cancel']);

const handleOk = () => {
  emit('ok');
};

const handleCancel = () => {
  emit('update:visible', false);
  emit('cancel');
};
</script>

<style scoped>
/* Ensure consistent form item alignment within this modal */
:deep(.ant-form-item .ant-row) {
    align-items: center !important;
}
</style>
