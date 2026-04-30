<template>
  <div class="menu-list-page">
    <div class="menu-page-header">
      <h1 class="menu-page-title">菜单权限管理</h1>
      <div class="menu-page-controls">
        <div class="menu-search-row">
          <StandardInput
            v-model:value="searchText"
            width="250px"
            placeholder="搜索菜单标题"
            class="menu-search-input"
            @pressEnter="loadData"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </StandardInput>
          <div class="menu-search-actions">
            <StandardButton
              type="search"
              icon="search"
              @click="loadData"
            >
              查询
            </StandardButton>
            <StandardButton
              type="reset"
              icon="reload"
              @click="resetSearch"
            >
              重置
            </StandardButton>
          </div>
        </div>

        <div class="menu-table-toolbar" role="toolbar" aria-label="菜单操作">
          <StandardButton
            type="add"
            class="menu-native-button menu-control-button menu-toolbar-button menu-toolbar-button--add menu-button-animated menu-button-animated--add"
            icon="plus"
            @click="handleAdd(null)"
          >
            新增顶级菜单
          </StandardButton>
          <StandardButton
            type="default"
            class="menu-native-button menu-control-button menu-toolbar-button menu-toolbar-button--toggle menu-button-animated menu-button-animated--toggle"
            :class="{ 'is-expanded': isAllExpanded }"
            :aria-label="isAllExpanded ? '全部折叠' : '全部展开'"
            @click="toggleExpandAll"
          >
            <template #icon>
              <component :is="isAllExpanded ? FoldVertical : UnfoldVertical" :size="16" class="menu-toggle-lucide" />
            </template>
            {{ isAllExpanded ? '全部折叠' : '全部展开' }}
          </StandardButton>
        </div>
      </div>
    </div>

    <GlassCard variant="table">
      <section class="workspace-subsection">
        <StandardTable
            class="openai-table"
            :configStyle="currentStyle"
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="false"
            rowKey="id"
            :scroll="{ x: 1380 }"
            v-model:expandedRowKeys="expandedRowKeys"
        >
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'icon'">
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
                    <a-space class="menu-action-group" :size="16">
                        <StandardButton type="link" size="sm" class="table-glass-action table-glass-action--primary" @click="handleAdd(record)">新增子菜单</StandardButton>
                        <StandardButton type="link" size="sm" class="table-glass-action table-glass-action--accent" @click="handleEdit(record)">编辑</StandardButton>
                        <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                            <StandardButton type="link" size="sm" danger class="table-glass-action table-glass-action--danger">删除</StandardButton>
                        </a-popconfirm>
                    </a-space>
                </template>
            </template>
        </StandardTable>
      </section>
    </GlassCard>

    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      :body-style="{ paddingTop: '30px', paddingBottom: '18px' }"
      @ok="handleModalOk"
    >
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        class="workspace-modal-form menu-config-form"
      >
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
         <a-form-item label="表格样式" :extra="isGlobalTraditional ? '经典风格下此选项已禁用，页面使用统一页壳外观' : '子菜单优先于父菜单配置'">
             <a-tooltip :title="isGlobalTraditional ? '当前为经典风格，表格将跟随统一的经典页面风格' : ''">
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
              <a-checkbox-group v-model:value="selectedRoles" data-field-key="roles">
                  <a-checkbox value="ADMIN">管理员</a-checkbox>
                  <a-checkbox value="STAFF">员工</a-checkbox>
                  <a-checkbox value="COACH">教练</a-checkbox>
                  <a-checkbox value="MEMBER">会员</a-checkbox>
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
    HomeOutlined, FormOutlined, SearchOutlined
} from '@ant-design/icons-vue';
import { FoldVertical, UnfoldVertical } from 'lucide-vue-next';
import request from '@/request';
import { usePageStyle } from '@/hooks/usePageStyle';
import { useStore } from 'vuex';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardInput from '@/components/common/StandardInput.vue';
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
  FileOutlined,
  FormOutlined
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
  permissionConfig?: string | null;
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
const isAllExpanded = computed(() => {
    if (allIds.value.length === 0) return false;
    return allIds.value.every(id => expandedRowKeys.value.includes(id));
});

const expandAll = () => {
    expandedRowKeys.value = [...allIds.value];
};

const collapseAll = () => {
    expandedRowKeys.value = [];
};

const toggleExpandAll = () => {
    if (isAllExpanded.value) {
        collapseAll();
        return;
    }

    expandAll();
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
    componentStyle: null,
    permissionConfig: null
});

const columns = [
  { title: '菜单标题', dataIndex: 'title', key: 'title', width: 280 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 72, align: 'center' },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 188 },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 198 },
  { title: '样式配置', dataIndex: 'componentStyle', key: 'componentStyle', width: 132, align: 'center' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 72, align: 'center' },
  { title: '权限', dataIndex: 'roles', key: 'roles', width: 220 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' },
];

// 构建 TreeSelect 数据源 (需要 deep copy 避免影响 tableData)
const treeData = computed(() => {
    return tableData.value; 
});

// 图标搜索过滤
const filterIconOption = (input: string, option: any) => {
  return String(option?.value || '').toLowerCase().includes(input.toLowerCase());
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
            let list = fullList;

            if (searchText.value) {
                const keyword = searchText.value.toLowerCase();
                list = list.filter((item: Menu) => item.title.toLowerCase().includes(keyword));
            }

            tableData.value = buildTree(list);
            allIds.value = list.map((item: Menu) => item.id);

            if (searchText.value) {
                expandedRowKeys.value = [...allIds.value];
            } else {
                const rootNode = list.find((item: Menu) => item.id === 999 || item.title === '系统主菜单');
                expandedRowKeys.value = rootNode ? [rootNode.id] : [];
            }
        }
    });
};

const resetSearch = () => {
    searchText.value = '';
    loadData();
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
    formState.permissionConfig = null;
    selectedRoles.value = ['ADMIN']; 
    modalVisible.value = true;
};

const handleEdit = (record: Menu) => {
    modalTitle.value = '编辑菜单';
    Object.assign(formState, { ...record });
    formState.componentStyle = record.componentStyle || null;
    if(formState.parentId === 0) formState.parentId = null;

    selectedRoles.value = splitRoles(record.roles);
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
 .menu-list-page {
     --menu-search-bg: #ffffff;
     --menu-search-border: rgba(17, 17, 17, 0.12);
     --menu-search-border-strong: rgba(17, 17, 17, 0.22);
     --menu-search-placeholder: rgba(17, 17, 17, 0.38);
     --menu-search-icon: rgba(17, 17, 17, 0.34);
     --menu-search-icon-active: rgba(17, 17, 17, 0.58);
     --menu-control-height: 40px;
     --menu-search-input-height: 44px;
     --menu-control-width: 112px;
     --menu-content-visual-offset: 10px;
     --menu-tree-icon-size: 16px;
     --menu-tree-icon-bar-length: 10px;
     --menu-tree-icon-stroke: 1.5px;
     --menu-tree-icon-duration: 0.28s;
     --menu-tree-icon-ease: cubic-bezier(0.22, 1, 0.36, 1);
     padding-top: 2px;
 }

 .menu-page-header {
     display: flex;
     flex-direction: column;
     align-items: flex-start;
     gap: 26px;
     width: 100%;
     margin-top: 0;
     margin-bottom: 40px;
     padding-top: 2px;
  }

 .menu-page-title {
     margin: 0;
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

 .menu-page-controls {
     display: flex;
     flex-direction: column;
     align-items: flex-start;
     gap: 48px;
     width: 100%;
     max-width: 100%;
     padding-bottom: 4px;
     position: relative;
     top: var(--menu-content-visual-offset);
  }

 .menu-search-row {
     display: flex;
     align-items: center;
     gap: 14px !important;
     flex-wrap: wrap;
     width: 100%;
     max-width: 100%;
     padding: 0;
     border-radius: 0;
     background: transparent;
     margin-top: 24px;
     margin-bottom: -24px;
     position: relative;
     top: -10px;
  }

 .menu-search-actions {
     display: flex;
     align-items: center;
     align-self: center;
     gap: 10px !important;
     flex-wrap: wrap;
     margin-left: 4px !important;
  }

 .menu-search-actions > * {
     flex: 0 0 auto;
 }

 .menu-search-input {
     flex: 0 1 250px;
     width: 250px;
     max-width: min(100%, 250px);
  }

 .menu-list-page :deep(.standard-input-wrapper.menu-search-input),
 .menu-list-page :deep(.standard-input-wrapper.menu-search-input.has-margin) {
     margin-top: 0 !important;
     display: flex;
     align-items: stretch;
     height: var(--menu-search-input-height);
     min-height: var(--menu-search-input-height);
  }

.menu-list-page :deep(.standard-input-wrapper.menu-search-input .std-input) {
     box-sizing: border-box !important;
     background: var(--menu-search-bg) !important;
     border: 1px solid var(--menu-search-border) !important;
     min-height: 100% !important;
     height: 100% !important;
     border-radius: 999px !important;
     box-shadow: none !important;
     transition: border-color 0.18s ease, background 0.18s ease !important;
  }

 .menu-list-page :deep(.menu-search-input .std-input-inner) {
     padding: 0 14px !important;
     gap: 8px;
 }

 .menu-list-page :deep(.menu-search-input .std-input:hover) {
     border-color: var(--menu-search-border-strong) !important;
     background: #ffffff !important;
  }

 .menu-list-page :deep(.menu-search-input .std-input:focus),
 .menu-list-page :deep(.menu-search-input .std-input:focus-within) {
     border-color: var(--menu-search-border-strong) !important;
     background: #ffffff !important;
     box-shadow: none !important;
  }

 .menu-list-page :deep(.menu-search-input .std-input-control) {
     background: transparent !important;
 }

 .menu-list-page :deep(.menu-search-input .std-input-control::placeholder),
 .menu-list-page :deep(.menu-search-input::placeholder) {
     color: var(--menu-search-placeholder) !important;
     font-weight: 500;
  }

 .menu-list-page :deep(.menu-search-input .std-input-control) {
     font-size: 13px;
     font-weight: 500;
     line-height: 1 !important;
     letter-spacing: -0.01em;
  }

 .menu-list-page :deep(.menu-search-input .std-input-prefix) {
     margin-right: 8px;
     color: var(--menu-search-icon) !important;
     font-size: 14px;
     transition: color 0.22s ease;
  }

 .menu-search-actions :deep(.std-button) {
     --std-button-label-shift: 1px;
     min-width: 92px;
     height: 38px;
     min-height: 38px;
     padding-inline: 14px !important;
 }

 .menu-search-actions :deep(.std-button .std-button-label) {
     font-size: 14px !important;
     font-weight: 500 !important;
     line-height: 1 !important;
 }

 .menu-search-actions :deep(.std-button--icon-text .std-button-label) {
     transform: translateY(var(--std-button-label-shift)) !important;
 }

 .menu-search-actions :deep(.std-button .std-button-icon),
 .menu-search-actions :deep(.std-button svg) {
     width: 15px;
     height: 15px;
 }

 .menu-list-page :deep(.menu-search-input .std-input:hover .std-input-prefix),
 .menu-list-page :deep(.menu-search-input .std-input:focus-within .std-input-prefix) {
     color: var(--menu-search-icon-active) !important;
  }

.menu-native-button {
     appearance: none;
     -webkit-appearance: none;
     -moz-appearance: none;
     position: relative;
     display: inline-flex;
     align-items: center;
     justify-content: center;
     gap: 8px;
     margin: 0;
     padding: 0 16px;
     height: var(--menu-control-height);
     min-height: var(--menu-control-height);
     border-radius: 999px;
     box-shadow: none;
     outline: none;
     cursor: pointer;
     white-space: nowrap;
     text-align: center;
     line-height: 1;
     text-decoration: none;
     vertical-align: middle;
     background-image: none;
     -webkit-tap-highlight-color: transparent;
     transition: border-color 0.24s ease, background-color 0.24s ease, color 0.24s ease;
 }

 .menu-native-button::before,
 .menu-native-button::after {
     content: none !important;
     display: none !important;
 }

.menu-native-button.menu-control-button {
    width: var(--menu-control-width);
    min-width: var(--menu-control-width);
}

.menu-table-toolbar .menu-native-button.menu-toolbar-button.menu-toolbar-button--add {
     width: auto;
     min-width: 0;
     padding-inline: 16px 18px;
  }

 .menu-table-toolbar .menu-native-button.menu-toolbar-button.menu-toolbar-button--toggle {
     width: auto;
     min-width: 0;
     padding-inline: 16px;
  }

 .menu-table-toolbar .menu-native-button.menu-toolbar-button {
     height: 36px;
     min-height: 36px;
     gap: 6px;
     --std-button-font-size: 13px;
     --std-button-label-shift: 0.5px;
  }

 .menu-table-toolbar .menu-native-button.menu-toolbar-button :deep(.std-button-icon),
 .menu-table-toolbar .menu-native-button.menu-toolbar-button :deep(svg) {
     width: 14px;
     height: 14px;
  }

 .menu-list-page :deep(.glass-container--table) {
     margin-top: 22px;
     padding-top: 10px;
     padding-left: 0;
     padding-right: 0;
     position: relative;
     top: var(--menu-content-visual-offset);
  }

 .menu-list-page .openai-table {
     padding: 0;
 }

 .menu-list-page .openai-table :deep(.ant-table),
 .menu-list-page .openai-table :deep(.ant-table-container),
 .menu-list-page .openai-table :deep(.ant-table-content) {
     background: transparent !important;
     border: none !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-thead > tr > th) {
     padding: 12px 14px !important;
     background: transparent !important;
     border-bottom: 1px solid rgba(15, 23, 42, 0.06) !important;
     border-top: none !important;
     border-left: none !important;
     border-right: none !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr:not(.ant-table-measure-row) > td) {
     padding: 12px 14px !important;
     background: transparent !important;
     border-bottom: none !important;
     border-top: none !important;
     border-left: none !important;
     border-right: none !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr.ant-table-measure-row) {
     height: 0 !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr.ant-table-measure-row > td) {
     height: 0 !important;
     padding: 0 !important;
     border: 0 !important;
     background: transparent !important;
     line-height: 0 !important;
     font-size: 0 !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr.ant-table-measure-row + tr > td),
 .menu-list-page .openai-table :deep(.ant-table-tbody > tr.ant-table-row:first-of-type > td) {
     padding-top: 20px !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-thead > tr > th:first-child) {
     padding-left: 56px !important;
     text-align: left !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr > td:first-child) {
     padding-left: 18px !important;
     text-align: left !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr > td:first-child) {
     font-weight: 400 !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-row-expand-icon),
 .menu-list-page .openai-table :deep(.ant-table-row-indent + .ant-table-row-expand-icon) {
     margin-inline-end: 12px !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-thead > tr > th:nth-child(3)),
 .menu-list-page .openai-table :deep(.ant-table-thead > tr > th:nth-child(4)),
 .menu-list-page .openai-table :deep(.ant-table-thead > tr > th:nth-child(7)),
 .menu-list-page .openai-table :deep(.ant-table-tbody > tr > td:nth-child(3)),
 .menu-list-page .openai-table :deep(.ant-table-tbody > tr > td:nth-child(4)),
 .menu-list-page .openai-table :deep(.ant-table-tbody > tr > td:nth-child(7)) {
     padding-left: 12px !important;
     padding-right: 12px !important;
 }

 .menu-list-page .openai-table :deep(.ant-table-tbody > tr:hover > td) {
     background: #fafaf9 !important;
 }

.menu-list-page .openai-table :deep(.ant-table-cell-fix-left),
.menu-list-page .openai-table :deep(.ant-table-cell-fix-right) {
     background: #ffffff !important;
 }

 :deep(.ant-form-item-extra) {
     margin-top: 8px;
     line-height: 1.5;
 }

 .menu-table-toolbar {
     display: flex;
     align-items: center;
     width: 100%;
     max-width: 100%;
     flex-wrap: wrap;
     row-gap: 10px;
     column-gap: 12px;
     padding-left: 14px;
     margin-top: 9px;
  }

 .menu-native-button.menu-button-animated :deep(.std-button-icon),
 .menu-native-button.menu-button-animated :deep(.std-button-label) {
     transition: transform 0.24s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.22s ease;
 }

 .menu-native-button.menu-button-animated:hover,
 .menu-native-button.menu-button-animated:focus,
 .menu-native-button.menu-button-animated:focus-visible,
 .menu-native-button.menu-button-animated:active {
     transform: none;
 }

 .menu-native-button.menu-button-animated--add:hover :deep(.std-button-icon),
 .menu-native-button.menu-button-animated--add:focus :deep(.std-button-icon),
 .menu-native-button.menu-button-animated--add:focus-visible :deep(.std-button-icon) {
     transform: scale(1.14);
 }

 .menu-native-button.menu-toolbar-button.menu-toolbar-button--add {
     position: relative;
     top: 24px;
     justify-content: center;
  }

 .menu-native-button.menu-toolbar-button.menu-toolbar-button--toggle {
     position: relative;
     top: 24px;
  }

 .menu-toggle-lucide {
     display: block;
     stroke-width: 1.8;
     transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.22s ease;
 }

 .menu-native-button.menu-toolbar-button--toggle.is-expanded .menu-toggle-lucide {
     transform: scale(0.96);
 }

 .menu-native-button.menu-button-animated--toggle:hover .menu-toggle-lucide,
 .menu-native-button.menu-button-animated--toggle:focus .menu-toggle-lucide,
 .menu-native-button.menu-button-animated--toggle:focus-visible .menu-toggle-lucide {
     transform: scale(1.08);
 }

 .menu-muted-text {
     color: rgba(90, 97, 108, 0.7);
 }

 .menu-pill {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      margin-inline-end: 8px;
      padding: 2px 10px;
      border-radius: 999px;
      border: 1px solid var(--tag-surface-border) !important;
      background: var(--tag-surface-bg) !important;
      color: var(--tag-surface-text) !important;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22);
      font-size: 13px;
      font-weight: 400;
      line-height: 1.35;
      letter-spacing: 0;
      text-align: center;
      vertical-align: middle;
      box-sizing: border-box;
      transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
   }

 .menu-pill--glass {
     color: var(--tag-tone-cyan-text) !important;
     background: var(--tag-tone-cyan-bg) !important;
     border-color: var(--tag-tone-cyan-border) !important;
 }

 .menu-pill--default {
     color: var(--tag-tone-amber-text) !important;
     background: var(--tag-tone-amber-bg) !important;
     border-color: var(--tag-tone-amber-border) !important;
 }

 .menu-pill--role.menu-pill--admin {
     color: var(--tag-tone-blue-text) !important;
     background: var(--tag-tone-blue-bg) !important;
     border-color: var(--tag-tone-blue-border) !important;
 }

 .menu-pill--role.menu-pill--staff {
     color: var(--tag-tone-indigo-text) !important;
     background: var(--tag-tone-indigo-bg) !important;
     border-color: var(--tag-tone-indigo-border) !important;
 }

 .menu-pill--role.menu-pill--coach,
 .menu-pill--role.menu-pill--member {
     color: var(--tag-tone-sky-text) !important;
     background: var(--tag-tone-sky-bg) !important;
     border-color: var(--tag-tone-sky-border) !important;
  }

 .menu-action-group {
     flex-wrap: wrap;
     row-gap: 6px;
 }

 .table-glass-action {
     height: auto;
     padding: 0 4px;
     border: none !important;
     background: transparent !important;
     box-shadow: none !important;
     font-weight: 400;
     font-size: 13px;
     letter-spacing: 0;
     transition: color 0.2s ease, opacity 0.2s ease;
 }

 .table-glass-action--primary {
     color: var(--action-tone-primary) !important;
 }

 .table-glass-action--accent {
     color: var(--action-tone-accent) !important;
 }

 .table-glass-action--danger {
     color: var(--action-tone-danger) !important;
 }

 .table-glass-action:hover,
 .table-glass-action:focus,
 .table-glass-action:active {
     background: transparent !important;
     border-color: transparent !important;
     box-shadow: none !important;
     opacity: 1;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-tbody > tr > td) {
     vertical-align: middle;
 }

 .menu-list-page :deep(.transparent-glass-mode .ant-table-container) {
     border: none !important;
     box-shadow: none !important;
 }

 .menu-list-page :deep(.ant-table-row-level-0 > td:first-child) {
     font-weight: 400 !important;
 }

 .menu-list-page :deep(.ant-table-row-level-1 > td:first-child) {
     color: rgba(17, 24, 39, 0.78);
 }

 .menu-list-page :deep(.ant-table-row-level-2 > td:first-child) {
     color: rgba(17, 24, 39, 0.62);
 }

 .menu-list-page :deep(.ant-table-row-indent) {
     position: relative;
     display: inline-block;
     float: none !important;
     height: var(--menu-tree-icon-size) !important;
     vertical-align: middle;
 }

 .menu-list-page :deep(.ant-table-row-expand-icon) {
     --menu-tree-icon-rotation: 0deg;
     --menu-tree-icon-scale: 1;
     --menu-tree-icon-horizontal-scale: 1;
     --menu-tree-icon-vertical-rotation: 0deg;
     --menu-tree-icon-vertical-scale: 1;
     --menu-tree-icon-vertical-opacity: 1;
     --menu-tree-icon-vertical-opacity-delay: 0s;
     position: relative !important;
     display: inline-flex !important;
     align-items: center;
     justify-content: center;
     width: var(--menu-tree-icon-size) !important;
     min-width: var(--menu-tree-icon-size) !important;
     height: var(--menu-tree-icon-size) !important;
     margin-inline-end: 14px !important;
     padding: 0 !important;
     color: #475569 !important;
     line-height: 1 !important;
     vertical-align: middle !important;
     background: transparent !important;
     border: none !important;
     border-radius: 0 !important;
     box-shadow: none !important;
     perspective: 48px;
     transform: translateZ(0) rotate(var(--menu-tree-icon-rotation)) scale(var(--menu-tree-icon-scale)) !important;
     transform-origin: 50% 50%;
     transition: color 0.22s ease, transform var(--menu-tree-icon-duration) var(--menu-tree-icon-ease);
 }

.menu-list-page :deep(.ant-table-row-expand-icon::before),
.menu-list-page :deep(.ant-table-row-expand-icon::after) {
     content: '' !important;
     position: absolute !important;
     display: block !important;
     background: currentColor !important;
     border-radius: 999px;
     opacity: 1;
     transform-origin: 50% 50%;
     will-change: transform, opacity;
     transition: transform var(--menu-tree-icon-duration) var(--menu-tree-icon-ease);
 }

 .menu-list-page :deep(.ant-table-row-expand-icon::before) {
     top: 50% !important;
     left: 50% !important;
     width: var(--menu-tree-icon-bar-length) !important;
     height: var(--menu-tree-icon-stroke) !important;
     transform: translate(-50%, -50%) scaleX(var(--menu-tree-icon-horizontal-scale)) !important;
 }

 .menu-list-page :deep(.ant-table-row-expand-icon::after) {
     top: 50% !important;
     left: 50% !important;
     width: var(--menu-tree-icon-stroke) !important;
     height: var(--menu-tree-icon-bar-length) !important;
     opacity: var(--menu-tree-icon-vertical-opacity) !important;
     transform: translate(-50%, -50%) rotate(var(--menu-tree-icon-vertical-rotation)) scaleY(var(--menu-tree-icon-vertical-scale)) !important;
     transition:
         transform var(--menu-tree-icon-duration) var(--menu-tree-icon-ease),
         opacity 0s linear var(--menu-tree-icon-vertical-opacity-delay);
 }

 .menu-list-page :deep(.ant-table-row-expand-icon-expanded) {
     --menu-tree-icon-rotation: 180deg;
     --menu-tree-icon-horizontal-scale: 0.98;
     --menu-tree-icon-vertical-rotation: 90deg;
     --menu-tree-icon-vertical-scale: 1;
     --menu-tree-icon-vertical-opacity: 0;
     --menu-tree-icon-vertical-opacity-delay: calc(var(--menu-tree-icon-duration) * 0.92);
  }

 .menu-list-page :deep(.ant-table-row-expand-icon-spaced) {
     visibility: hidden !important;
     pointer-events: none;
 }

 .menu-list-page :deep(.ant-table-row-expand-icon-spaced::before),
 .menu-list-page :deep(.ant-table-row-expand-icon-spaced::after) {
     display: none !important;
 }

 .menu-list-page :deep(.ant-table-row-expand-icon:hover),
 .menu-list-page :deep(.ant-table-row-expand-icon:focus),
 .menu-list-page :deep(.ant-table-row-expand-icon:active) {
     --menu-tree-icon-scale: 1.06;
     background: transparent !important;
     border-color: transparent !important;
     color: #111827 !important;
 }

 @media (prefers-reduced-motion: reduce) {
     .menu-list-page :deep(.ant-table-row-expand-icon),
     .menu-list-page :deep(.ant-table-row-expand-icon::before),
     .menu-list-page :deep(.ant-table-row-expand-icon::after) {
         transition: none !important;
     }
 }

.menu-list-page :deep(.transparent-glass-mode .ant-table-cell-fix-right) {
     background: rgba(255, 255, 255, 0.12) !important;
     backdrop-filter: blur(10px);
     -webkit-backdrop-filter: blur(10px);
 }

 html.dark .menu-list-page {
     --menu-search-bg: rgba(255, 255, 255, 0.04);
     --menu-search-border: rgba(255, 255, 255, 0.12);
     --menu-search-border-strong: rgba(255, 255, 255, 0.2);
     --menu-search-placeholder: rgba(255, 255, 255, 0.48);
     --menu-search-icon: rgba(255, 255, 255, 0.5);
     --menu-search-icon-active: rgba(255, 255, 255, 0.82);
 }

 html.dark .menu-list-page :deep(.ant-table-row-expand-icon) {
     color: rgba(255, 255, 255, 0.72) !important;
 }

 html.dark .menu-list-page :deep(.ant-table-row-level-1 > td:first-child) {
     color: rgba(255, 255, 255, 0.74);
 }

 html.dark .menu-list-page :deep(.ant-table-row-level-2 > td:first-child) {
     color: rgba(255, 255, 255, 0.58);
 }

 html.dark .menu-list-page :deep(.ant-table-row-expand-icon:hover),
 html.dark .menu-list-page :deep(.ant-table-row-expand-icon:focus),
 html.dark .menu-list-page :deep(.ant-table-row-expand-icon:active) {
     color: rgba(255, 255, 255, 0.92) !important;
 }

html.dark .menu-list-page :deep(.transparent-glass-mode .ant-table-cell-fix-right) {
     background: rgba(18, 18, 18, 0.55) !important;
 }
</style>
