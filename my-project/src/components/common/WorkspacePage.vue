<template>
  <section :class="['workspace-shell', `workspace-shell--${variant}`]">
    <div class="workspace-header">
      <div class="title-group">
        <h1 class="page-title">{{ title }}</h1>
        <slot name="meta" />
      </div>

      <div v-if="$slots.actions && variant === 'default'" class="header-actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.filters && variant === 'default'" class="filter-row">
      <slot name="filters" />
    </div>

    <div v-if="variant === 'menu-list' && ($slots.filters || $slots.actions)" class="workspace-controls">
      <div v-if="$slots.filters" class="filter-row">
        <slot name="filters" />
      </div>

      <div v-if="$slots.actions" class="actions-row">
        <slot name="actions" />
      </div>
    </div>

    <div class="workspace-body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string;
  variant?: 'default' | 'menu-list';
}>(), {
  variant: 'default',
});
</script>

<style scoped>
.workspace-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.workspace-shell--menu-list {
  padding-top: 2px;
}

.workspace-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.title-group {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.page-title {
  margin: 0;
  color: var(--mono-text);
  font-size: var(--app-page-title-size);
  line-height: 0.98;
  font-weight: 600;
  letter-spacing: -0.03em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 0 0;
}

.workspace-body {
  padding-top: 24px;
}

.workspace-controls {
  width: 100%;
}

.workspace-shell--menu-list .workspace-header {
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 26px;
  width: 100%;
  margin-top: 0;
  margin-bottom: 40px;
  padding-top: 2px;
}

.workspace-shell--menu-list .title-group {
  gap: 0;
}

.workspace-shell--menu-list .page-title {
  display: inline-block;
  color: var(--mono-text);
  font-size: clamp(21px, 1.65vw, 26px);
  line-height: 1.06;
  font-weight: 700;
  letter-spacing: -0.01em;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  font-synthesis: none;
  transform: translate(-6px, -11px);
}

.workspace-shell--menu-list .workspace-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 48px;
  width: 100%;
  max-width: 100%;
  padding-bottom: 4px;
  position: relative;
  top: 10px;
}

.workspace-shell--menu-list .filter-row {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 100%;
  padding: 0;
  margin-top: 24px;
  margin-bottom: -24px;
  position: relative;
  top: -10px;
}

.workspace-shell--menu-list .actions-row {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 100%;
  flex-wrap: wrap;
  row-gap: 10px;
  column-gap: 12px;
  padding-left: 14px;
  margin-top: 33px;
}

.workspace-shell--menu-list .workspace-body {
  padding-top: 32px;
}

.workspace-shell--menu-list :deep(.table-search-toolbar--menu-list) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 14px !important;
  width: 100%;
}

.workspace-shell--menu-list .actions-row :deep(.std-button) {
  height: 36px;
  min-height: 36px;
  gap: 6px;
  --std-button-font-size: 13px;
  --std-button-label-shift: 0.5px;
}

.workspace-shell--menu-list .actions-row :deep(.std-button .std-button-icon),
.workspace-shell--menu-list .actions-row :deep(.std-button svg) {
  width: 14px;
  height: 14px;
}

@media (max-width: 960px) {
  .workspace-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .workspace-body {
    padding-top: 18px;
  }

  .workspace-shell--menu-list .workspace-header {
    gap: 18px;
    margin-bottom: 28px;
  }

  .workspace-shell--menu-list .page-title {
    transform: none;
  }

  .workspace-shell--menu-list .workspace-controls {
    gap: 18px;
    top: 0;
  }

  .workspace-shell--menu-list .filter-row {
    margin-top: 0;
    margin-bottom: 0;
    top: 0;
  }

  .workspace-shell--menu-list .actions-row {
    padding-left: 0;
    margin-top: 0;
  }

  .workspace-shell--menu-list .workspace-body {
    padding-top: 18px;
  }
}
</style>
