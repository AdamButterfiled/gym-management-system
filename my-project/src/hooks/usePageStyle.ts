import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { normalizeComponentStyle, resolveMenuComponentStyle, type MenuStyleNode, type PageComponentStyle } from '@/utils/menuStyle';

export function usePageStyle() {
  const route = useRoute();
  const store = useStore();

  const resolveCurrentStyle = (): PageComponentStyle => {
    const flatMenus = (store.getters.getFlatMenus || []) as MenuStyleNode[];
    const treeMenus = (store.getters.getMenuTree || []) as MenuStyleNode[];

    return resolveMenuComponentStyle(flatMenus, route.path)
      || resolveMenuComponentStyle(treeMenus, route.path)
      || normalizeComponentStyle(route.meta.style as string | undefined)
      || 'default';
  };

  const currentStyle = ref<PageComponentStyle>(resolveCurrentStyle());
  const applyCurrentStyle = () => {
    currentStyle.value = resolveCurrentStyle();
  };

  watch(
    () => [route.path, store.getters.getFlatMenus, store.getters.getMenuTree, route.meta.style],
    applyCurrentStyle,
    { immediate: true },
  );

  const loadMenuConfig = async () => {
    applyCurrentStyle();
    await store.dispatch('fetchUserMenus');
    applyCurrentStyle();
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
