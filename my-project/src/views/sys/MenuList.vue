<template>
  <div class="menu-list-page">
    <!-- 搜索栏 -->
    <GlassCard variant="search">
       <a-form layout="inline" style="padding-top: 0px;">
           <div style="margin-left: 14px;">
               <div style="color:#867E7E;">菜单名称</div>
               <StandardInput 
                  v-model:value="searchText" 
                  placeholder="请输入菜单标题" 
                  class="input-1" 
                  variant="grey"
                  @keydown.enter="loadData" 
               />
           </div>
           <a-form-item style="margin-left: 40px; margin-top: 30px;">
               <StandardButton type="search" icon="search" @click="loadData">搜索</StandardButton>
               <StandardButton type="reset" icon="reload" @click="searchText = ''; loadData()">重置</StandardButton>
           </a-form-item>
       </a-form>
    </GlassCard>

    <!-- 表格区域 -->
    <GlassCard variant="table">
        <a-space class="menu-table-toolbar" style="margin-bottom: 20px;">
            <StandardButton type="add" icon="plus" @click="handleAdd(null)">
                新增顶级菜单
            </StandardButton>
            <a-button class="menu-toolbar-button" @click="expandAll" style="margin-left: 10px;">
                <MenuUnfoldOutlined /> 全部展开
            </a-button>
            <a-button class="menu-toolbar-button" @click="collapseAll" style="margin-left: 10px;">
                <MenuFoldOutlined /> 全部折叠
            </a-button>
        </a-space>

        <!-- 树形表格 -->
        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="false"
            rowKey="id"
            :scroll="{ x: 1000 }"
            v-model:expandedRowKeys="expandedRowKeys"
        >
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'icon'">
                    <!-- 使用 iconMap 动态渲染图标 -->
                    <component :is="iconMap[record.icon]" v-if="record.icon && iconMap[record.icon]" style="font-size: 16px;"/>
                    <span v-else>{{ record.icon || '-' }}</span>
                </template>

                 <template v-if="column.key === 'componentStyle'">
                     <a-tag v-if="record.componentStyle === 'glass'" class="menu-pill menu-pill--glass">透明玻璃</a-tag>
                     <a-tag v-else-if="record.componentStyle === 'default'" class="menu-pill menu-pill--default">默认</a-tag>
                     <span v-else class="menu-muted-text">继承父级</span>
                 </template>

                 <template v-if="column.key === 'roles'">
                     <a-tag
                        v-for="role in splitRoles(record.roles)"
                        :key="role"
                        :class="['menu-pill', 'menu-pill--role', `menu-pill--${role.toLowerCase()}`]"
                     >
                         {{ formatRoleLabel(role) }}
                     </a-tag>
                 </template>

                <template v-if="column.key === 'action'">
                    <a-space class="menu-action-group" :size="8">
                        <a-button type="text" class="table-glass-action table-glass-action--primary" @click="handleAdd(record)">新增子菜单</a-button>
                        <a-button type="text" class="table-glass-action table-glass-action--accent" @click="handleEdit(record)">编辑</a-button>
                        <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                            <a-button type="text" class="table-glass-action table-glass-action--danger">删除</a-button>
                        </a-popconfirm>
                    </a-space>
                </template>
            </template>
        </StandardTable>
    </GlassCard>

    <!-- 新增/编辑 弹窗 -->
    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
         <a-form-item label="父级菜单">
             <!-- 使用 TreeSelect 实现树形选择 -->
             <a-tree-select
                v-model:value="formState.parentId"
                class="modal-input-unified"
                :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                :tree-data="treeData"
                placeholder="请选择父级菜单 (留空则为顶级)"
                tree-default-expand-all
                allow-clear
                :field-names="{ children: 'children', label: 'title', value: 'id' }"
             >
             </a-tree-select>
         </a-form-item>

         <a-form-item label="菜单标题">
             <StandardInput v-model:value="formState.title" placeholder="如: 用户管理" variant="grey" class="modal-input-unified" />
         </a-form-item>

         <a-form-item label="路由Name">
             <StandardInput v-model:value="formState.name" placeholder="如: UserList (对应前端路由配置)" variant="grey" class="modal-input-unified" />
         </a-form-item>
         
         <a-form-item label="路由Path">
             <StandardInput v-model:value="formState.path" placeholder="如: /user 或 /sys/menu" variant="grey" class="modal-input-unified" />
         </a-form-item>

         <a-form-item label="组件路径">
             <StandardInput v-model:value="formState.component" placeholder="如: gym/UserList (src/views下的路径)" variant="grey" class="modal-input-unified" />
         </a-form-item>

         <a-form-item label="图标">
             <!-- 图标选择器 -->
             <a-select
                v-model:value="formState.icon"
                show-search
                placeholder="请选择图标"
                class="modal-input-unified"
                :filter-option="filterIconOption"
             >
                <a-select-option v-for="(comp, name) in iconMap" :key="name" :value="name">
                    <component :is="comp" style="margin-right: 8px;" />
                    {{ name }}
                </a-select-option>
             </a-select>
         </a-form-item>
         
         <!-- 新增：表格样式配置 -->
         <a-form-item label="表格样式" :extra="isGlobalTraditional ? '经典风格下此选项已禁用' : '子菜单优先于父菜单配置'">
             <a-tooltip :title="isGlobalTraditional ? '当前为经典风格，玻璃效果已全局关闭' : ''">
               <a-radio-group v-model:value="formState.componentStyle" :disabled="isGlobalTraditional">
                <a-radio-button :value="null">继承</a-radio-button>
                <a-radio-button value="glass">透明玻璃</a-radio-button>
                <a-radio-button value="default">默认</a-radio-button>
               </a-radio-group>
             </a-tooltip>
         </a-form-item>

         <a-form-item label="排序">
             <a-input-number v-model:value="formState.sort" class="modal-input-unified" />
         </a-form-item>

         <a-form-item label="权限角色">
              <a-checkbox-group v-model:value="selectedRoles">
                  <a-checkbox value="ADMIN">管理员</a-checkbox>
                  <a-checkbox value="STAFF">员工</a-checkbox>
                  <a-checkbox value="COACH">教练</a-checkbox>
              </a-checkbox-group>
         </a-form-item>
      </a-form>
    </StandardModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message } from 'ant-design-vue';
import { 
    UserOutlined, SettingOutlined, AppstoreOutlined, EnvironmentOutlined,
    ToolOutlined, CalendarOutlined, TeamOutlined, ScheduleOutlined,
    UserSwitchOutlined, WarningOutlined, FolderOutlined, FileOutlined,
    HomeOutlined, MenuUnfoldOutlined, MenuFoldOutlined
} from '@ant-design/icons-vue';
import request from '@/request';
import { usePageStyle } from '@/hooks/usePageStyle';
import { useStore } from 'vuex';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';

// 图标映射表
const iconMap: Record<string, any> = {
  AppstoreOutlined,
  HomeOutlined,
  EnvironmentOutlined,
  ToolOutlined,
  CalendarOutlined,
  TeamOutlined,
  ScheduleOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WarningOutlined,
  SettingOutlined,
  FolderOutlined,
  FileOutlined
};

interface Menu {
  id: number;
  parentId: number | null;
  title: string;
  name: string;
  path: string;
  component: string;
  icon: string;
  sort: number;
  roles: string;
  componentStyle?: string | null; // null=inherit, glass, default
  children?: Menu[];
}

const searchText = ref('');
const tableData = ref<Menu[]>([]);
const modalVisible = ref(false);
const modalTitle = ref('');
const selectedRoles = ref<string[]>([]);
const expandedRowKeys = ref<number[]>([]);
const allIds = ref<number[]>([]);

// Page Style Hook
const { currentStyle, loadMenuConfig } = usePageStyle();

// Global theme
const __store = useStore();
const isGlobalTraditional = computed(() => __store.state.themeSettings?.styleMode === 'traditional');

const expandAll = () => {
    expandedRowKeys.value = [...allIds.value];
};

const collapseAll = () => {
    expandedRowKeys.value = [];
};

const formState = reactive<Menu>({
    id: 0,
    parentId: null,
    title: '',
    name: '',
    path: '',
    component: '',
    icon: '',
    sort: 0,
    roles: '',
    componentStyle: null
});

const columns = [
  { title: '菜单标题', dataIndex: 'title', key: 'title', width: 200 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 80, align: 'center' },
  { title: '路由路径', dataIndex: 'path', key: 'path' },
  { title: '组件路径', dataIndex: 'component', key: 'component' },
  { title: '样式配置', dataIndex: 'componentStyle', key: 'componentStyle', width: 120 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '权限', dataIndex: 'roles', key: 'roles' },
  { title: '操作', key: 'action', width: 250, fixed: 'right' },
];

// 构建 TreeSelect 数据源 (需要 deep copy 避免影响 tableData)
const treeData = computed(() => {
    return tableData.value; 
});

// 图标搜索过滤
const filterIconOption = (input: string, option: any) => {
  return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

const splitRoles = (roles?: string) => {
    return roles ? roles.split(',').filter(Boolean) : [];
};

const formatRoleLabel = (role: string) => {
    if (role === 'ADMIN') return '管理员';
    if (role === 'STAFF') return '员工';
    if (role === 'COACH') return '教练';
    if (role === 'MEMBER') return '会员';
    return role;
};

// 将扁平列表转换为树形结构
const buildTree = (list: Menu[]) => {
    // 先按 sort 排序
    list.sort((a, b) => (a.sort || 0) - (b.sort || 0));

    const temp: any = {};
    const tree: Menu[] = [];
    list.forEach(item => {
        temp[item.id] = { ...item, children: [] };
    });
    
    list.forEach(item => {
        if (item.parentId && temp[item.parentId]) {
            temp[item.parentId].children.push(temp[item.id]);
        } else {
            tree.push(temp[item.id]);
        }
    });
    
    const cleanChildren = (nodes: Menu[]) => {
        nodes.forEach(node => {
            if (node.children && node.children.length === 0) {
                delete node.children;
            } else if (node.children) {
                cleanChildren(node.children);
            }
        });
    };
    cleanChildren(tree);
    return tree;
};

const loadData = () => {
    request.get("/menu/list").then((res: any) => {
        if(res.code === 200) {
            const fullList = res.data;
            
            // 2. Filter for view
            let list = fullList;
            if(searchText.value) {
                const keyword = searchText.value.toLowerCase();
                list = list.filter((item: Menu) => item.title.toLowerCase().includes(keyword));
            }
            
            // 3. Build view tree
            const tree = buildTree(list);
            tableData.value = tree;
            
            // 4. Default Expansion Logic
            if (searchText.value) {
                 allIds.value = list.map((item: Menu) => item.id);
                 expandedRowKeys.value = [...allIds.value];
            } else {
                 const rootNode = list.find((item: Menu) => item.id === 999 || item.title === '系统主菜单');
                 if (rootNode) {
                     expandedRowKeys.value = [rootNode.id];
                 } else {
                     expandedRowKeys.value = [];
                 }
                 allIds.value = list.map((item: Menu) => item.id);
            }
        }
    });
};

const handleAdd = (parent: Menu | null) => {
    modalTitle.value = parent ? `新增子菜单 [${parent.title}]` : '新增顶级菜单';
    formState.id = 0;
    formState.parentId = parent ? parent.id : null; 
    
    formState.title = '';
    formState.name = '';
    formState.path = '';
    formState.component = '';
    formState.icon = '';
    formState.sort = 0;
    selectedRoles.value = ['ADMIN']; 
    modalVisible.value = true;
};

const handleEdit = (record: Menu) => {
    modalTitle.value = '编辑菜单';
    Object.assign(formState, { ...record });
    formState.componentStyle = record.componentStyle || null;
    if(formState.parentId === 0) formState.parentId = null;

    let roles = record.roles ? record.roles.split(',') : [];
    roles = roles.map(r => r === 'MEMBER' ? 'COACH' : r);
    selectedRoles.value = roles;
    modalVisible.value = true;
};

const handleDelete = (id: number) => {
     request.delete("/menu/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

const handleModalOk = () => {
    formState.roles = selectedRoles.value.join(',');
    
    request.post("/menu", formState).then((res: any) => {
        if(res.code === 200) {
            message.success("保存成功");
            modalVisible.value = false;
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

onMounted(() => {
    loadMenuConfig(); // Load config for page style
    loadData();
});
</script>

<style scoped>
 :deep(.ant-input-number-input) { height: 40px; }
 :deep(.ant-input) { width: 260px; }

 
 :deep(.ant-select-selector), :deep(.ant-tree-select-selector) { 
     background-color: #F7F5F5 !important; 
     border: none !important;
     height: 40px !important;  
     align-items: center; 
     border-radius: 8px !important;
 }
 :deep(.ant-select-selection-search-input) { height: 40px !important; }

 /* Fix Vertical Alignment for Form Items using standard row centering */
 :deep(.ant-form-item .ant-row) {
     align-items: center;
 }
 :deep(.ant-form-item-control-input) {
     min-height: 40px;
 }

 /* Modal Button & Title Adjustments */
 :deep(.ant-modal-close) {
     top: 5px !important;    /* Move X down (inwards) */
     right: 5px !important;  /* Move X left (inwards) */
 }
 :deep(.ant-modal-header) {
     padding: 10px 20px !important;  /* Move Title closer to top-left (reduce top/left padding slightly) */
 }
 
 /* Fix Radio Group Alignment (Push down due to 'extra' text affecting center) */
 :deep(.ant-radio-group) {
     position: relative;
     top: 8px;
 }

 .menu-table-toolbar {
     width: 100%;
     flex-wrap: wrap;
     row-gap: 10px;
 }

 .menu-toolbar-button {
     height: 34px;
     padding: 0 16px;
     border: 1px solid rgba(15, 23, 42, 0.08) !important;
     border-radius: 999px !important;
     background: rgba(255, 255, 255, 0.58) !important;
     color: #4b5563 !important;
     box-shadow: none !important;
 }

 .menu-toolbar-button:hover {
     background: rgba(255, 255, 255, 0.8) !important;
     border-color: rgba(15, 23, 42, 0.12) !important;
     color: #1f2937 !important;
 }

 .menu-muted-text {
     color: rgba(90, 97, 108, 0.7);
 }

 .menu-pill {
     margin-inline-end: 8px;
     padding: 2px 10px;
     border-radius: 999px;
     border: 1px solid rgba(255, 255, 255, 0.45) !important;
     background: rgba(255, 255, 255, 0.34) !important;
     color: #4b5563 !important;
     box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22);
 }

 .menu-pill--glass {
     color: #0f8f9a !important;
     background: rgba(45, 212, 191, 0.16) !important;
     border-color: rgba(45, 212, 191, 0.24) !important;
 }

 .menu-pill--default {
     color: #b7791f !important;
     background: rgba(250, 204, 21, 0.14) !important;
     border-color: rgba(250, 204, 21, 0.24) !important;
 }

 .menu-pill--role.menu-pill--admin {
     color: #2563eb !important;
     background: rgba(59, 130, 246, 0.1) !important;
     border-color: rgba(59, 130, 246, 0.2) !important;
 }

 .menu-pill--role.menu-pill--staff {
     color: #1d4ed8 !important;
     background: rgba(96, 165, 250, 0.12) !important;
     border-color: rgba(96, 165, 250, 0.2) !important;
 }

 .menu-pill--role.menu-pill--coach,
 .menu-pill--role.menu-pill--member {
     color: #0369a1 !important;
     background: rgba(56, 189, 248, 0.12) !important;
     border-color: rgba(56, 189, 248, 0.22) !important;
 }

 .menu-action-group {
     flex-wrap: wrap;
 }

 .table-glass-action {
     height: 30px;
     padding: 0 12px;
     border-radius: 999px;
     border: 1px solid transparent !important;
     background: rgba(255, 255, 255, 0.26) !important;
     box-shadow: none !important;
 }

 .table-glass-action--primary {
     color: #2563eb !important;
     background: rgba(59, 130, 246, 0.08) !important;
     border-color: rgba(59, 130, 246, 0.16) !important;
 }

 .table-glass-action--accent {
     color: #b7791f !important;
     background: rgba(245, 158, 11, 0.1) !important;
     border-color: rgba(245, 158, 11, 0.18) !important;
 }

 .table-glass-action--danger {
     color: #dc2626 !important;
     background: rgba(248, 113, 113, 0.1) !important;
     border-color: rgba(248, 113, 113, 0.18) !important;
 }

 .table-glass-action:hover {
     transform: translateY(-1px);
     background: rgba(255, 255, 255, 0.42) !important;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-tbody > tr > td) {
     vertical-align: middle;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-row-expand-icon) {
     background: rgba(255, 255, 255, 0.36) !important;
     border-color: rgba(15, 23, 42, 0.08) !important;
     color: #475569 !important;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-row-expand-icon:hover) {
     background: rgba(255, 255, 255, 0.56) !important;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-cell-fix-right) {
     background: rgba(255, 255, 255, 0.12) !important;
     backdrop-filter: blur(10px);
     -webkit-backdrop-filter: blur(10px);
 }
</style>
