<template>
  <WorkspacePage title="权限管理" variant="menu-list">
    <template #meta>
      <a-tag class="permission-meta-tag">
        {{ currentMetaLabel }}
      </a-tag>
    </template>

    <template #filters>
      <div class="permission-filters">
        <StandardInput
          v-model:value="searchText"
          width="260px"
          placeholder="搜索菜单标题或路径"
          @pressEnter="loadMenus"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </StandardInput>
        <StandardButton type="search" icon="search" @click="loadMenus">
          查询
        </StandardButton>
        <StandardButton type="reset" icon="reload" @click="resetFilters">
          重置
        </StandardButton>
      </div>
    </template>

    <template #actions>
      <div class="permission-toolbar">
        <div class="permission-scope-tabs">
          <button
            v-for="item in scopeOptions"
            :key="item.value"
            type="button"
            :class="['permission-scope-tab', { 'is-active': scope === item.value }]"
            @click="scope = item.value"
          >
            <span class="permission-scope-title">{{ item.label }}</span>
            <span class="permission-scope-desc">{{ item.description }}</span>
          </button>
          <StandardButton type="default" icon="reload" @click="refreshAll">
            刷新
          </StandardButton>
        </div>

        <div v-if="scope === 'role'" class="permission-subject-strip">
          <button
            v-for="role in roleOptions"
            :key="role.value"
            type="button"
            :class="['permission-subject-card', { 'is-active': activeRole === role.value }]"
            @click="activeRole = role.value"
          >
            <span class="permission-subject-title">{{ role.label }}</span>
            <span class="permission-subject-meta">{{ getRoleViewCount(role.value) }} 个可见菜单</span>
          </button>
        </div>

        <div v-else class="permission-user-panel">
          <div class="permission-user-toolbar">
            <StandardInput
              v-model:value="userSearchText"
              width="260px"
              placeholder="搜索账号、姓名或昵称"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </StandardInput>
          </div>

          <div v-if="filteredUsers.length" class="permission-user-grid">
            <button
              v-for="user in filteredUsers"
              :key="user.id"
              type="button"
              :class="['permission-user-card', { 'is-active': activeUserId === user.id }]"
              @click="activeUserId = user.id"
            >
              <div class="permission-user-name">{{ formatUserLabel(user) }}</div>
              <div class="permission-user-meta">
                {{ formatRoleLabel(user.role) }} · {{ getUserOverrideCount(user) }} 条覆盖
              </div>
              <div class="permission-user-subline">{{ user.username }}</div>
            </button>
          </div>
          <a-empty v-else description="暂无可配置账号" />
        </div>
      </div>
    </template>

    <section class="workspace-subsection">
      <StandardTable
        :configStyle="currentStyle"
        surface="menu-list"
        :loading="loading"
        :dataSource="filteredMenus"
        :columns="columns"
        :pagination="false"
        rowKey="id"
        :scroll="{ x: 1320 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'trail'">
            <div class="permission-menu-cell">
              <div class="permission-menu-title">{{ record.title }}</div>
              <div class="permission-menu-trail">{{ formatTrail(record.trail) }}</div>
            </div>
          </template>

          <template v-else-if="column.key === 'path'">
            <span class="permission-path">{{ record.path || `menu:${record.id}` }}</span>
          </template>

          <template v-else-if="column.key === 'type'">
            <a-tag class="permission-type-tag">
              {{ isLeafMenu(record) ? '页面菜单' : '目录菜单' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'roles'">
            <div class="permission-tag-row">
              <a-tag
                v-for="role in splitRoles(record.roles)"
                :key="`${record.id}-${role}`"
                :class="['permission-role-tag', { 'is-active': role === activeRole }]"
              >
                {{ formatRoleLabel(role) }}
              </a-tag>
              <span v-if="splitRoles(record.roles).length === 0" class="permission-empty-text">未配置</span>
            </div>
          </template>

          <template v-else-if="column.key === 'roleActions'">
            <div class="permission-tag-row">
              <a-tag
                v-for="action in getRoleActions(record, currentRoleForTable)"
                :key="`${record.id}-${currentRoleForTable}-${action}`"
                class="permission-action-tag"
              >
                {{ formatActionLabel(action) }}
              </a-tag>
              <span v-if="getRoleActions(record, currentRoleForTable).length === 0" class="permission-empty-text">
                无访问权限
              </span>
            </div>
          </template>

          <template v-else-if="column.key === 'subjectActions'">
            <div class="permission-tag-row">
              <a-tag
                v-for="action in getCurrentActions(record)"
                :key="`${record.id}-${currentSubjectKey}-${action}`"
                class="permission-action-tag"
              >
                {{ formatActionLabel(action) }}
              </a-tag>
              <span v-if="getCurrentActions(record).length === 0" class="permission-empty-text">
                无访问权限
              </span>
            </div>
          </template>

          <template v-else-if="column.key === 'source'">
            <a-tag :class="['permission-source-tag', `is-${getUserActionSource(record)}`]">
              {{ formatSourceLabel(getUserActionSource(record)) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'operate'">
            <StandardButton type="link" size="sm" @click="openPermissionModal(record)">
              配置权限
            </StandardButton>
          </template>
        </template>
      </StandardTable>
    </section>
  </WorkspacePage>

  <StandardModal
    v-model:visible="modalVisible"
    :title="modalTitle"
    @ok="handleSavePermissions"
  >
    <div class="permission-modal">
      <div class="permission-modal-summary">
        <div class="permission-modal-title">{{ editingMenu?.title || '-' }}</div>
        <div class="permission-modal-subtitle">
          {{ editingMenu?.path || `menu:${editingMenu?.id || '-'}` }}
        </div>
      </div>

      <a-alert
        type="info"
        show-icon
        class="permission-modal-alert"
        :message="modalAlert.message"
        :description="modalAlert.description"
      />

      <template v-if="scope === 'user'">
        <div class="permission-mode-tabs">
          <button
            v-for="item in userModeOptions"
            :key="item.value"
            type="button"
            :class="['permission-mode-card', { 'is-active': editingMode === item.value }]"
            @click="editingMode = item.value"
          >
            <span class="permission-mode-title">{{ item.label }}</span>
            <span class="permission-mode-desc">{{ item.description }}</span>
          </button>
        </div>

        <div v-if="editingMode === 'inherit'" class="permission-preview-block">
          <div class="permission-preview-label">继承角色模板结果</div>
          <div class="permission-tag-row">
            <a-tag
              v-for="action in inheritedActions"
              :key="`inherit-${action}`"
              class="permission-action-tag"
            >
              {{ formatActionLabel(action) }}
            </a-tag>
            <span v-if="inheritedActions.length === 0" class="permission-empty-text">继承后无访问权限</span>
          </div>
        </div>

        <div v-else-if="editingMode === 'deny'" class="permission-preview-block">
          <div class="permission-preview-label">当前账号将被明确禁止访问此菜单</div>
        </div>
      </template>

      <div v-if="scope === 'role' || editingMode === 'custom'" class="permission-action-picker">
        <div class="permission-preview-label">
          {{ scope === 'role' ? '角色模板权限' : '账号覆盖权限' }}
        </div>
        <div class="permission-action-pills">
          <button
            v-for="action in editingActionOptions"
            :key="action.value"
            type="button"
            :class="['permission-action-pill', { 'is-active': editingActions.includes(action.value) }]"
            @click="toggleEditingAction(action.value)"
          >
            {{ action.label }}
          </button>
        </div>
      </div>
    </div>
  </StandardModal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import request from '@/request'
import { usePageStyle } from '@/hooks/usePageStyle'
import StandardButton from '@/components/common/StandardButton.vue'
import StandardInput from '@/components/common/StandardInput.vue'
import StandardTable from '@/components/common/StandardTable.vue'
import StandardModal from '@/components/common/StandardModal.vue'
import WorkspacePage from '@/components/common/WorkspacePage.vue'

type PermissionScope = 'role' | 'user'
type PermissionAction = 'view' | 'create' | 'update' | 'delete'
type UserPermissionMode = 'inherit' | 'custom' | 'deny'
type PermissionSource = 'inherit' | 'override' | 'deny'

interface MenuRecord {
  id: number
  parentId: number | null
  title: string
  name?: string
  path?: string
  component?: string | null
  icon?: string | null
  sort?: number
  roles?: string
  hidden?: boolean
  componentStyle?: string | null
  permissionConfig?: string | null
  trail?: string[]
}

interface UserSubject {
  id: number
  username: string
  realName?: string
  nickname?: string
  role: string
  status?: string
  permissionConfig?: string | null
}

const scopeOptions = [
  { value: 'role', label: '角色模板', description: '定义角色基线权限' },
  { value: 'user', label: '账号例外', description: '给具体账号单独加减权限' },
] as const

const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'STAFF', label: '员工' },
  { value: 'COACH', label: '教练' },
  { value: 'MEMBER', label: '会员' },
]

const actionOptions: Array<{ value: PermissionAction; label: string }> = [
  { value: 'view', label: '查看' },
  { value: 'create', label: '新增' },
  { value: 'update', label: '编辑' },
  { value: 'delete', label: '删除' },
]

const userModeOptions = [
  { value: 'inherit', label: '继承模板', description: '沿用角色模板权限' },
  { value: 'custom', label: '账号覆盖', description: '给当前账号单独配置动作' },
  { value: 'deny', label: '明确禁用', description: '当前账号直接不可访问' },
] as const

const { currentStyle, loadMenuConfig } = usePageStyle()

const loading = ref(false)
const scope = ref<PermissionScope>('role')
const searchText = ref('')
const userSearchText = ref('')
const activeRole = ref('ADMIN')
const activeUserId = ref<number | null>(null)
const menus = ref<MenuRecord[]>([])
const users = ref<UserSubject[]>([])
const modalVisible = ref(false)
const editingMenu = ref<MenuRecord | null>(null)
const editingActions = ref<PermissionAction[]>([])
const editingMode = ref<UserPermissionMode>('custom')

const filteredMenus = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) {
    return menus.value
  }
  return menus.value.filter((item) => {
    const title = String(item.title || '').toLowerCase()
    const path = String(item.path || '').toLowerCase()
    return title.includes(keyword) || path.includes(keyword)
  })
})

const filteredUsers = computed(() => {
  const keyword = userSearchText.value.trim().toLowerCase()
  if (!keyword) {
    return users.value
  }
  return users.value.filter((item) => {
    return [
      item.username,
      item.realName,
      item.nickname,
      formatRoleLabel(item.role),
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
  })
})

const activeUser = computed(() => {
  return users.value.find((item) => item.id === activeUserId.value) || null
})

const activeRoleLabel = computed(() => formatRoleLabel(activeRole.value))
const activeUserLabel = computed(() => {
  if (!activeUser.value) {
    return '未选择账号'
  }
  return formatUserLabel(activeUser.value)
})
const currentRoleForTable = computed(() => {
  return scope.value === 'role' ? activeRole.value : (activeUser.value?.role || '')
})
const currentSubjectKey = computed(() => {
  return scope.value === 'role'
    ? activeRole.value
    : String(activeUser.value?.id || 'none')
})
const currentMetaLabel = computed(() => {
  if (scope.value === 'role') {
    return `角色模板 · ${activeRoleLabel.value} · ${getRoleViewCount(activeRole.value)} 个可见菜单`
  }
  return `账号例外 · ${activeUserLabel.value} · ${activeUser.value ? getUserOverrideCount(activeUser.value) : 0} 条覆盖`
})

const columns = computed(() => {
  if (scope.value === 'role') {
    return [
      { title: '菜单', dataIndex: 'title', key: 'trail', width: 280 },
      { title: '路径', dataIndex: 'path', key: 'path', width: 220 },
      { title: '类型', key: 'type', width: 120, align: 'center' },
      { title: '已配置角色', dataIndex: 'roles', key: 'roles', width: 220 },
      { title: '当前模板权限', key: 'subjectActions', width: 260 },
      { title: '操作', key: 'operate', width: 120, fixed: 'right' },
    ]
  }

  return [
    { title: '菜单', dataIndex: 'title', key: 'trail', width: 280 },
    { title: '路径', dataIndex: 'path', key: 'path', width: 220 },
    { title: '类型', key: 'type', width: 120, align: 'center' },
    { title: `${formatRoleLabel(currentRoleForTable.value)}模板权限`, key: 'roleActions', width: 240 },
    { title: '当前账号权限', key: 'subjectActions', width: 260 },
    { title: '来源', key: 'source', width: 140, align: 'center' },
    { title: '操作', key: 'operate', width: 120, fixed: 'right' },
  ]
})

const modalTitle = computed(() => {
  if (!editingMenu.value) {
    return '配置权限'
  }
  return scope.value === 'role'
    ? `${activeRoleLabel.value} · ${editingMenu.value.title}`
    : `${activeUserLabel.value} · ${editingMenu.value.title}`
})

const editingActionOptions = computed(() => {
  return editingMenu.value ? getAvailableActions(editingMenu.value) : actionOptions
})

const inheritedActions = computed(() => {
  if (!editingMenu.value || !activeUser.value) {
    return []
  }
  return getRoleActions(editingMenu.value, activeUser.value.role)
})

const modalAlert = computed(() => {
  if (scope.value === 'role') {
    return {
      message: `正在配置 ${activeRoleLabel.value} 的角色模板`,
      description: '角色模板是基线权限。页面菜单支持查看、新增、编辑、删除；目录菜单只支持查看。',
    }
  }

  return {
    message: `正在配置 ${activeUserLabel.value} 的账号例外`,
    description: '账号例外会覆盖角色模板。你可以让账号继承模板，也可以额外放开或明确禁用某个菜单。',
  }
})

onMounted(async () => {
  loadMenuConfig()
  await refreshAll()
})

watch(filteredUsers, (nextUsers) => {
  if (scope.value !== 'user') {
    return
  }

  if (!nextUsers.some((item) => item.id === activeUserId.value)) {
    activeUserId.value = nextUsers[0]?.id ?? null
  }
}, { immediate: true })

watch(scope, (nextScope) => {
  if (nextScope === 'user' && !activeUserId.value) {
    activeUserId.value = filteredUsers.value[0]?.id ?? null
  }
})

async function refreshAll() {
  await Promise.all([loadMenus(), loadUsers()])
}

async function loadMenus() {
  loading.value = true
  try {
    const res = await request.get('/menu/list')
    if (res.code !== 200) {
      message.error(res.msg || '加载菜单失败')
      return
    }
    menus.value = buildMenuTrails(Array.isArray(res.data) ? res.data : [])
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  const res = await request.get('/user/permission-subjects')
  if (res.code !== 200) {
    message.error(res.msg || '加载账号失败')
    return
  }
  users.value = Array.isArray(res.data) ? res.data : []
  if (!users.value.some((item) => item.id === activeUserId.value)) {
    activeUserId.value = users.value[0]?.id ?? null
  }
}

function resetFilters() {
  searchText.value = ''
  userSearchText.value = ''
  loadMenus()
}

function openPermissionModal(menu: MenuRecord) {
  if (scope.value === 'user' && !activeUser.value) {
    message.warning('请先选择一个账号')
    return
  }

  editingMenu.value = { ...menu }
  if (scope.value === 'role') {
    editingMode.value = 'custom'
    editingActions.value = [...getRoleActions(menu, activeRole.value)]
  } else {
    const source = getUserActionSource(menu)
    if (source === 'inherit') {
      editingMode.value = 'inherit'
      editingActions.value = [...getUserActions(menu, activeUser.value)]
    } else if (source === 'deny') {
      editingMode.value = 'deny'
      editingActions.value = []
    } else {
      editingMode.value = 'custom'
      editingActions.value = [...getUserActions(menu, activeUser.value)]
    }
  }
  modalVisible.value = true
}

async function handleSavePermissions() {
  if (!editingMenu.value) {
    return
  }

  if (scope.value === 'role') {
    await saveRolePermissions()
    return
  }

  await saveUserPermissions()
}

async function saveRolePermissions() {
  if (!editingMenu.value) {
    return
  }

  const nextMenu = { ...editingMenu.value }
  const nextRoles = splitRoles(nextMenu.roles)
  const nextConfig = parsePermissionConfig(nextMenu.permissionConfig)
  const nextActions = normalizeActions(editingActions.value, nextMenu)
  const currentRole = activeRole.value

  if (nextActions.includes('view')) {
    if (!nextRoles.includes(currentRole)) {
      nextRoles.push(currentRole)
    }
    nextConfig[currentRole] = nextActions
  } else {
    delete nextConfig[currentRole]
    removeRole(nextRoles, currentRole)
  }

  nextMenu.roles = nextRoles.join(',')
  nextMenu.permissionConfig = stringifyRolePermissionConfig(nextConfig, nextRoles)

  const res = await request.post('/menu', buildMenuPayload(nextMenu))
  if (res.code !== 200) {
    message.error(res.msg || '保存权限失败')
    return
  }

  message.success('角色模板已更新')
  modalVisible.value = false
  await refreshAll()
}

async function saveUserPermissions() {
  if (!editingMenu.value || !activeUser.value) {
    return
  }

  const permissionKey = getPermissionKey(editingMenu.value)
  const nextConfig = parsePermissionConfig(activeUser.value.permissionConfig)

  if (editingMode.value === 'inherit') {
    delete nextConfig[permissionKey]
  } else if (editingMode.value === 'deny') {
    nextConfig[permissionKey] = []
  } else {
    nextConfig[permissionKey] = normalizeActions(editingActions.value, editingMenu.value)
  }

  const res = await request.put(`/user/${activeUser.value.id}/permission-config`, {
    permissionConfig: stringifyUserPermissionConfig(nextConfig),
  })
  if (res.code !== 200) {
    message.error(res.msg || '保存账号权限失败')
    return
  }

  message.success('账号例外权限已更新')
  modalVisible.value = false
  await refreshAll()
}

function toggleEditingAction(action: PermissionAction) {
  if (!editingMenu.value) {
    return
  }

  const exists = editingActions.value.includes(action)
  const nextActions = exists
    ? editingActions.value.filter((item) => item !== action)
    : [...editingActions.value, action]

  editingActions.value = normalizeActions(nextActions, editingMenu.value)
}

function getCurrentActions(menu: MenuRecord) {
  if (scope.value === 'role') {
    return getRoleActions(menu, activeRole.value)
  }
  return getUserActions(menu, activeUser.value)
}

function getUserActions(menu: MenuRecord, user?: UserSubject | null) {
  if (!user) {
    return []
  }

  const permissionKey = getPermissionKey(menu)
  const config = parsePermissionConfig(user.permissionConfig)
  if (Object.prototype.hasOwnProperty.call(config, permissionKey)) {
    return normalizeActions(config[permissionKey], menu)
  }
  return getRoleActions(menu, user.role)
}

function getUserActionSource(menu: MenuRecord): PermissionSource {
  if (!activeUser.value) {
    return 'inherit'
  }

  const permissionKey = getPermissionKey(menu)
  const config = parsePermissionConfig(activeUser.value.permissionConfig)
  if (!Object.prototype.hasOwnProperty.call(config, permissionKey)) {
    return 'inherit'
  }
  return normalizeActions(config[permissionKey], menu).length > 0 ? 'override' : 'deny'
}

function getRoleActions(menu: MenuRecord, role: string): PermissionAction[] {
  if (!splitRoles(menu.roles).includes(role)) {
    return []
  }

  const config = parsePermissionConfig(menu.permissionConfig)
  const configured = normalizeActions(config[role], menu)
  if (configured.length) {
    return configured
  }
  return getAvailableActions(menu).map((item) => item.value)
}

function getAvailableActions(menu: Pick<MenuRecord, 'path' | 'component'>) {
  return isLeafMenu(menu) ? actionOptions : actionOptions.filter((item) => item.value === 'view')
}

function normalizeActions(
  actions: PermissionAction[] | undefined,
  menu: Pick<MenuRecord, 'path' | 'component'>
): PermissionAction[] {
  if (!Array.isArray(actions) || actions.length === 0) {
    return []
  }

  const supportedValues = getAvailableActions(menu).map((item) => item.value)
  const next = Array.from(new Set(actions.filter((item) => supportedValues.includes(item))))
  if (next.length > 0 && !next.includes('view')) {
    next.unshift('view')
  }
  return next
}

function getPermissionKey(menu: Pick<MenuRecord, 'id' | 'path'>) {
  if (menu.path) {
    return normalizeMenuPath(menu.path)
  }
  return `menu:${menu.id}`
}

function buildMenuTrails(source: MenuRecord[]) {
  const nodeMap = new Map<number, MenuRecord>()
  const normalized = [...source]
    .map((item) => ({ ...item, trail: [] as string[] }))
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))

  normalized.forEach((item) => {
    nodeMap.set(item.id, item)
  })

  normalized.forEach((item) => {
    const trail: string[] = []
    let current: MenuRecord | undefined = item
    while (current) {
      trail.unshift(current.title)
      current = current.parentId ? nodeMap.get(current.parentId) : undefined
    }
    item.trail = trail
  })

  return normalized
}

function parsePermissionConfig(raw?: string | null): Record<string, PermissionAction[]> {
  if (!raw) {
    return {}
  }
  try {
    const parsed = JSON.parse(raw)
    return typeof parsed === 'object' && parsed ? parsed : {}
  } catch {
    return {}
  }
}

function stringifyRolePermissionConfig(
  config: Record<string, PermissionAction[]>,
  roles: string[]
) {
  const filteredEntries = Object.entries(config).filter(([role, actions]) => {
    return roles.includes(role) && Array.isArray(actions) && actions.length > 0
  })

  if (!filteredEntries.length) {
    return null
  }

  return JSON.stringify(Object.fromEntries(filteredEntries))
}

function stringifyUserPermissionConfig(config: Record<string, PermissionAction[]>) {
  const filteredEntries = Object.entries(config).filter(([key, actions]) => {
    return Boolean(key) && Array.isArray(actions)
  })

  if (!filteredEntries.length) {
    return null
  }

  return JSON.stringify(Object.fromEntries(filteredEntries))
}

function buildMenuPayload(menu: MenuRecord) {
  return {
    id: menu.id,
    parentId: menu.parentId,
    title: menu.title,
    name: menu.name,
    path: menu.path,
    component: menu.component,
    icon: menu.icon,
    sort: menu.sort,
    roles: menu.roles,
    hidden: menu.hidden,
    componentStyle: menu.componentStyle,
    permissionConfig: menu.permissionConfig,
  }
}

function getRoleViewCount(role: string) {
  return menus.value.filter((item) => getRoleActions(item, role).includes('view')).length
}

function getUserOverrideCount(user: UserSubject) {
  return Object.keys(parsePermissionConfig(user.permissionConfig)).length
}

function splitRoles(roles?: string) {
  return roles ? roles.split(',').map((item) => item.trim()).filter(Boolean) : []
}

function removeRole(list: string[], role: string) {
  const index = list.indexOf(role)
  if (index >= 0) {
    list.splice(index, 1)
  }
}

function isLeafMenu(menu: Pick<MenuRecord, 'path' | 'component'>) {
  return Boolean(menu.path && menu.component && String(menu.component).trim().toLowerCase() !== 'layout')
}

function normalizeMenuPath(path?: string) {
  if (!path) return ''
  const normalized = String(path).trim()
  if (!normalized) return ''
  if (normalized === '/') return '/'
  return `/${normalized.replace(/^\/+/, '').replace(/\/+$/, '')}`
}

function formatTrail(trail?: string[]) {
  if (!trail || trail.length <= 1) {
    return '顶级菜单'
  }
  return trail.slice(0, -1).join(' / ')
}

function formatRoleLabel(role: string) {
  return roleOptions.find((item) => item.value === role)?.label || role
}

function formatActionLabel(action: PermissionAction) {
  return actionOptions.find((item) => item.value === action)?.label || action
}

function formatSourceLabel(source: PermissionSource) {
  if (source === 'override') {
    return '账号覆盖'
  }
  if (source === 'deny') {
    return '明确禁用'
  }
  return '继承角色'
}

function formatUserLabel(user: UserSubject) {
  return user.realName || user.nickname || user.username
}
</script>

<style scoped>
.permission-meta-tag {
  border-radius: 999px;
}

.permission-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.permission-toolbar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
}

.permission-scope-tabs,
.permission-subject-strip,
.permission-user-toolbar,
.permission-tag-row,
.permission-action-pills {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.permission-scope-tab,
.permission-subject-card,
.permission-user-card,
.permission-mode-card,
.permission-action-pill {
  appearance: none;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--mono-text);
}

.permission-scope-tab,
.permission-mode-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.permission-scope-tab {
  min-width: 160px;
}

.permission-scope-tab.is-active,
.permission-subject-card.is-active,
.permission-user-card.is-active,
.permission-mode-card.is-active,
.permission-action-pill.is-active {
  background: #111;
  color: #fff;
  border-color: #111;
}

.permission-scope-title,
.permission-subject-title,
.permission-mode-title,
.permission-user-name,
.permission-menu-title,
.permission-modal-title {
  font-weight: 600;
}

.permission-scope-desc,
.permission-subject-meta,
.permission-user-meta,
.permission-user-subline,
.permission-menu-trail,
.permission-path,
.permission-empty-text,
.permission-modal-subtitle,
.permission-mode-desc {
  color: rgba(17, 17, 17, 0.48);
  font-size: 12px;
  line-height: 1.5;
}

.permission-scope-tab.is-active .permission-scope-desc,
.permission-subject-card.is-active .permission-subject-meta,
.permission-user-card.is-active .permission-user-meta,
.permission-user-card.is-active .permission-user-subline,
.permission-mode-card.is-active .permission-mode-desc {
  color: rgba(255, 255, 255, 0.72);
}

.permission-subject-card {
  min-width: 132px;
  text-align: left;
}

.permission-user-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.permission-user-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
  min-height: 88px;
}

.permission-menu-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.permission-role-tag,
.permission-action-tag,
.permission-type-tag,
.permission-source-tag {
  border-radius: 6px;
}

.permission-role-tag.is-active {
  border-color: rgba(17, 17, 17, 0.28);
  color: var(--mono-text);
}

.permission-source-tag.is-override {
  border-color: rgba(17, 17, 17, 0.18);
  color: #111;
}

.permission-source-tag.is-deny {
  border-color: rgba(17, 17, 17, 0.18);
  color: rgba(17, 17, 17, 0.72);
}

.permission-modal {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 8px;
}

.permission-modal-summary,
.permission-preview-block,
.permission-action-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.permission-modal-alert {
  border-radius: 8px;
}

.permission-mode-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 12px;
}

.permission-preview-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--mono-text);
}

.permission-action-pill {
  padding: 9px 14px;
  min-width: 88px;
}
</style>
