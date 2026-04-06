<template>
  <div class="glass-container" :style="customStyle">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    variant?: 'search' | 'table';
}>();

const customStyle = computed(() => {
    if (props.variant === 'search') {
        return { marginTop: '0', minHeight: '100px', height: 'auto', paddingTop: '17px' }; 
    }
    if (props.variant === 'table') {
        return { paddingBottom: '20px', minHeight: 'auto' };
    }
    return {};
});
</script>

<style scoped>
.glass-container {
     padding: 20px;
     border-radius: 19px;
     background-color: #FDFDFD;
     backdrop-filter: blur(28px);
     -webkit-backdrop-filter: blur(8px);
     border: 0.1px solid #d9d9d9;
     margin-top: 20px;
     box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1);
 }

 :global(.dark) .glass-container {
     background-color: rgba(40, 40, 40, 0.7);
     border-color: rgba(255, 255, 255, 0.08);
     box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.3);
 }
</style>
