<template>
  <a-button 
    :class="buttonClass" 
    :loading="loading" 
    @click="$emit('click')"
  >
      <slot name="icon">
          <component :is="iconComponent" v-if="iconComponent" />
      </slot>
      <span style="margin-left: 4px;"><slot /></span>
  </a-button>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { SearchOutlined, ReloadOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps<{
    type?: 'search' | 'reset' | 'add' | 'delete' | 'default';
    loading?: boolean;
    icon?: 'search' | 'reload' | 'plus' | 'delete';
}>();
const emit = defineEmits(['click'])

const iconComponent = computed(() => {
    switch(props.icon) {
        case 'search': return SearchOutlined;
        case 'reload': return ReloadOutlined;
        case 'plus': return PlusOutlined;
        case 'delete': return DeleteOutlined;
        default: return null;
    }
});

const buttonClass = computed(() => {
    return {
        'search-button': props.type === 'search',
        'reset-button': props.type === 'reset',
        'add-button': props.type === 'add',
        'delete-button': props.type === 'delete'
    }
});
</script>

<style scoped>
/* Force styles with !important to override Ant Design defaults where necessary to match MenuList EXACTLY */
:deep(.ant-btn) { display: inline-flex; align-items: center; justify-content: center; }

.search-button { 
    background-color: #f7b500 !important; color: white !important; border-radius: 24px; width: 85px; height: 39px; border: none !important;
}
.reset-button { 
    background-color: #f3f3f3 !important; color: #333 !important; width: 85px; height: 39px; border-radius: 24px; margin-left: 17px; border: none !important;
}
.add-button {
    background-color: #00b96b !important; color: white !important; border-radius: 24px; height: 34px; box-shadow: 0 2px 0 rgba(0,0,0,0.045); border: none !important;
}
.add-button:hover { background-color: #009456 !important; }

.delete-button {
    background-color: #ff4d4f !important; color: white !important; border-radius: 24px; height: 36px; margin-left: 10px; border: none !important;
}
.delete-button:hover { background-color: #d9363e !important; }
</style>
