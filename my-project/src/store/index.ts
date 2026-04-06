import { createStore } from 'vuex'
import request from '@/request'
import themeSettings from './themeSettings'

export default createStore({
  state: {
    // 侧边栏菜单树 (树形结构)
    menuTree: [],
    // 路由是否已加载
    routesLoaded: false,
    // 当前用户的角色
    userRole: ''
  },
  getters: {
    getMenuTree: (state) => state.menuTree,
    areRoutesLoaded: (state) => state.routesLoaded,
  },
  mutations: {
    SET_MENU_TREE(state, menuTree) {
      state.menuTree = menuTree;
    },
    SET_ROUTES_LOADED(state, loaded) {
      state.routesLoaded = loaded;
    },
    SET_USER_ROLE(state, role) {
      state.userRole = role;
    }
  },
  actions: {
    // 异步加载菜单数据
    async fetchUserMenus({ commit }) {
      // 从本地存储获取用户信息中的角色
      const userStr = localStorage.getItem('user');
      if (!userStr) return [];

      const user = JSON.parse(userStr);
      const role = user.role || 'MEMBER';
      commit('SET_USER_ROLE', role);

      try {
        // 调用后端接口获取当前角色可用的菜单列表
        const res: any = await request.get('/menu/user-menus', { params: { role } });
        if (res.code === 200) {
          const menuList = res.data;
          // 构建树形结构供侧边栏使用
          const tree = buildTree(menuList);
          commit('SET_MENU_TREE', tree);
          return menuList; // 返回扁平列表供路由处理
        }
      } catch (error) {
        console.error("Failed to fetch menus", error);
      }
      return [];
    }
  },
  modules: {
    themeSettings
  }
})

// 辅助函数：列表转树
function buildTree(list: any[]) {
  const temp: any = {};
  const tree: any[] = [];
  // 深拷贝一份 list，避免修改原数据，同时建立 ID索引
  list.forEach(item => {
    temp[item.id] = { ...item, children: [] };
  });

  list.forEach(item => {
    // 如果有父级且父级在列表中存在，则挂载到父级下
    if (item.parentId && temp[item.parentId]) {
      temp[item.parentId].children.push(temp[item.id]);
    } else {
      // 否则视为顶级节点
      tree.push(temp[item.id]);
    }
  });

  // 递归清理空的 children 数组 (可选，Antd Menu如果有空children可能会有显隐问题)
  const cleanChildren = (nodes: any[]) => {
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
}
