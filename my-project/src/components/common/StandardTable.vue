<template>
  <div :class="containerClass">
    <a-table v-bind="$attrs">
        <template #bodyCell="scope">
            <slot name="bodyCell" v-bind="scope" />
        </template>
        <template #headerCell="scope" v-if="$slots.headerCell">
            <slot name="headerCell" v-bind="scope" />
        </template>
        <template #summary v-if="$slots.summary">
            <slot name="summary" />
        </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    configStyle?: 'default' | 'glass';
}>();

const containerClass = computed(() => {
    return {
        'default-yellow-mode': props.configStyle !== 'glass',
        'transparent-glass-mode': props.configStyle === 'glass'
    };
});
</script>

<style scoped>
 :deep(.default-yellow-mode .ant-table-header) { background-color: #F8F6EF !important; }
 /* Fix Scoped CSS: .default-yellow-mode is on the root, so it should be outside :deep */
 .default-yellow-mode :deep(.ant-table-thead > tr > th) {
    background-color: #F8F6EF !important; 
    color: #867E7E !important;            
    font-size: 13.6px !important;
    text-align: center !important;        
    border-radius: 0px; 
    border-bottom: 0.05px solid #F4EFEE !important;
    border-top: none !important;
    border-left: none !important;
    border-right: none !important;
 }
 /* Remove Ant Design Header Column Separators */
 .default-yellow-mode :deep(.ant-table-thead > tr > th::before) {
    display: none !important;
    width: 0px !important;
    background-color: transparent !important;
 }

 :deep(.default-yellow-mode .ant-table-tbody > tr > td) {
    text-align: center !important;        
    border-bottom: 0.05px solid #F4EFEE !important;
    border-top: none !important;
    border-left: none !important;
    border-right: none !important;
 }
 :deep(.default-yellow-mode .ant-table-tbody) {
    background-color: #FDFCF9 !important;
 }
 :deep(.default-yellow-mode .ant-table-tbody > tr:nth-child(even) > td) {
    background-color: #FDFCF9 !important;
 }
 :deep(.default-yellow-mode .ant-table-tbody > tr:nth-child(odd) > td) {
    background-color: #FFFFFF !important;
 }
 :deep(.default-yellow-mode .ant-table-tbody > tr:hover > td) {
    background-color: #faf7f0 !important;
 }
 :deep(.default-yellow-mode .ant-table-container) {
    border: 0.8px solid #F4EFEE !important;
    border-top: none !important; /* Top border often dupes header */
 }

 /* Remove tree connecting lines if any */
 :deep(.ant-table-row-indent + .ant-table-row-expand-icon) {
    margin-right: 8px; 
 }
 :deep(.ant-table-row-indent.indent-level-1) {
    border-right: none !important; 
 }
 
 /* Ensure no vertical borders on cells globally for this mode */
 :deep(.default-yellow-mode td), :deep(.default-yellow-mode th) {
    border-right: none !important;
    border-left: none !important;
 }
</style>
