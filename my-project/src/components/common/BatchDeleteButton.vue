<template>
  <div class="batch-delete-wrapper">
    <transition name="slide-width">
        <a-button 
           v-if="active"
           type="text" 
           size="small" 
           class="cancel-delete-btn"
           @click="toggleMode"
       >
           取消
       </a-button>
    </transition>

    <a-popconfirm 
       :title="popconfirmTitle || '确定删除选中项吗?'" 
       @confirm="handleConfirm"
       :disabled="!active || count === 0"
       okText="删除"
       cancelText="取消"
       :okButtonProps="{ shape: 'round', size: 'small', danger: true }"
       :cancelButtonProps="{ shape: 'round', size: 'small' }"
   >
        <a-tooltip :title="active ? (count > 0 ? '点击删除选中项' : '退出删除模式') : '批量删除管理'">
            <a-button 
               type="text"
               size="small" 
               class="trash-btn"
               :class="{ 'active-jitter': active }"
               @click="handleClick"
            >
               <DeleteOutlined :style="{ color: active ? '#ff4d4f' : 'inherit' }" />
            </a-button>
       </a-tooltip>
   </a-popconfirm>

   <transition name="slide-width">
       <span v-if="active && count > 0" class="delete-count">
           已选中 {{ count }} 项
       </span>
   </transition>
  </div>
</template>

<script setup lang="ts">
import { DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps<{
    active: boolean;      // Is delete mode active?
    count: number;        // Number of selected items
    popconfirmTitle?: string;
}>();

const emit = defineEmits<{
    (e: 'update:active', value: boolean): void;
    (e: 'delete'): void;
}>();

const toggleMode = () => {
    emit('update:active', !props.active);
};

const handleClick = () => {
    if (!props.active) {
        // Activate mode
        emit('update:active', true);
    } else if (props.count === 0) {
        // Deactivate mode if nothing selected
         emit('update:active', false);
    }
    // If active and count > 0, popconfirm handles the click
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
     min-width: 32px;
 }
 
 /* Jitter Animation */
 @keyframes jitter {
   0% { transform: rotate(0deg); }
   25% { transform: rotate(10deg); }
   50% { transform: rotate(0deg); }
   75% { transform: rotate(-10deg); }
   100% { transform: rotate(0deg); }
 }
 .active-jitter .anticon {
   display: inline-block;
   animation: jitter 0.3s ease-in-out infinite; 
 }

 .trash-btn { font-size: 16px; color: #666; transition: color 0.3s; }
 .trash-btn:hover { color: #333; }

 :global(.dark) .trash-btn { color: rgba(255, 255, 255, 0.45); }
 :global(.dark) .trash-btn:hover { color: rgba(255, 255, 255, 0.85); }

 .cancel-delete-btn { 
     margin-right: 8px; 
     color: #999;
     font-size: 12px;
     white-space: nowrap;
 }
 .delete-count { 
     margin-left: 4px; 
     font-size: 13px; 
     color: #ff4d4f; 
     font-weight: bold; 
     white-space: nowrap;
 }
 
 /* Slide Width Transition */
 .slide-width-enter-active, .slide-width-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
 }
 .slide-width-enter-from, .slide-width-leave-to {
  width: 0;
  opacity: 0;
  margin: 0;
 }
</style>
