import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import request from '@/request';

interface Menu {
  id: number;
  parentId?: number | null;
  path: string;
  componentStyle?: string | null;
  children?: Menu[];
}

export function usePageStyle() {
  const route = useRoute();
  const currentStyle = ref<'default' | 'glass'>('default');

  const checkCurrentPageStyle = (menus: Menu[]) => {
    const isFlat = menus.length > 0 && !menus[0].children;

    if (isFlat) {
      const currentMenu = menus.find((item) => item.path === route.path);
      if (!currentMenu) {
        currentStyle.value = 'default';
        return;
      }

      if (currentMenu.componentStyle) {
        currentStyle.value = currentMenu.componentStyle === 'glass' ? 'glass' : 'default';
        return;
      }

      let parentId = currentMenu.parentId;
      while (parentId) {
        const parent = menus.find((item) => item.id === parentId);
        if (!parent) {
          break;
        }

        if (parent.componentStyle) {
          currentStyle.value = parent.componentStyle === 'glass' ? 'glass' : 'default';
          return;
        }

        parentId = parent.parentId;
      }

      currentStyle.value = 'default';
      return;
    }

    const findStyle = (nodes: Menu[], parentStyle: string | null = null): string | null => {
      for (const node of nodes) {
        const effectiveStyle = node.componentStyle || parentStyle;
        if (node.path === route.path) {
          return effectiveStyle;
        }
        if (node.children) {
          const childResult = findStyle(node.children, effectiveStyle);
          if (childResult) {
            return childResult;
          }
        }
      }
      return null;
    };

    const style = findStyle(menus);
    currentStyle.value = style === 'glass' ? 'glass' : 'default';
  };

  const loadMenuConfig = () => {
    request.get('/menu/list').then((res: any) => {
      if (res.code === 200) {
        checkCurrentPageStyle(res.data);
      }
    });
  };

  const pageStyleClass = computed(() => ({
    'transparent-glass-mode': currentStyle.value === 'glass',
    'default-yellow-mode': currentStyle.value !== 'glass',
  }));

  return {
    currentStyle,
    pageStyleClass,
    loadMenuConfig,
  };
}
