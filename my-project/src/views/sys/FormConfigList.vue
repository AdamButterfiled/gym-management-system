<template>
  <WorkspacePage title="表单管理">
    <template #meta>
      <span class="page-meta">按菜单结构定位页面，再分别配置搜索区、表格和 Modal 表单。</span>
    </template>

    <template #actions>
      <StandardButton type="default" icon="reload" @click="handleReloadWorkspace">刷新元数据</StandardButton>
      <StandardButton type="default" :disabled="!activeDraft" @click="openCreateTargetModal">补充目标</StandardButton>
      <StandardButton type="primary" :disabled="!activeDraft" :loading="savingPage" @click="handleSavePage">
        保存配置
      </StandardButton>
    </template>

    <div class="designer-shell">
      <aside class="designer-sidebar">
        <div class="sidebar-head">
          <div>
            <h2 class="section-title">菜单树</h2>
            <p class="section-sub">按真实菜单层级查找页面，不再靠平铺列表猜页面。</p>
          </div>
        </div>

        <a-input
            v-model:value="treeSearchText"
            class="sidebar-search"
            allow-clear
            placeholder="搜索菜单标题、路由或组件"
        />

        <div class="tree-panel">
          <a-tree
              v-if="renderMenuTree.length"
              class="tree-menu"
              block-node
              :tree-data="renderMenuTree"
              :selectedKeys="selectedMenuTreeKeys"
              :expandedKeys="visibleExpandedTreeKeys"
              :show-line="{ showLeafIcon: false }"
              @select="handleTreeSelect"
              @expand="handleTreeExpand"
          >
            <template #switcherIcon="{ expanded, isLeaf }">
              <span v-if="isLeaf" class="ant-tree-switcher-leaf-line tree-switcher-line" aria-hidden="true"/>
              <span v-else class="tree-switcher-icon" aria-hidden="true">
                <component :is="expanded ? DownOutlined : RightOutlined"/>
              </span>
            </template>
            <template #title="{ title: nodeTitle, icon: nodeIcon, selection, path, children }">
              <div class="tree-node-row">
                <span class="tree-node-icon" aria-hidden="true">
                  <component :is="resolveTreeIcon({ icon: nodeIcon, selection })"/>
                </span>
                <div class="tree-row-content">
                  <span class="tree-row-title">{{ nodeTitle }}</span>
                  <span>{{ treeNodeMeta({selection, path, children}) }}</span>
                </div>
                <span
                    v-if="selection"
                    :class="['status-chip', `status-chip--${selection.status}`]"
                >
                  {{ statusLabel(selection.status) }}
                </span>
              </div>
            </template>
          </a-tree>

          <div v-else class="empty-hint">没有匹配的菜单节点。</div>
        </div>

        <div v-if="filteredUnclassifiedSelections.length" class="unclassified-panel">
          <div class="sidebar-subhead">
            <h3>未归类页面</h3>
            <span>未挂菜单的已存配置或扫描结果。</span>
          </div>
          <div
              v-for="item in filteredUnclassifiedSelections"
              :key="item.id"
              class="unclassified-row"
              :class="{ 'unclassified-row--selected': activeSelectionId === item.id }"
              @click="handleSelectionChange(item)"
          >
            <div>
              <strong>{{ item.pageTitle }}</strong>
              <span>{{ item.routePath || item.menuBinding?.componentPath || item.pageKey }}</span>
            </div>
            <span :class="['status-chip', `status-chip--${item.status}`]">
              {{ statusLabel(item.status) }}
            </span>
          </div>
        </div>
      </aside>

      <section class="designer-main">
        <template v-if="activeDraft">
          <div v-if="pageDirty" class="draft-banner">
            当前页面存在未提交草稿。只有点击右上角“保存配置”后，运行态页面才会真正生效。
          </div>

          <section class="designer-card overview-card">
            <div class="card-head">
              <div>
                <h2 class="section-title">页面概览</h2>
                <p class="section-sub">先确认页面归属，再设置搜索区和页面级风格开关。</p>
              </div>
              <div class="card-head-meta">
                <span class="mono-badge">{{ activeDraft.pageKey }}</span>
                <span class="mono-badge mono-badge--soft">{{ activeDraft.routePath || '未绑定路由' }}</span>
              </div>
            </div>

            <div v-if="activePageIssues.length" class="issue-stack">
              <div v-for="issue in activePageIssues" :key="issue" class="issue-item">
                {{ issue }}
              </div>
            </div>

            <div class="overview-grid">
              <label class="field-block">
                <span>页面标题</span>
                <a-input v-model:value="activeDraft.pageTitle"/>
              </label>
              <label class="field-block">
                <span>页面键</span>
                <a-input :value="activeDraft.pageKey" disabled/>
              </label>
              <label class="field-block">
                <span>路由路径</span>
                <a-input :value="activeDraft.routePath" disabled/>
              </label>
              <label class="field-block">
                <span>组件路径</span>
                <a-input :value="formatComponentLabel(activeDraft.menuBinding?.componentPath)" disabled/>
              </label>
              <label class="field-block field-block--full">
                <span>菜单归属</span>
                <a-input :value="activeMenuTrailText" disabled/>
              </label>
            </div>

            <div class="switch-strip">
              <div class="switch-tile">
                <span>启用页面配置</span>
                <a-switch v-model:checked="activeDraft.enabled"/>
              </div>
              <div class="switch-tile">
                <span>普通输入框跟随系统圆角</span>
                <a-switch v-model:checked="activeDraft.formInputFollowSystemRadius"/>
              </div>
              <div class="switch-tile">
                <span>启用快捷搜索</span>
                <a-switch v-model:checked="quickSearchDraft.enabled"/>
              </div>
            </div>

            <div class="overview-grid overview-grid--search">
              <label class="field-block">
                <span>默认条件关系</span>
                <a-select v-model:value="quickSearchDraft.defaultLogic">
                  <a-select-option value="AND">AND</a-select-option>
                  <a-select-option value="OR">OR</a-select-option>
                </a-select>
              </label>
              <label class="field-block field-block--wide">
                <span>快捷搜索占位文案</span>
                <a-input v-model:value="quickSearchDraft.placeholder" placeholder="搜索用户名、订单号或标题"/>
              </label>
              <label class="field-block field-block--full">
                <span>快捷搜索字段</span>
                <a-select
                    v-model:value="quickSearchDraft.fields"
                    mode="multiple"
                    :options="quickSearchFieldOptions"
                    placeholder="选择顶部搜索框可检索的字段"
                />
              </label>
            </div>
          </section>

          <section class="designer-card target-nav-card">
            <div class="card-head">
              <div>
                <h2 class="section-title">配置目标</h2>
                <p class="section-sub">一个页面可以同时包含主表格、多个 Modal 表单和搜索区。</p>
              </div>
            </div>

            <div class="target-tabs">
              <button
                  v-for="target in sortedTargets"
                  :key="target.targetKey"
                  type="button"
                  class="target-tab"
                  :class="{ 'target-tab--active': activeTargetKey === target.targetKey }"
                  @click="activeTargetKey = target.targetKey"
              >
                <strong>{{ target.title }}</strong>
                <span>{{ targetTypeLabel(target.targetType) }}</span>
                <small>{{ countVisibleFields(target) }}/{{ target.fields.length }} 字段</small>
              </button>
            </div>
          </section>

          <section v-if="activeTarget" class="designer-card target-editor-card">
            <div class="card-head card-head--target">
              <div class="target-head-content">
                <a-input v-model:value="activeTarget.title" class="target-title-input"/>
                <p class="target-source-text">{{ activeTargetSourceText }}</p>
              </div>
              <div class="target-head-actions">
                <span class="mono-badge">{{ targetTypeLabel(activeTarget.targetType) }}</span>
                <div class="target-toggle">
                  <span>启用目标</span>
                  <a-switch v-model:checked="activeTarget.enabled"/>
                </div>
                <StandardButton type="default" @click="openFieldModal()">新增字段</StandardButton>
                <StandardButton
                    v-if="activeTarget.targetType !== 'table' && activeTableTarget"
                    type="default"
                    @click="handleSyncFormTarget"
                >
                  按表格顺序同步
                </StandardButton>
                <StandardButton type="delete" @click="handleRemoveTarget">删除目标</StandardButton>
              </div>
            </div>

            <div v-if="activeTargetIssues.length" class="issue-stack">
              <div v-for="issue in activeTargetIssues" :key="issue" class="issue-item">
                {{ issue }}
              </div>
            </div>

            <div v-if="activeTarget.targetType === 'table'" class="target-body">
              <div class="editor-hint">
                横向预览的顺序就是运行态表格顺序。拖动列卡片可换序，拖右侧把手可改列宽。
              </div>

              <div class="table-header-preview">
                <article
                    v-for="field in activeTargetFields"
                    :key="field.fieldKey"
                    class="table-header-cell"
                    :class="{ 'table-preview-card--hidden': !isFieldVisible(field) }"
                    :style="tableCardStyle(field)"
                    draggable="true"
                    @dragstart="handleTableDragStart(field.fieldKey)"
                    @dragover.prevent
                    @drop="handleTableDrop(field.fieldKey)"
                >
                  <span class="drag-label">拖拽换序</span>
                  <strong>{{ field.label || field.fieldKey }}</strong>
                  <span class="field-key">{{ field.fieldKey }} · {{ field.columnWidth || 180 }}px</span>
                  <button
                      type="button"
                      class="table-resize-handle"
                      @mousedown.prevent="startColumnResize(field, $event)"
                  />
                </article>
              </div>

              <div class="field-config-list">
                <article
                    v-for="field in activeTargetFields"
                    :key="`${field.fieldKey}-config-row`"
                    class="field-config-row"
                    :class="{ 'field-config-row--hidden': !isFieldVisible(field) }"
                >
                  <div class="field-config-title">
                    <strong>{{ field.label || field.fieldKey }}</strong>
                    <span>{{ field.fieldKey }}</span>
                  </div>
                  <div class="field-config-controls">
                    <label>
                      <span>列宽</span>
                      <a-input-number
                          :value="field.columnWidth || 180"
                          :min="96"
                          :max="520"
                          class="compact-number"
                          @update:value="(value) => updateFieldWidth(field, value)"
                      />
                    </label>
                    <label>
                      <span>显示</span>
                      <a-switch :checked="isFieldVisible(field)" size="small"
                                @change="(checked) => toggleFieldVisible(field, Boolean(checked))"/>
                    </label>
                    <label>
                      <span>筛选</span>
                      <a-switch :checked="Boolean(field.filterEnabled)" size="small"
                                @change="(checked) => (field.filterEnabled = Boolean(checked))"/>
                    </label>
                  </div>
                  <div class="field-config-actions">
                    <StandardButton type="text" size="sm" @click="moveField(activeTarget, field.fieldKey, -1)">前移
                    </StandardButton>
                    <StandardButton type="text" size="sm" @click="moveField(activeTarget, field.fieldKey, 1)">后移
                    </StandardButton>
                    <StandardButton type="link" size="sm" @click="openFieldModal(field)">编辑字段</StandardButton>
                    <StandardButton type="link" size="sm" danger @click="handleRemoveField(field.fieldKey)">删除
                    </StandardButton>
                  </div>
                </article>
              </div>
            </div>

            <div v-else class="target-body">
              <div class="editor-hint">
                这里直接按真实表单样式预览。拖进某一行会自动均分宽度，拖到行间会新起一行；同一行内也可以直接拖拽调整左右顺序，右下角把手负责调宽和调高。
              </div>
              <FormLayoutDesignerCanvas
                  :title="activeTarget.title"
                  :fields="activeTargetFields"
                  :layout-mode="activeTarget.sourceSignature?.formLayout || ''"
                  :form-class="activeTarget.sourceSignature?.formClass || ''"
                  :label-col-span="activeTarget.sourceSignature?.formLabelSpan"
                  :wrapper-col-span="activeTarget.sourceSignature?.formWrapperSpan"
                  @edit="openFieldModal"
                  @remove="handleRemoveField"
              />
            </div>
          </section>
        </template>

        <div v-else class="designer-empty">
          从左侧菜单树选择页面后，再开始配置搜索区、表格列和 Modal 表单。
        </div>
      </section>
    </div>
  </WorkspacePage>

  <StandardModal
      :visible="fieldModalVisible"
      width="920px"
      title="字段配置"
      okText="保存字段"
      cancelText="关闭"
      @update:visible="handleFieldModalVisibilityChange"
      @ok="handleFieldModalSave"
  >
    <div v-if="fieldModalDraft" class="field-modal-layout">
      <div class="field-modal-head">
        <div>
          <strong>{{ fieldModalDraft.label || fieldModalDraft.fieldKey || '新字段' }}</strong>
          <span>{{
              fieldModalTarget ? `${fieldModalTarget.title} · ${targetTypeLabel(fieldModalTarget.targetType)}` : '未选择目标'
            }}</span>
        </div>
        <span v-if="fieldModalDirty" class="field-dirty-indicator">尚未写回页面草稿</span>
      </div>

      <section class="field-modal-section">
        <h3>基础信息</h3>
        <div class="modal-grid">
          <label class="field-block">
            <span>字段标题</span>
            <a-input v-model:value="fieldModalDraft.label"/>
          </label>
          <label class="field-block">
            <span>字段 Key</span>
            <a-input v-model:value="fieldModalDraft.fieldKey"/>
          </label>
          <label class="field-block">
            <span>查询键</span>
            <a-input v-model:value="fieldModalDraft.queryKey"/>
          </label>
          <label class="field-block">
            <span>控件类型</span>
            <a-select v-model:value="fieldModalDraft.controlType" :options="controlTypeOptions"/>
          </label>
          <label class="field-block field-block--wide">
            <span>占位文案</span>
            <a-input v-model:value="fieldModalDraft.placeholder"/>
          </label>
        </div>
      </section>

      <section class="field-modal-section">
        <h3>显示与筛选</h3>
        <div class="switch-strip switch-strip--modal">
          <div class="switch-tile">
            <span>页面显示</span>
            <a-switch :checked="isFieldVisible(fieldModalDraft)"
                      @change="(checked) => toggleFieldVisible(fieldModalDraft, Boolean(checked))"/>
          </div>
          <div class="switch-tile">
            <span>允许筛选</span>
            <a-switch v-model:checked="fieldModalDraft.filterEnabled"/>
          </div>
          <div class="switch-tile">
            <span>参与快捷搜索</span>
            <a-switch v-model:checked="fieldModalDraft.quickSearchEnabled"/>
          </div>
          <div class="switch-tile">
            <span>允许切换模糊/精确</span>
            <a-switch v-model:checked="fieldModalDraft.allowMatchModeToggle"/>
          </div>
          <div class="switch-tile">
            <span>允许多选</span>
            <a-switch v-model:checked="fieldModalDraft.allowMultiple"/>
          </div>
          <div class="switch-tile">
            <span>隐藏但可筛选</span>
            <a-switch v-model:checked="fieldModalDraft.hiddenButFilterable"/>
          </div>
        </div>
      </section>

      <section class="field-modal-section">
        <h3>{{ fieldModalTarget?.targetType === 'table' ? '表格设置' : '表单布局' }}</h3>
        <div class="modal-grid">
          <label class="field-block">
            <span>排序</span>
            <a-input-number
                :value="fieldModalDraft.order ?? fieldModalDraft.columnOrder ?? 0"
                :min="0"
                style="width: 100%"
                @update:value="(value) => updateFieldOrder(fieldModalDraft, value)"
            />
          </label>
          <label v-if="fieldModalTarget?.targetType === 'table'" class="field-block">
            <span>列宽(px)</span>
            <a-input-number v-model:value="fieldModalDraft.columnWidth" :min="96" :max="520" style="width: 100%"/>
          </label>
          <template v-else>
            <label class="field-block">
              <span>X</span>
              <a-input-number v-model:value="fieldModalLayout.x" :min="0" :max="23" style="width: 100%"/>
            </label>
            <label class="field-block">
              <span>Y</span>
              <a-input-number v-model:value="fieldModalLayout.y" :min="0" style="width: 100%"/>
            </label>
            <label class="field-block">
              <span>宽度</span>
              <a-input-number v-model:value="fieldModalLayout.w" :min="1" :max="24" style="width: 100%"/>
            </label>
            <label class="field-block">
              <span>高度</span>
              <a-input-number v-model:value="fieldModalLayout.h" :min="1" :max="6" style="width: 100%"/>
            </label>
          </template>
        </div>
      </section>

      <section class="field-modal-section">
        <h3>选项来源</h3>
        <div class="modal-grid">
          <label class="field-block">
            <span>来源类型</span>
            <a-select v-model:value="fieldModalDraft.optionSourceType" allow-clear :options="optionSourceTypeOptions"/>
          </label>
          <label class="field-block">
            <span>默认操作符</span>
            <a-select v-model:value="fieldModalDraft.defaultOperator" allow-clear :options="operatorOptions"/>
          </label>
          <label class="field-block">
            <span>默认匹配方式</span>
            <a-select v-model:value="fieldModalDraft.defaultMatchMode" allow-clear :options="matchModeSelectOptions"/>
          </label>
          <label class="field-block field-block--wide">
            <span>操作符集合</span>
            <a-select v-model:value="fieldModalDraft.operatorSet" mode="multiple" :options="operatorOptions"/>
          </label>
        </div>

        <div v-if="fieldModalDraft.optionSourceType === 'dict'" class="source-panel">
          <label class="field-block">
            <span>字典类型</span>
            <a-input v-model:value="fieldDictType" placeholder="如: booking_status"/>
          </label>
        </div>

        <div v-else-if="fieldModalDraft.optionSourceType === 'static'" class="source-panel">
          <label class="field-block field-block--full">
            <span>固定选项</span>
            <a-textarea
                v-model:value="fieldStaticOptionsText"
                :rows="5"
                placeholder="每行一个选项，格式：标签:值"
            />
          </label>
        </div>

        <div v-else-if="fieldModalDraft.optionSourceType === 'remote-form'" class="source-panel">
          <div class="modal-grid">
            <label class="field-block">
              <span>来源页面</span>
              <a-select v-model:value="fieldSourcePageKey" allow-clear :options="sourcePageOptions"
                        placeholder="选择系统表单页面"/>
            </label>
            <label class="field-block">
              <span>标签字段</span>
              <a-select v-model:value="fieldSourceLabelField" allow-clear :options="remoteFieldOptions"/>
            </label>
            <label class="field-block">
              <span>值字段</span>
              <a-select v-model:value="fieldSourceValueField" allow-clear :options="remoteFieldOptions"/>
            </label>
            <label class="field-block field-block--wide">
              <span>搜索字段</span>
              <a-input v-model:value="fieldSourceSearchFieldsText" placeholder="逗号分隔，如 username,realName"/>
            </label>
          </div>
        </div>

        <div v-else-if="fieldModalDraft.optionSourceType === 'remote-api'" class="source-panel">
          <div class="modal-grid">
            <label class="field-block field-block--wide">
              <span>接口地址</span>
              <a-input v-model:value="fieldRemoteEndpoint" placeholder="/api/endpoint"/>
            </label>
            <label class="field-block">
              <span>标签字段</span>
              <a-input v-model:value="fieldSourceLabelField" placeholder="label"/>
            </label>
            <label class="field-block">
              <span>值字段</span>
              <a-input v-model:value="fieldSourceValueField" placeholder="value"/>
            </label>
          </div>
        </div>
      </section>
    </div>
  </StandardModal>

  <StandardModal
      v-model:visible="targetModalVisible"
      width="520px"
      title="补充目标"
      okText="创建目标"
      @ok="handleCreateTarget"
  >
    <div class="modal-grid">
      <label class="field-block">
        <span>目标标题</span>
        <a-input v-model:value="targetDraft.title" placeholder="如：新增/编辑表单"/>
      </label>
      <label class="field-block">
        <span>目标 Key</span>
        <a-input v-model:value="targetDraft.targetKey" placeholder="如：editor-form"/>
      </label>
      <label class="field-block field-block--full">
        <span>目标类型</span>
        <a-select v-model:value="targetDraft.targetType" :options="targetTypeOptions"/>
      </label>
    </div>
  </StandardModal>

  <StandardModal
      v-model:visible="guardModalVisible"
      width="520px"
      :title="guardDialog.title"
      :mask-closable="false"
      :closable="false"
  >
    <p class="guard-message">{{ guardDialog.message }}</p>
    <template #footer>
      <div class="guard-footer">
        <StandardButton type="primary" :loading="guardBusy" @click="handleGuardDecision('save')">
          {{ guardDialog.saveText }}
        </StandardButton>
        <StandardButton type="default" :disabled="guardBusy" @click="handleGuardDecision('discard')">
          {{ guardDialog.discardText }}
        </StandardButton>
        <StandardButton type="text" :disabled="guardBusy" @click="handleGuardDecision('cancel')">
          取消
        </StandardButton>
      </div>
    </template>
  </StandardModal>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {Modal, message} from 'ant-design-vue';
import {
  AppstoreOutlined,
  BarChartOutlined,
  CalendarOutlined,
  DownOutlined,
  EnvironmentOutlined,
  FolderOpenOutlined,
  FormOutlined,
  QrcodeOutlined,
  RightOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WalletOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue';
import {onBeforeRouteLeave} from 'vue-router';
import request from '@/request';
import {
  createFormConfigPage,
  fetchFormConfigPage,
  fetchFormConfigPages,
  fetchSourceFields,
  fetchSourcePages,
  updateFormConfigPage,
} from '@/api/formConfig';
import StandardButton from '@/components/common/StandardButton.vue';
import FormLayoutDesignerCanvas from '@/components/common/FormLayoutDesignerCanvas.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import {usePageStyle} from '@/hooks/usePageStyle';
import type {
  FormConfigManifestPage,
  FormConfigTarget,
  FormConfigTargetType,
  FormFieldConfig,
  FormFieldLayout,
  FormPageConfig,
  FormPageMenuBinding,
  SourceFieldItem,
  SourcePageItem,
} from '@/types/formConfig';
import {
  cloneField,
  clonePageConfig,
  createDraftFromManifest,
  getQuickSearchConfig,
  normalizePageConfig,
  resolveTableTarget,
  resolveTarget,
  sortTargetFields,
  syncFormTargetWithTable,
  upsertTargetField,
} from '@/utils/formConfigDesigner';
import {
  listFormConfigManifestPages,
  reloadFormConfigManifestPages,
  findFormConfigManifestPage,
  findFormConfigManifestPageByKey,
} from '@/utils/formConfigManifest';
import {
  matchModeOptions as buildMatchModeOptions,
  operatorOptionsForField,
  smartSourceFieldMatch
} from '@/utils/formConfig';

interface MenuRecord {
  id: number;
  parentId: number | null;
  title: string;
  name?: string;
  path?: string;
  component?: string;
  icon?: string;
  hidden?: boolean;
  sort?: number;
  children?: MenuRecord[];
}

type SelectionStatus = 'configured' | 'unconfigured' | 'issue';
type GuardDecision = 'save' | 'discard' | 'cancel';

interface PageSelection {
  id: string;
  pageKey: string;
  routePath: string;
  pageTitle: string;
  manifest: FormConfigManifestPage | null;
  savedConfig: FormPageConfig | null;
  menuBinding?: FormPageMenuBinding;
  status: SelectionStatus;
  issues: string[];
}

interface MenuTreeNode extends MenuRecord {
  children: MenuTreeNode[];
  selection: PageSelection | null;
  trail: string[];
}

interface MenuTreeRenderNode extends MenuTreeNode {
  key: number;
  selectable: boolean;
  children: MenuTreeRenderNode[];
}

const {loadMenuConfig} = usePageStyle();

const controlTypeOptions = [
  {label: '文本', value: 'text'},
  {label: '选择', value: 'select'},
  {label: '远程接口', value: 'remote-api'},
  {label: '系统表单', value: 'remote-form'},
  {label: '日期', value: 'date'},
  {label: '日期区间', value: 'date-range'},
  {label: '数字', value: 'number'},
  {label: '数字区间', value: 'number-range'},
];

const optionSourceTypeOptions = [
  {label: '固定枚举', value: 'static'},
  {label: '系统字典', value: 'dict'},
  {label: '远程接口', value: 'remote-api'},
  {label: '系统表单', value: 'remote-form'},
];

const targetTypeOptions = [
  {label: '表格', value: 'table'},
  {label: '表单', value: 'form'},
  {label: 'Modal 表单', value: 'modal-form'},
];

const matchModeSelectOptions = buildMatchModeOptions();
const FORM_GRID_COLUMNS = 24;
const FORM_GRID_ROW_HEIGHT = 88;
const TABLE_PREVIEW_MIN_WIDTH = 220;
const FORM_LAYOUT_MIN_SPAN = 1;

const menuIconMap: Record<string, any> = {
  AppstoreOutlined,
  BarChartOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  FormOutlined,
  QrcodeOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WalletOutlined,
  WarningOutlined,
};

const treeSearchText = ref('');
const menuRecords = ref<MenuRecord[]>([]);
const savedConfigs = ref<FormPageConfig[]>([]);
const sourcePages = ref<SourcePageItem[]>([]);
const sourceFields = ref<SourceFieldItem[]>([]);
const activeSelectionId = ref('');
const activeDraft = ref<FormPageConfig | null>(null);
const activeTargetKey = ref('');
const savedSnapshot = ref('');
const savingPage = ref(false);
const targetModalVisible = ref(false);
const fieldModalVisible = ref(false);
const fieldModalTargetKey = ref('');
const fieldModalDraft = ref<FormFieldConfig | null>(null);
const fieldModalSnapshot = ref('');
const fieldStaticOptionsText = ref('');
const fieldSourceSearchFieldsText = ref('');
const remoteSourceFieldCatalog = ref<SourceFieldItem[]>([]);
const formCanvasRef = ref<HTMLElement | null>(null);
const tableDragFieldKey = ref('');
const guardModalVisible = ref(false);
const guardBusy = ref(false);
const manifestVersion = ref(0);

const targetDraft = reactive<{
  title: string;
  targetKey: string;
  targetType: FormConfigTargetType;
}>({
  title: '',
  targetKey: '',
  targetType: 'modal-form',
});

const guardDialog = reactive({
  title: '',
  message: '',
  saveText: '保存并离开',
  discardText: '放弃修改',
});

let guardResolver: ((decision: GuardDecision) => void) | null = null;
let resizeCleanup: (() => void) | null = null;
let canvasCleanup: (() => void) | null = null;

const manifestPages = computed(() => {
  manifestVersion.value;
  return listFormConfigManifestPages();
});

const quickSearchDraft = computed(() => {
  if (!activeDraft.value) {
    return null;
  }
  if (!activeDraft.value.quickSearch) {
    activeDraft.value.quickSearch = {
      enabled: true,
      placeholder: '',
      fields: [],
      defaultLogic: 'AND',
    };
  }
  return activeDraft.value.quickSearch;
});

const savedConfigMap = computed(() => new Map(savedConfigs.value.map((config) => [config.pageKey, config])));
const sourcePageOptions = computed(() => sourcePages.value.map((page) => ({
  label: `${page.pageTitle} (${page.routePath})`,
  value: page.pageKey,
})));
const remoteFieldOptions = computed(() => remoteSourceFieldCatalog.value.map((field) => ({
  label: `${field.label} (${field.fieldKey})`,
  value: field.fieldKey,
})));

const menuTree = computed(() => buildMenuTree(menuRecords.value));
const filteredMenuTree = computed(() => filterMenuTree(menuTree.value, treeSearchText.value));
const hasTreeSearch = computed(() => Boolean(treeSearchText.value.trim()));
const renderMenuTree = computed(() => buildRenderMenuTree(filteredMenuTree.value));
const allMenuSelections = computed(() => collectSelections(menuTree.value));
const unclassifiedSelections = computed(() => buildUnclassifiedSelections(allMenuSelections.value));
const selectedMenuTreeKeys = computed(() => {
  const selectedId = activeSelectionId.value.startsWith('menu:') ? Number(activeSelectionId.value.replace('menu:', '')) : NaN;
  return Number.isFinite(selectedId) ? [selectedId] : [];
});
const visibleExpandedTreeKeys = computed(() => (
    hasTreeSearch.value ? collectExpandableNodeKeys(renderMenuTree.value) : expandedNodeIds.value
));
const filteredUnclassifiedSelections = computed(() => {
  const keyword = normalizeText(treeSearchText.value);
  if (!keyword) {
    return unclassifiedSelections.value;
  }
  return unclassifiedSelections.value.filter((item) =>
      [item.pageTitle, item.routePath, item.pageKey, item.menuBinding?.componentPath]
          .filter(Boolean)
          .some((value) => normalizeText(value).includes(keyword))
  );
});

const activeManifest = computed(() => {
  if (!activeDraft.value) {
    return null;
  }
  return (
      findFormConfigManifestPage(activeDraft.value.routePath, activeDraft.value.menuBinding?.componentPath || null) ||
      findFormConfigManifestPageByKey(activeDraft.value.pageKey)
  );
});

const sortedTargets = computed(() => {
  if (!activeDraft.value?.targets) {
    return [] as FormConfigTarget[];
  }
  return [...activeDraft.value.targets].sort((left, right) => (left.order ?? 0) - (right.order ?? 0));
});

const activeTarget = computed(() => {
  if (!activeDraft.value) {
    return null;
  }
  return resolveTarget(activeDraft.value, activeTargetKey.value) || sortedTargets.value[0] || null;
});

const activeTableTarget = computed(() => resolveTableTarget(activeDraft.value));

const activeTargetFields = computed(() => {
  if (!activeTarget.value) {
    return [] as FormFieldConfig[];
  }
  return sortTargetFields(activeTarget.value.fields || [], activeTarget.value.targetType);
});

const quickSearchFieldOptions = computed(() => {
  const tableTarget = activeTableTarget.value;
  if (!tableTarget) {
    return [];
  }
  return sortTargetFields(tableTarget.fields || [], tableTarget.targetType)
      .filter((field) => (field.controlType || 'text') === 'text')
      .map((field) => ({
        label: `${field.label || field.fieldKey} (${field.fieldKey})`,
        value: field.fieldKey,
      }));
});

const activeMenuTrailText = computed(() => {
  const trail = activeDraft.value?.menuBinding?.menuTrail || [];
  return trail.length ? trail.join(' / ') : '未绑定菜单';
});

const pageDirty = computed(() => {
  if (!activeDraft.value) {
    return false;
  }
  return serializeDraft(activeDraft.value) !== savedSnapshot.value;
});

const activePageIssues = computed(() => {
  const issues = new Set<string>();
  activeDraft.value?.runtimeIssues?.forEach((item) => issues.add(item.message));
  activeManifest.value?.detectionIssues?.forEach((item) => issues.add(item));
  if (activeDraft.value && !(activeDraft.value.targets || []).length) {
    issues.add('当前页面尚未识别到任何配置目标，请补充表格或表单目标。');
  }
  return Array.from(issues);
});

const activeTargetIssues = computed(() => {
  const issues = new Set<string>();
  const target = activeTarget.value;
  if (!target) {
    return [] as string[];
  }
  target.runtimeIssues?.forEach((item) => issues.add(item.message));

  const manifestTarget = activeManifest.value?.targets.find((item) => item.targetKey === target.targetKey) || null;
  manifestTarget?.detectionIssues?.forEach((item) => issues.add(item));

  const manifestFieldKeys = new Set((manifestTarget?.fields || []).map((field) => field.fieldKey));
  const sourceFieldKeys = new Set(sourceFields.value.map((field) => field.fieldKey));

  target.fields.forEach((field) => {
    if (target.targetType === 'table' && sourceFieldKeys.size && !sourceFieldKeys.has(field.fieldKey) && !manifestFieldKeys.has(field.fieldKey)) {
      issues.add(`${field.label || field.fieldKey} 已保存，但后端查询层未接入该字段映射。`);
      return;
    }
    if (manifestFieldKeys.size && !manifestFieldKeys.has(field.fieldKey)) {
      issues.add(`${field.label || field.fieldKey} 已保存，但页面模板未检测到该字段接入。`);
    }
  });

  return Array.from(issues);
});

const activeTargetSourceText = computed(() => {
  const target = activeTarget.value;
  if (!target) {
    return '未选择目标';
  }
  const signature = target.sourceSignature || {};
  const pieces = [
    signature.titleCandidate,
    signature.columnsBinding ? `列源 ${signature.columnsBinding}` : '',
    signature.modalBinding ? `Modal ${signature.modalBinding}` : '',
    signature.componentPath ? formatComponentLabel(signature.componentPath) : '',
  ].filter(Boolean);
  return pieces.length ? pieces.join(' · ') : '可直接在此预览顺序、显隐和布局。';
});

const formCanvasStyle = computed(() => ({}));

const fieldModalTarget = computed(() => {
  if (!activeDraft.value) {
    return null;
  }
  return resolveTarget(activeDraft.value, fieldModalTargetKey.value);
});

const fieldModalDirty = computed(() => {
  if (!fieldModalDraft.value) {
    return false;
  }
  return JSON.stringify(fieldModalDraft.value) !== fieldModalSnapshot.value;
});

const fieldModalLayout = computed(() => {
  if (!fieldModalDraft.value) {
    return {x: 0, y: 0, w: 12, h: 1};
  }
  return ensureFieldLayout(fieldModalDraft.value, 0);
});

const operatorOptions = computed(() => operatorOptionsForField(fieldModalDraft.value));

const fieldDictType = computed({
  get: () => String(ensureOptionSourceConfig(fieldModalDraft.value).dictType ?? ''),
  set: (value: string) => {
    ensureOptionSourceConfig(fieldModalDraft.value).dictType = value;
  },
});

const fieldSourcePageKey = computed({
  get: () => String(ensureOptionSourceConfig(fieldModalDraft.value).sourcePageKey ?? ''),
  set: (value: string) => {
    ensureOptionSourceConfig(fieldModalDraft.value).sourcePageKey = value;
  },
});

const fieldSourceLabelField = computed({
  get: () => String(ensureOptionSourceConfig(fieldModalDraft.value).labelField ?? ''),
  set: (value: string) => {
    ensureOptionSourceConfig(fieldModalDraft.value).labelField = value;
  },
});

const fieldSourceValueField = computed({
  get: () => String(ensureOptionSourceConfig(fieldModalDraft.value).valueField ?? ''),
  set: (value: string) => {
    ensureOptionSourceConfig(fieldModalDraft.value).valueField = value;
  },
});

const fieldRemoteEndpoint = computed({
  get: () => String(ensureOptionSourceConfig(fieldModalDraft.value).endpoint ?? ''),
  set: (value: string) => {
    ensureOptionSourceConfig(fieldModalDraft.value).endpoint = value;
  },
});

watch(
    () => fieldSourcePageKey.value,
    async (pageKey) => {
      if (!fieldModalVisible.value || !pageKey) {
        remoteSourceFieldCatalog.value = [];
        return;
      }
      try {
        const catalog = await fetchSourceFields(pageKey);
        remoteSourceFieldCatalog.value = catalog;
        if (!fieldSourceLabelField.value || !fieldSourceValueField.value) {
          const suggestion = smartSourceFieldMatch(catalog);
          if (!fieldSourceLabelField.value && suggestion.labelField) {
            fieldSourceLabelField.value = suggestion.labelField;
          }
          if (!fieldSourceValueField.value && suggestion.valueField) {
            fieldSourceValueField.value = suggestion.valueField;
          }
          if (!fieldSourceSearchFieldsText.value && suggestion.searchFields?.length) {
            fieldSourceSearchFieldsText.value = suggestion.searchFields.join(',');
          }
        }
      } catch (error) {
        remoteSourceFieldCatalog.value = [];
      }
    }
);

watch(
    sortedTargets,
    (targets) => {
      if (!targets.length) {
        activeTargetKey.value = '';
        return;
      }
      if (!targets.some((target) => target.targetKey === activeTargetKey.value)) {
        activeTargetKey.value = targets[0].targetKey;
      }
    },
    {immediate: true}
);

watch(
    () => targetDraft.title,
    (value) => {
      if (!targetModalVisible.value) {
        return;
      }
      if (!targetDraft.targetKey) {
        targetDraft.targetKey = slugify(value);
      }
    }
);

onMounted(async () => {
  loadMenuConfig();
  await reloadWorkspace();
});

onBeforeUnmount(() => {
  resizeCleanup?.();
  canvasCleanup?.();
});

onBeforeRouteLeave(async (_to, _from, next) => {
  const fieldOkay = await ensureFieldModalCanClose();
  if (!fieldOkay) {
    next(false);
    return;
  }
  const draftOkay = await ensurePageDraftCanLeave();
  next(draftOkay);
});

async function reloadWorkspace() {
  const [menuRes, configRes, sourceRes] = await Promise.all([
    request.get('/menu/list'),
    fetchFormConfigPages(),
    fetchSourcePages().catch(() => [] as SourcePageItem[]),
  ]);

  menuRecords.value = Array.isArray(menuRes?.data) ? menuRes.data : [];
  if (!expandedNodeIds.value.length) {
    expandedNodeIds.value = menuRecords.value.filter((item) => !item.parentId).map((item) => item.id);
  }
  sourcePages.value = Array.isArray(sourceRes) ? sourceRes : [];
  savedConfigs.value = (Array.isArray(configRes) ? configRes : []).map((config) =>
      normalizePageConfig(config, resolveManifestForConfig(config))
  );

  if (!activeSelectionId.value) {
    const firstSelection = allMenuSelections.value[0] || unclassifiedSelections.value[0];
    if (firstSelection) {
      await loadSelection(firstSelection);
    }
    return;
  }

  const currentSelection = findSelectionById(activeSelectionId.value);
  if (currentSelection) {
    await loadSelection(currentSelection, true);
  }
}

async function handleReloadWorkspace() {
  const fieldOkay = await ensureFieldModalCanClose();
  if (!fieldOkay) {
    return;
  }
  const draftOkay = await ensurePageDraftCanLeave();
  if (!draftOkay) {
    return;
  }
  await reloadFormConfigManifestPages();
  manifestVersion.value += 1;
  await reloadWorkspace();
}

function buildMenuTree(source: MenuRecord[]) {
  const map = new Map<number, MenuTreeNode>();
  const sorted = [...source].sort((left, right) => (left.sort || 0) - (right.sort || 0));
  sorted.forEach((item) => {
    map.set(item.id, {
      ...item,
      path: normalizeRoutePath(item.path || ''),
      children: [],
      selection: null,
      trail: [],
    });
  });

  const roots: MenuTreeNode[] = [];
  sorted.forEach((item) => {
    const node = map.get(item.id)!;
    const parent = item.parentId ? map.get(item.parentId) : null;
    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });

  const visit = (nodes: MenuTreeNode[], trail: string[] = []) => {
    nodes.forEach((node) => {
      node.children.sort((left, right) => (left.sort || 0) - (right.sort || 0));
      node.trail = [...trail, node.title];
      node.selection = buildSelectionForMenuNode(node);
      if (node.children.length) {
        visit(node.children, node.trail);
      }
    });
  };

  visit(roots);
  return roots;
}

function buildSelectionForMenuNode(node: MenuTreeNode): PageSelection | null {
  if (!isSelectableMenuNode(node)) {
    return null;
  }
  const manifest = resolveManifestForMenuNode(node);
  const savedConfig = resolveSavedConfigForMenuNode(node);
  const pageKey = savedConfig?.pageKey || manifest?.pageKey || derivePageKey(node.path || node.component || node.title);
  const routePath = savedConfig?.routePath || manifest?.routePath || normalizeRoutePath(node.path || '');
  const issues = new Set<string>();
  if (!manifest) {
    issues.add('未检测到页面结构，当前节点只能手工补目标。');
  }
  manifest?.detectionIssues?.forEach((item) => issues.add(item));
  savedConfig?.runtimeIssues?.forEach((item) => issues.add(item.message));

  return {
    id: `menu:${node.id}`,
    pageKey,
    routePath,
    pageTitle: savedConfig?.pageTitle || manifest?.pageTitle || node.title,
    manifest,
    savedConfig,
    menuBinding: {
      menuId: node.id,
      menuTitle: node.title,
      menuPath: routePath,
      menuTrail: node.trail,
      componentPath: resolveMenuComponentPath(node.component),
      routeName: node.name,
    },
    status: issues.size ? 'issue' : savedConfig ? 'configured' : 'unconfigured',
    issues: Array.from(issues),
  };
}

function collectSelections(nodes: MenuTreeNode[]): PageSelection[] {
  const result: PageSelection[] = [];
  const visit = (items: MenuTreeNode[]) => {
    items.forEach((item) => {
      if (item.selection) {
        result.push(item.selection);
      }
      if (item.children.length) {
        visit(item.children);
      }
    });
  };
  visit(nodes);
  return result;
}

function buildUnclassifiedSelections(menuSelections: PageSelection[]) {
  const menuPageKeys = new Set(menuSelections.map((item) => item.pageKey));
  const menuRoutes = new Set(menuSelections.map((item) => item.routePath));
  const menuComponents = new Set(menuSelections.map((item) => item.menuBinding?.componentPath).filter(Boolean));
  const result = new Map<string, PageSelection>();

  savedConfigs.value.forEach((config) => {
    const componentPath = config.menuBinding?.componentPath;
    const inMenu =
        menuPageKeys.has(config.pageKey) ||
        (config.routePath && menuRoutes.has(config.routePath)) ||
        (componentPath && menuComponents.has(componentPath));
    if (inMenu) {
      return;
    }
    const manifest = resolveManifestForConfig(config);
    const issues = new Set<string>();
    config.runtimeIssues?.forEach((item) => issues.add(item.message));
    manifest?.detectionIssues?.forEach((item) => issues.add(item));

    result.set(config.pageKey, {
      id: `saved:${config.pageKey}`,
      pageKey: config.pageKey,
      routePath: config.routePath,
      pageTitle: config.pageTitle,
      manifest,
      savedConfig: config,
      menuBinding: config.menuBinding,
      status: issues.size ? 'issue' : 'configured',
      issues: Array.from(issues),
    });
  });

  manifestPages.value.forEach((manifest) => {
    const inMenu =
        menuPageKeys.has(manifest.pageKey) ||
        (manifest.routePath && menuRoutes.has(manifest.routePath)) ||
        (manifest.componentPath && menuComponents.has(manifest.componentPath));
    if (inMenu || result.has(manifest.pageKey)) {
      return;
    }
    result.set(manifest.pageKey, {
      id: `manifest:${manifest.pageKey}`,
      pageKey: manifest.pageKey,
      routePath: manifest.routePath,
      pageTitle: manifest.pageTitle,
      manifest,
      savedConfig: savedConfigMap.value.get(manifest.pageKey) || null,
      menuBinding: {
        componentPath: manifest.componentPath,
        routeName: manifest.routeName,
      },
      status: manifest.detectionIssues?.length ? 'issue' : savedConfigMap.value.has(manifest.pageKey) ? 'configured' : 'unconfigured',
      issues: manifest.detectionIssues || [],
    });
  });

  return Array.from(result.values());
}

function filterMenuTree(nodes: MenuTreeNode[], keyword: string) {
  const normalized = normalizeText(keyword);
  if (!normalized) {
    return nodes;
  }

  return nodes
      .map((node) => {
        const matchedChildren = filterMenuTree(node.children, keyword);
        const selfMatched = [
          node.title,
          node.path,
          node.component,
          node.selection?.pageKey,
          node.selection?.routePath,
        ]
            .filter(Boolean)
            .some((value) => normalizeText(value).includes(normalized));

        if (!selfMatched && !matchedChildren.length) {
          return null;
        }

        return {
          ...node,
          children: matchedChildren,
        } as MenuTreeNode;
      })
      .filter((item): item is MenuTreeNode => Boolean(item));
}

const expandedNodeIds = ref<number[]>([]);

function buildRenderMenuTree(nodes: MenuTreeNode[]): MenuTreeRenderNode[] {
  return nodes.map((node) => ({
    ...node,
    key: node.id,
    selectable: Boolean(node.selection),
    children: buildRenderMenuTree(node.children),
  }));
}

function collectExpandableNodeKeys(nodes: MenuTreeRenderNode[]): number[] {
  const keys: number[] = [];
  nodes.forEach((node) => {
    if (node.children.length) {
      keys.push(node.key);
      keys.push(...collectExpandableNodeKeys(node.children));
    }
  });
  return keys;
}

async function handleTreeSelect(_selectedKeys: (string | number)[], info: { node: MenuTreeRenderNode }) {
  if (info.node?.selection) {
    await handleSelectionChange(info.node.selection);
  }
}

function handleTreeExpand(keys: (string | number)[]) {
  if (hasTreeSearch.value) {
    return;
  }
  expandedNodeIds.value = keys
      .map((key) => Number(key))
      .filter((key) => Number.isFinite(key));
}

function statusLabel(status: SelectionStatus) {
  if (status === 'configured') {
    return '已配置';
  }
  if (status === 'unconfigured') {
    return '未配置';
  }
  return '检测异常';
}

function resolveTreeIcon(node: { icon?: string; selection?: PageSelection | null }) {
  if (node.icon && menuIconMap[node.icon]) {
    return menuIconMap[node.icon];
  }
  return node.selection ? FormOutlined : FolderOpenOutlined;
}

function treeNodeMeta(node: { selection?: PageSelection | null; path?: string; children?: unknown[] }) {
  if (node.selection) {
    return `页面 · ${node.selection.routePath || node.path || node.selection.pageKey}`;
  }
  if (node.path) {
    return `目录 · ${node.path}`;
  }
  if (node.children.length) {
    return `目录 · ${node.children.length} 个子项`;
  }
  return '目录节点';
}

async function handleSelectionChange(selection: PageSelection) {
  const fieldOkay = await ensureFieldModalCanClose();
  if (!fieldOkay) {
    return;
  }
  const draftOkay = await ensurePageDraftCanLeave();
  if (!draftOkay) {
    return;
  }
  await loadSelection(selection);
}

async function loadSelection(selection: PageSelection, preserveSelectionId = false) {
  const manifest =
      selection.manifest ||
      findFormConfigManifestPage(selection.routePath, selection.menuBinding?.componentPath || null) ||
      findFormConfigManifestPageByKey(selection.pageKey);

  let nextDraft: FormPageConfig;
  if (selection.savedConfig) {
    try {
      const saved = await fetchFormConfigPage(selection.savedConfig.pageKey);
      nextDraft = normalizePageConfig(saved, manifest);
    } catch (error) {
      nextDraft = normalizePageConfig(selection.savedConfig, manifest);
    }
    nextDraft.menuBinding = mergeMenuBinding(nextDraft.menuBinding, selection.menuBinding);
  } else if (manifest) {
    nextDraft = createDraftFromManifest({
      manifest,
      pageKey: selection.pageKey,
      routePath: selection.routePath,
      pageTitle: selection.pageTitle,
      menuBinding: selection.menuBinding,
    });
  } else {
    nextDraft = normalizePageConfig(
        {
          pageKey: selection.pageKey,
          routePath: selection.routePath,
          pageTitle: selection.pageTitle,
          enabled: true,
          formInputFollowSystemRadius: false,
          quickSearchFields: [],
          defaultFilterLogic: 'AND',
          fields: [],
          targets: [],
          menuBinding: selection.menuBinding,
          runtimeIssues: [
            {
              code: 'manifest-missing',
              level: 'warning',
              message: '未检测到页面结构，请先补充目标和字段后再保存。',
            },
          ],
        },
        null
    );
  }

  activeDraft.value = nextDraft;
  activeSelectionId.value = preserveSelectionId ? activeSelectionId.value : selection.id;
  savedSnapshot.value = serializeDraft(nextDraft);
  activeTargetKey.value = sortedTargets.value[0]?.targetKey || '';
  sourceFields.value = await safeFetchSourceFields(nextDraft.pageKey);
}

async function safeFetchSourceFields(pageKey: string) {
  try {
    return await fetchSourceFields(pageKey);
  } catch (error) {
    return [] as SourceFieldItem[];
  }
}

function ensureOptionSourceConfig(field?: FormFieldConfig | null) {
  if (!field) {
    return {} as Record<string, unknown>;
  }
  if (!field.optionSourceConfig) {
    field.optionSourceConfig = {};
  }
  return field.optionSourceConfig;
}

function resolveSavedConfigForMenuNode(node: MenuTreeNode) {
  const routePath = normalizeRoutePath(node.path || '');
  const componentPath = resolveMenuComponentPath(node.component);
  return (
      savedConfigs.value.find((config) => config.routePath === routePath) ||
      savedConfigs.value.find((config) => config.menuBinding?.componentPath === componentPath) ||
      null
  );
}

function resolveManifestForMenuNode(node: MenuTreeNode) {
  return findFormConfigManifestPage(normalizeRoutePath(node.path || ''), resolveMenuComponentPath(node.component));
}

function resolveManifestForConfig(config: FormPageConfig) {
  return (
      findFormConfigManifestPage(config.routePath, config.menuBinding?.componentPath || null) ||
      findFormConfigManifestPageByKey(config.pageKey)
  );
}

function findSelectionById(id: string) {
  return [...allMenuSelections.value, ...unclassifiedSelections.value].find((item) => item.id === id) || null;
}

function isSelectableMenuNode(node: MenuRecord) {
  const routePath = normalizeRoutePath(node.path || '');
  const componentPath = normalizeComponentPath(node.component);
  if (!routePath || !componentPath) {
    return false;
  }
  return !/(^|\/)layout$/i.test(componentPath);
}

function normalizeRoutePath(value: string) {
  if (!value) {
    return '';
  }
  return value.startsWith('/') ? value : `/${value}`;
}

function normalizeComponentPath(value?: string | null) {
  return String(value || '')
      .replace(/^\/?src\/views\//, '')
      .replace(/^\/?views\//, '')
      .replace(/^\//, '')
      .replace(/\.vue$/, '');
}

function resolveMenuComponentPath(value?: string | null) {
  const normalized = normalizeComponentPath(value);
  return normalized ? `${normalized}.vue` : '';
}

function formatComponentLabel(value?: string | null) {
  return String(value || '').replace(/\.vue$/, '') || '未检测到组件';
}

function derivePageKey(seed: string) {
  const normalized = normalizeComponentPath(seed).replace(/\//g, '-');
  return slugify(normalized || 'unclassified-page');
}

function slugify(value: string) {
  return String(value || '')
      .trim()
      .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
      .replace(/[\s_/]+/g, '-')
      .replace(/[^a-zA-Z0-9-]/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '')
      .toLowerCase();
}

function normalizeText(value: unknown) {
  return String(value || '').trim().toLowerCase();
}

function mergeMenuBinding(origin?: FormPageMenuBinding, patch?: FormPageMenuBinding) {
  return {
    ...(origin || {}),
    ...(patch || {}),
    menuTrail: patch?.menuTrail?.length ? patch.menuTrail : origin?.menuTrail,
  };
}

function serializeDraft(config: FormPageConfig) {
  return JSON.stringify(clonePageConfig(config));
}

function countVisibleFields(target: FormConfigTarget) {
  return (target.fields || []).filter((field) => isFieldVisible(field)).length;
}

function targetTypeLabel(targetType: FormConfigTargetType) {
  if (targetType === 'table') {
    return '表格';
  }
  if (targetType === 'form') {
    return '表单';
  }
  return 'Modal 表单';
}

function isFieldVisible(field: FormFieldConfig) {
  return (field.visible ?? field.columnVisible ?? true) !== false;
}

function toggleFieldVisible(field: FormFieldConfig, visible: boolean) {
  field.visible = visible;
  field.columnVisible = visible;
}

function updateFieldOrder(field: FormFieldConfig, order: number | null) {
  const safeValue = Number(order ?? 0);
  field.order = safeValue;
  field.columnOrder = safeValue;
}

function updateFieldWidth(field: FormFieldConfig, width: number | null) {
  field.columnWidth = clampNumber(Number(width ?? 180), 96, 520);
}

function tableCardStyle(field: FormFieldConfig) {
  return {
    width: `${Math.max(field.columnWidth || 180, TABLE_PREVIEW_MIN_WIDTH)}px`,
    minWidth: `${TABLE_PREVIEW_MIN_WIDTH}px`,
  };
}

function ensureFieldLayout(field: FormFieldConfig, index: number): FormFieldLayout {
  if (!field.layout) {
    field.layout = {
      x: (index % 2) * 12,
      y: Math.floor(index / 2),
      w: 12,
      h: 1,
    };
  }
  field.layout.x = clampNumber(field.layout.x, 0, 23);
  field.layout.y = Math.max(field.layout.y, 0);
  field.layout.w = clampNumber(field.layout.w, FORM_LAYOUT_MIN_SPAN, 24);
  field.layout.h = clampNumber(field.layout.h, 1, 6);
  if (field.layout.x + field.layout.w > FORM_GRID_COLUMNS) {
    field.layout.x = Math.max(0, FORM_GRID_COLUMNS - field.layout.w);
  }
  return field.layout;
}

function formBlockStyle(field: FormFieldConfig) {
  const layout = ensureFieldLayout(field, activeTargetFields.value.findIndex((item) => item.fieldKey === field.fieldKey));
  return {
    order: layout.y * FORM_GRID_COLUMNS + layout.x,
  };
}

function layoutLabel(field: FormFieldConfig) {
  const layout = ensureFieldLayout(field, activeTargetFields.value.findIndex((item) => item.fieldKey === field.fieldKey));
  return `x:${layout.x} y:${layout.y} w:${layout.w} h:${layout.h}`;
}

function nudgeLayout(field: FormFieldConfig, deltaX: number, deltaY: number) {
  const layout = ensureFieldLayout(field, activeTargetFields.value.findIndex((item) => item.fieldKey === field.fieldKey));
  layout.x = clampNumber(layout.x + deltaX, 0, FORM_GRID_COLUMNS - layout.w);
  layout.y = Math.max(0, layout.y + deltaY);
}

function handleTableDragStart(fieldKey: string) {
  tableDragFieldKey.value = fieldKey;
}

function handleTableDrop(targetFieldKey: string) {
  if (!tableDragFieldKey.value || !activeTarget.value || tableDragFieldKey.value === targetFieldKey) {
    return;
  }
  reorderTargetFields(activeTarget.value, tableDragFieldKey.value, targetFieldKey);
  tableDragFieldKey.value = '';
}

function moveField(target: FormConfigTarget, fieldKey: string, offset: number) {
  const ordered = sortTargetFields(target.fields || [], target.targetType);
  const index = ordered.findIndex((field) => field.fieldKey === fieldKey);
  if (index < 0) {
    return;
  }
  const nextIndex = clampNumber(index + offset, 0, ordered.length - 1);
  if (nextIndex === index) {
    return;
  }
  const [moved] = ordered.splice(index, 1);
  ordered.splice(nextIndex, 0, moved);
  writeTargetFieldOrder(target, ordered);
}

function reorderTargetFields(target: FormConfigTarget, draggedKey: string, targetKey: string) {
  const ordered = sortTargetFields(target.fields || [], target.targetType);
  const fromIndex = ordered.findIndex((field) => field.fieldKey === draggedKey);
  const toIndex = ordered.findIndex((field) => field.fieldKey === targetKey);
  if (fromIndex < 0 || toIndex < 0) {
    return;
  }
  const [moved] = ordered.splice(fromIndex, 1);
  ordered.splice(toIndex, 0, moved);
  writeTargetFieldOrder(target, ordered);
}

function writeTargetFieldOrder(target: FormConfigTarget, orderedFields: FormFieldConfig[]) {
  orderedFields.forEach((field, index) => {
    field.order = index;
    field.columnOrder = index;
  });
  target.fields = [...orderedFields];
}

function startColumnResize(field: FormFieldConfig, event: MouseEvent) {
  resizeCleanup?.();
  const startX = event.clientX;
  const startWidth = field.columnWidth || 180;

  const handleMove = (moveEvent: MouseEvent) => {
    field.columnWidth = clampNumber(startWidth + moveEvent.clientX - startX, 96, 520);
  };

  const handleUp = () => {
    window.removeEventListener('mousemove', handleMove);
    window.removeEventListener('mouseup', handleUp);
    resizeCleanup = null;
  };

  window.addEventListener('mousemove', handleMove);
  window.addEventListener('mouseup', handleUp);
  resizeCleanup = handleUp;
}

function startCanvasInteraction(field: FormFieldConfig, mode: 'move' | 'resize', event: PointerEvent) {
  if (!formCanvasRef.value) {
    return;
  }
  canvasCleanup?.();

  const orderedIndex = activeTargetFields.value.findIndex((item) => item.fieldKey === field.fieldKey);
  const layout = {...ensureFieldLayout(field, orderedIndex)};
  const startX = event.clientX;
  const startY = event.clientY;
  const canvasWidth = formCanvasRef.value.getBoundingClientRect().width;
  const columnWidth = canvasWidth / FORM_GRID_COLUMNS;

  const handleMove = (moveEvent: PointerEvent) => {
    const deltaColumns = Math.round((moveEvent.clientX - startX) / columnWidth);
    const deltaRows = Math.round((moveEvent.clientY - startY) / FORM_GRID_ROW_HEIGHT);
    if (mode === 'move') {
      field.layout = {
        ...layout,
        x: clampNumber(layout.x + deltaColumns, 0, FORM_GRID_COLUMNS - layout.w),
        y: Math.max(0, layout.y + deltaRows),
      };
      return;
    }

    field.layout = {
      ...layout,
      w: clampNumber(layout.w + deltaColumns, FORM_LAYOUT_MIN_SPAN, FORM_GRID_COLUMNS - layout.x),
      h: clampNumber(layout.h + deltaRows, 1, 6),
    };
  };

  const handleUp = () => {
    window.removeEventListener('pointermove', handleMove);
    window.removeEventListener('pointerup', handleUp);
    canvasCleanup = null;
  };

  window.addEventListener('pointermove', handleMove);
  window.addEventListener('pointerup', handleUp);
  canvasCleanup = handleUp;
}

function openCreateTargetModal() {
  if (!activeDraft.value) {
    return;
  }
  targetDraft.title = '';
  targetDraft.targetKey = '';
  targetDraft.targetType = 'modal-form';
  targetModalVisible.value = true;
}

function handleCreateTarget() {
  if (!activeDraft.value) {
    return;
  }
  const title = targetDraft.title.trim();
  const targetKey = slugify(targetDraft.targetKey || title);
  if (!title || !targetKey) {
    message.warning('请填写目标标题和目标 Key');
    return;
  }
  if ((activeDraft.value.targets || []).some((target) => target.targetKey === targetKey)) {
    message.warning('目标 Key 已存在');
    return;
  }

  activeDraft.value.targets = [
    ...(activeDraft.value.targets || []),
    {
      targetKey,
      targetType: targetDraft.targetType,
      title,
      enabled: true,
      order: (activeDraft.value.targets || []).length,
      fields: [],
      sourceSignature: {
        componentPath: activeDraft.value.menuBinding?.componentPath,
        routePath: activeDraft.value.routePath,
        titleCandidate: title,
        detectedBy: 'manual',
      },
    },
  ];
  activeTargetKey.value = targetKey;
  targetModalVisible.value = false;
}

function createFieldCandidate(target: FormConfigTarget) {
  const existingKeys = new Set((target.fields || []).map((field) => field.fieldKey));
  const manifestTarget = activeManifest.value?.targets.find((item) => item.targetKey === target.targetKey) || null;
  const manifestCandidate = manifestTarget?.fields.find((field) => !existingKeys.has(field.fieldKey));
  if (manifestCandidate) {
    return {
      fieldKey: manifestCandidate.fieldKey,
      label: manifestCandidate.label,
      queryKey: manifestCandidate.queryKey || manifestCandidate.fieldKey,
      controlType: manifestCandidate.controlType || 'text',
      visible: true,
      columnVisible: true,
      order: target.fields.length,
      columnOrder: target.fields.length,
      columnWidth: manifestCandidate.columnWidth || 180,
      layout: manifestCandidate.layout ? {...manifestCandidate.layout} : undefined,
      filterEnabled: target.targetType === 'table',
      optionSourceConfig: {},
      placeholder: '',
      previewSchema: manifestCandidate.previewSchema,
    } as FormFieldConfig;
  }

  const sourceCandidate = sourceFields.value.find((field) => !existingKeys.has(field.fieldKey));
  if (sourceCandidate) {
    return {
      fieldKey: sourceCandidate.fieldKey,
      label: sourceCandidate.label,
      queryKey: sourceCandidate.queryKey || sourceCandidate.fieldKey,
      controlType: sourceCandidate.controlType || 'text',
      visible: true,
      columnVisible: true,
      order: target.fields.length,
      columnOrder: target.fields.length,
      columnWidth: 180,
      layout: undefined,
      filterEnabled: target.targetType === 'table' ? sourceCandidate.filterEnabled !== false : false,
      optionSourceConfig: {},
      placeholder: '',
    } as FormFieldConfig;
  }

  return {
    fieldKey: `field-${target.fields.length + 1}`,
    label: `字段 ${target.fields.length + 1}`,
    queryKey: `field${target.fields.length + 1}`,
    controlType: 'text',
    visible: true,
    columnVisible: true,
    order: target.fields.length,
    columnOrder: target.fields.length,
    columnWidth: 180,
    filterEnabled: target.targetType === 'table',
    optionSourceConfig: {},
    placeholder: '',
  } as FormFieldConfig;
}

function openFieldModal(field?: FormFieldConfig) {
  const target = activeTarget.value;
  if (!target) {
    return;
  }
  const candidate = field ? cloneField(field) : createFieldCandidate(target);
  if (target.targetType !== 'table') {
    ensureFieldLayout(candidate, target.fields.length);
  }
  if (!candidate.operatorSet?.length) {
    candidate.operatorSet = defaultOperators(candidate.controlType);
  }
  candidate.defaultOperator = candidate.defaultOperator || candidate.operatorSet?.[0];
  candidate.defaultMatchMode = candidate.defaultMatchMode || ((candidate.controlType || 'text') === 'text' ? 'contains' : 'exact');
  candidate.optionSourceConfig = {...(candidate.optionSourceConfig || {})};

  fieldModalTargetKey.value = target.targetKey;
  fieldModalDraft.value = candidate;
  fieldModalSnapshot.value = JSON.stringify(candidate);
  fieldStaticOptionsText.value = formatStaticOptions(candidate.optionSourceConfig?.options);
  fieldSourceSearchFieldsText.value = Array.isArray(candidate.optionSourceConfig?.searchFields)
      ? (candidate.optionSourceConfig?.searchFields as string[]).join(',')
      : '';
  fieldModalVisible.value = true;
}

function handleFieldModalVisibilityChange(next: boolean) {
  if (next) {
    fieldModalVisible.value = true;
    return;
  }
  void ensureFieldModalCanClose();
}

async function ensureFieldModalCanClose() {
  if (!fieldModalVisible.value) {
    return true;
  }
  if (!fieldModalDirty.value) {
    fieldModalVisible.value = false;
    fieldModalDraft.value = null;
    return true;
  }

  const decision = await openGuardDialog({
    title: '字段内容未保存',
    message: '当前字段修改仅保存在字段弹窗草稿中，关闭前要不要先写回页面草稿？',
    saveText: '保存到草稿',
    discardText: '放弃修改',
  });

  if (decision === 'save') {
    return handleFieldModalSave();
  }
  if (decision === 'discard') {
    fieldModalVisible.value = false;
    fieldModalDraft.value = null;
    return true;
  }
  return false;
}

function handleFieldModalSave() {
  if (!fieldModalDraft.value || !fieldModalTarget.value) {
    return false;
  }
  if (!fieldModalDraft.value.fieldKey.trim() || !fieldModalDraft.value.label.trim()) {
    message.warning('字段标题和字段 Key 不能为空');
    return false;
  }

  applyFieldModalSourceConfig();
  upsertTargetField(fieldModalTarget.value, fieldModalDraft.value);
  fieldModalVisible.value = false;
  fieldModalDraft.value = null;
  message.success('字段已保存到页面草稿');
  return true;
}

function applyFieldModalSourceConfig() {
  if (!fieldModalDraft.value) {
    return;
  }
  const optionSourceConfig = ensureOptionSourceConfig(fieldModalDraft.value);
  if (fieldModalDraft.value.optionSourceType === 'static') {
    optionSourceConfig.options = parseStaticOptions(fieldStaticOptionsText.value);
  }
  if (fieldModalDraft.value.optionSourceType === 'remote-form') {
    optionSourceConfig.searchFields = fieldSourceSearchFieldsText.value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
  }
  if (!fieldModalDraft.value.optionSourceType) {
    fieldModalDraft.value.optionSourceConfig = {};
  }
}

function handleRemoveField(fieldKey: string) {
  const target = activeTarget.value;
  if (!target) {
    return;
  }
  Modal.confirm({
    title: '删除字段',
    content: '删除后只会从当前页面草稿移除，保存配置后才会真正生效。',
    okText: '删除字段',
    cancelText: '取消',
    centered: true,
    onOk: () => {
      target.fields = (target.fields || []).filter((field) => field.fieldKey !== fieldKey);
      if (fieldModalDraft.value?.fieldKey === fieldKey) {
        fieldModalVisible.value = false;
        fieldModalDraft.value = null;
      }
    },
  });
}

function handleRemoveTarget() {
  const target = activeTarget.value;
  if (!activeDraft.value || !target) {
    return;
  }
  Modal.confirm({
    title: '删除目标',
    content: '删除后会移除当前目标下的全部字段草稿。',
    okText: '删除目标',
    cancelText: '取消',
    centered: true,
    onOk: () => {
      activeDraft.value!.targets = (activeDraft.value!.targets || []).filter((item) => item.targetKey !== target.targetKey);
      activeTargetKey.value = sortedTargets.value[0]?.targetKey || '';
    },
  });
}

function handleSyncFormTarget() {
  if (!activeTarget.value || !activeTableTarget.value || activeTarget.value.targetType === 'table') {
    return;
  }
  syncFormTargetWithTable(activeTableTarget.value, activeTarget.value);
  message.success('表单顺序已同步到页面草稿');
}

async function handleSavePage() {
  const fieldOkay = await ensureFieldModalCanClose();
  if (!fieldOkay || !activeDraft.value) {
    return false;
  }
  const validationError = validatePageDraft(activeDraft.value);
  if (validationError) {
    message.warning(validationError);
    return false;
  }

  savingPage.value = true;
  try {
    const manifest = activeManifest.value;
    const payload = normalizePageConfig(clonePageConfig(activeDraft.value), manifest);
    const exists = savedConfigs.value.some((config) => config.pageKey === payload.pageKey);
    const saved = exists
        ? await updateFormConfigPage(payload.pageKey, payload)
        : await createFormConfigPage(payload);

    const normalizedSaved = normalizePageConfig(saved, manifest);
    activeDraft.value = normalizedSaved;
    savedSnapshot.value = serializeDraft(normalizedSaved);
    savedConfigs.value = (await fetchFormConfigPages()).map((config) => normalizePageConfig(config, resolveManifestForConfig(config)));
    sourceFields.value = await safeFetchSourceFields(normalizedSaved.pageKey);
    message.success('配置已保存并立即生效');
    return true;
  } catch (error) {
    message.error('保存失败，请检查配置后重试');
    return false;
  } finally {
    savingPage.value = false;
  }
}

function validatePageDraft(config: FormPageConfig) {
  if (!config.pageKey.trim()) {
    return '页面键不能为空';
  }
  if (!config.pageTitle.trim()) {
    return '页面标题不能为空';
  }
  const targetKeys = new Set<string>();
  for (const target of config.targets || []) {
    if (!target.targetKey.trim()) {
      return '存在未填写 targetKey 的目标';
    }
    if (targetKeys.has(target.targetKey)) {
      return `目标 Key 重复：${target.targetKey}`;
    }
    targetKeys.add(target.targetKey);
    const fieldKeys = new Set<string>();
    for (const field of target.fields || []) {
      if (!field.fieldKey.trim()) {
        return `目标 ${target.title} 存在空字段 Key`;
      }
      if (fieldKeys.has(field.fieldKey)) {
        return `目标 ${target.title} 存在重复字段 Key：${field.fieldKey}`;
      }
      fieldKeys.add(field.fieldKey);
    }
  }
  return '';
}

async function ensurePageDraftCanLeave() {
  if (!pageDirty.value) {
    return true;
  }
  const decision = await openGuardDialog({
    title: '页面草稿未保存',
    message: '当前页面改动还没有提交到服务器。切换页面或离开前，是否先保存配置？',
    saveText: '保存并离开',
    discardText: '放弃草稿',
  });

  if (decision === 'save') {
    return handleSavePage();
  }
  return decision === 'discard';
}

function openGuardDialog(options: { title: string; message: string; saveText: string; discardText: string }) {
  guardDialog.title = options.title;
  guardDialog.message = options.message;
  guardDialog.saveText = options.saveText;
  guardDialog.discardText = options.discardText;
  guardModalVisible.value = true;
  return new Promise<GuardDecision>((resolve) => {
    guardResolver = resolve;
  });
}

async function handleGuardDecision(decision: GuardDecision) {
  if (!guardResolver) {
    guardModalVisible.value = false;
    return;
  }
  if (decision === 'save') {
    guardBusy.value = true;
  }
  const resolver = guardResolver;
  guardResolver = null;
  guardModalVisible.value = false;
  guardBusy.value = false;
  resolver(decision);
}

function defaultOperators(controlType?: string) {
  switch (controlType) {
    case 'text':
      return ['contains', 'equals'];
    case 'select':
    case 'remote-api':
    case 'remote-form':
      return ['equals', 'in'];
    case 'date':
      return ['on'];
    case 'date-range':
    case 'number-range':
      return ['between'];
    case 'number':
      return ['equals', 'gt', 'lt'];
    default:
      return ['equals'];
  }
}

function formatStaticOptions(raw: unknown) {
  if (!Array.isArray(raw)) {
    return '';
  }
  return raw
      .map((item) => {
        const option = item as Record<string, unknown>;
        return `${String(option.label ?? '')}:${String(option.value ?? '')}`;
      })
      .join('\n');
}

function parseStaticOptions(value: string) {
  return value
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [label, ...rest] = line.split(':');
        return {
          label: label?.trim() || '',
          value: (rest.join(':').trim() || label?.trim() || '') as string,
        };
      });
}

function clampNumber(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Number.isFinite(value) ? value : min));
}
</script>

<style scoped>
.designer-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 28px;
  min-height: calc(100vh - 220px);
}

.designer-sidebar,
.designer-card,
.designer-empty,
.tree-panel,
.unclassified-panel {
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  box-shadow: none;
}

.designer-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 24px 0 0;
  border: none;
  border-right: 1px solid var(--mono-line);
  border-radius: 0;
}

.designer-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.designer-card,
.designer-empty,
.tree-panel,
.unclassified-panel {
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.section-title {
  margin: 0;
  color: var(--mono-text);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.15;
}

.section-sub,
.sidebar-subhead span,
.editor-hint,
.target-source-text,
.tree-row-content span,
.unclassified-row span,
.field-modal-head span,
.guard-message {
  color: var(--mono-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.sidebar-head,
.card-head,
.sidebar-subhead {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.sidebar-search {
  border-radius: var(--mono-radius-pill);
}

.tree-panel {
  flex: 1 1 auto;
  overflow: auto;
  padding: 8px 0 0;
}

.tree-row,
.unclassified-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 6px 10px;
  border: 1px solid transparent;
  border-radius: var(--mono-radius-sm);
  background: transparent;
  color: var(--mono-text);
  text-align: left;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.tree-row--selectable:hover,
.unclassified-row:hover {
  border-color: var(--mono-line-strong);
  background: var(--mono-surface-soft);
}

.tree-row--selected,
.unclassified-row--selected {
  border-color: #111111;
  background: rgba(17, 17, 17, 0.05);
}

.tree-row--selectable {
  cursor: pointer;
}

.tree-menu :deep(.ant-tree-list-holder-inner) {
  align-items: stretch;
}

.tree-menu :deep(.ant-tree-treenode) {
  width: 100%;
  padding: 1px 0;
}

.tree-menu :deep(.ant-tree-node-content-wrapper) {
  width: calc(100% - 20px);
  min-height: 40px;
  padding: 0 8px 0 6px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  color: #4b5563;
  transition: background-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.tree-menu :deep(.ant-tree-node-content-wrapper:hover) {
  background: #f8f8f8;
  color: #111827;
}

.tree-menu :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background: #f1f3f5 !important;
  color: #111827 !important;
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.06);
}

.tree-menu :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected:hover) {
  background: #eceff3 !important;
}

.tree-menu :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected .tree-row-title) {
  color: #111827;
}

.tree-menu :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected .tree-row-content span) {
  color: #4b5563;
}

.tree-menu :deep(.ant-tree-switcher) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  min-width: 18px;
  height: 40px;
  color: #9ca3af;
}

.tree-menu :deep(.ant-tree-switcher-noop) {
  align-items: stretch;
}

.tree-menu :deep(.ant-tree-indent-unit) {
  width: 18px;
}

.tree-menu :deep(.ant-tree-show-line .ant-tree-indent-unit::before),
.tree-menu :deep(.ant-tree-show-line .ant-tree-switcher-leaf-line::before),
.tree-menu :deep(.ant-tree-show-line .ant-tree-switcher-leaf-line::after) {
  border-color: #e5e7eb;
}

.tree-switcher-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #9ca3af;
}

.tree-switcher-icon :deep(svg) {
  font-size: 12px;
}

.tree-switcher-line {
  display: block;
  width: 100%;
  height: 100%;
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.tree-node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #9ca3af;
  flex: 0 0 auto;
}

.tree-node-icon :deep(svg) {
  font-size: 14px;
}

.tree-row-content,
.unclassified-row > div {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 1px;
  flex: 1 1 auto;
}

.tree-row-title,
.unclassified-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.3;
  color: #1f2937;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.tree-row-content span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  line-height: 1.35;
  font-weight: 400;
  color: #6b7280;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.status-chip,
.mono-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-pill);
  font-size: 11px;
  font-weight: 500;
  line-height: 1.35;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
  box-sizing: border-box;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  padding: 20px 16px;
  border: 1px dashed var(--mono-line);
  border-radius: var(--mono-radius-sm);
  color: var(--mono-text-secondary);
  font-size: 13px;
  text-align: center;
}

.status-chip--configured {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.status-chip--unconfigured,
.mono-badge--soft {
  background: #ffffff;
  color: var(--mono-text);
}

.status-chip--issue,
.issue-item {
  border-color: rgba(17, 17, 17, 0.18);
  background: rgba(17, 17, 17, 0.04);
  color: #111111;
}

.draft-banner {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: var(--mono-radius-sm);
  background: rgba(17, 17, 17, 0.04);
  color: var(--mono-text);
  font-size: 13px;
  font-weight: 600;
}

.card-head-meta,
.switch-strip,
.target-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-grid,
.modal-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px 16px;
}

.overview-grid {
  margin-top: 18px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  grid-column: span 3;
}

.field-block--wide {
  grid-column: span 5;
}

.field-block--full {
  grid-column: 1 / -1;
}

.field-block span {
  color: var(--mono-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.switch-strip {
  margin-top: 18px;
}

.switch-tile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 220px;
  padding: 12px 14px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
}

.switch-tile span,
.target-toggle span {
  color: var(--mono-text);
  font-size: 13px;
  font-weight: 600;
}

.issue-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.issue-item {
  padding: 10px 12px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: var(--mono-radius-sm);
  font-size: 13px;
  line-height: 1.5;
}

.target-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.target-tab {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 180px;
  padding: 14px 16px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  color: var(--mono-text);
  text-align: left;
}

.target-tab--active {
  border-color: #111111;
  background: rgba(17, 17, 17, 0.04);
}

.target-tab strong {
  font-size: 15px;
}

.target-tab span,
.target-tab small {
  color: var(--mono-text-secondary);
  font-size: 12px;
}

.card-head--target {
  align-items: center;
}

.target-head-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  flex: 1 1 auto;
}

.target-title-input {
  max-width: 320px;
}

.target-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.target-body {
  margin-top: 18px;
}

.table-preview-track {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 6px;
  margin-top: 16px;
}

.table-header-preview {
  display: flex;
  gap: 0;
  overflow-x: auto;
  margin-top: 16px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
}

.table-header-cell {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 88px;
  padding: 14px 32px 14px 14px;
  border-right: 1px solid var(--mono-line);
  background: #ffffff;
}

.table-header-cell:last-child {
  border-right: none;
}

.table-header-cell strong,
.table-header-cell .field-key {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-config-list {
  margin-top: 14px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  overflow: hidden;
}

.field-config-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(340px, auto) auto;
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--mono-line);
}

.field-config-row:last-child {
  border-bottom: none;
}

.field-config-row--hidden {
  opacity: 0.52;
}

.field-config-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.field-config-title strong,
.field-config-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-config-title span {
  color: var(--mono-text-secondary);
  font-size: 12px;
}

.field-config-controls,
.field-config-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.field-config-controls label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--mono-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.compact-number {
  width: 88px;
}

.table-preview-card {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 218px;
  padding: 16px 18px 18px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
}

.table-preview-card--hidden,
.form-block--hidden {
  opacity: 0.48;
  border-style: dashed;
}

.table-preview-top,
.table-preview-main,
.table-preview-controls,
.table-preview-actions,
.form-block-head,
.form-block-body,
.form-block-actions,
.form-nudge-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-preview-top,
.form-block-body {
  justify-content: space-between;
}

.drag-label,
.field-key {
  color: var(--mono-text-secondary);
  font-size: 12px;
}

.table-preview-main {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.table-preview-controls {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.table-preview-controls label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--mono-text-secondary);
  font-size: 12px;
}

.table-preview-actions {
  margin-top: auto;
  flex-wrap: wrap;
}

.table-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  width: 12px;
  height: 100%;
  border: none;
  border-radius: 0 var(--mono-radius-sm) var(--mono-radius-sm) 0;
  background: linear-gradient(180deg, rgba(17, 17, 17, 0), rgba(17, 17, 17, 0.14), rgba(17, 17, 17, 0));
  cursor: col-resize;
}

.form-canvas {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  grid-auto-rows: minmax(180px, auto);
  gap: 14px;
  margin-top: 16px;
  padding: 18px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  min-height: 260px;
}

.form-block {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 180px;
  padding: 16px;
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
  overflow: hidden;
}

.form-block-head {
  justify-content: space-between;
  align-items: start;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
}

.form-block-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.form-block-title span {
  color: var(--mono-text-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-block-body {
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.form-block-body span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--mono-line);
  border-radius: 999px;
  background: #ffffff;
  color: var(--mono-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-block-actions {
  margin-top: auto;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--mono-line);
}

.mini-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--mono-line);
  border-radius: 6px;
  background: #ffffff;
  color: var(--mono-text);
  font-size: 12px;
  font-weight: 600;
  flex: 0 0 auto;
}

.form-block-resizer {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 18px;
  height: 18px;
  border: none;
  background: linear-gradient(135deg, rgba(17, 17, 17, 0) 0%, rgba(17, 17, 17, 0) 45%, rgba(17, 17, 17, 0.36) 45%, rgba(17, 17, 17, 0.36) 58%, rgba(17, 17, 17, 0) 58%, rgba(17, 17, 17, 0) 100%);
  cursor: nwse-resize;
}

.designer-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  color: var(--mono-text-secondary);
  font-size: 14px;
}

.field-modal-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--mono-line);
}

.field-modal-head strong {
  display: block;
  color: var(--mono-text);
  font-size: 16px;
}

.field-dirty-indicator {
  color: var(--mono-text);
  font-size: 12px;
  font-weight: 600;
}

.field-modal-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-modal-section h3 {
  margin: 0;
  color: var(--mono-text);
  font-size: 15px;
  font-weight: 700;
}

.switch-strip--modal {
  margin-top: 0;
}

.source-panel {
  padding: 14px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: rgba(17, 17, 17, 0.02);
}

.guard-message {
  margin: 0;
}

.guard-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

@media (max-width: 1200px) {
  .designer-shell {
    grid-template-columns: 1fr;
  }

  .designer-sidebar {
    padding-right: 0;
    padding-bottom: 24px;
    border-right: none;
    border-bottom: 1px solid var(--mono-line);
  }

  .overview-grid,
  .modal-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .field-block {
    grid-column: span 3;
  }

  .field-block--wide,
  .field-block--full {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .designer-sidebar,
  .designer-card,
  .designer-empty,
  .tree-panel,
  .unclassified-panel {
    padding: 0;
  }

  .overview-grid,
  .modal-grid {
    grid-template-columns: 1fr;
  }

  .field-block,
  .field-block--wide,
  .field-block--full {
    grid-column: 1 / -1;
  }

  .switch-tile {
    min-width: 100%;
  }

  .card-head,
  .card-head--target,
  .field-modal-head {
    flex-direction: column;
    align-items: stretch;
  }

  .target-head-actions,
  .guard-footer {
    justify-content: flex-start;
  }

  .field-config-row {
    grid-template-columns: 1fr;
  }

  .field-config-controls,
  .field-config-actions {
    justify-content: flex-start;
  }

  .form-canvas {
    grid-template-columns: 1fr;
  }
}
</style>
