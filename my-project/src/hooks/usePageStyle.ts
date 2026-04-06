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

    /**
     * 从菜单数据中计算当前页面的有效样式
     * 支持两种数据结构：
     *   1. 扁平数组 (带 parentId) — /menu/list 返回的格式
     *   2. 嵌套树结构 (带 children) — /menu/tree 返回的格式
     */
    const checkCurrentPageStyle = (menus: Menu[]) => {
        // 判断数据是扁平结构还是树结构
        const isFlat = menus.length > 0 && !menus[0].children;

        if (isFlat) {
            // 扁平数组：通过 parentId 向上查找
            const currentMenu = menus.find(m => m.path === route.path);
            if (!currentMenu) {
                currentStyle.value = 'default';
                return;
            }

            // 如果当前菜单自身有配置，直接使用
            if (currentMenu.componentStyle) {
                currentStyle.value = currentMenu.componentStyle === 'glass' ? 'glass' : 'default';
                return;
            }

            // 否则向上查找父菜单的样式配置
            let parentId = currentMenu.parentId;
            while (parentId) {
                const parent = menus.find(m => m.id === parentId);
                if (parent) {
                    if (parent.componentStyle) {
                        currentStyle.value = parent.componentStyle === 'glass' ? 'glass' : 'default';
                        return;
                    }
                    parentId = parent.parentId;
                } else {
                    break;
                }
            }
            currentStyle.value = 'default';
        } else {
            // 树结构：递归遍历
            const findStyle = (nodes: Menu[], parentStyle: string | null = null): string | null => {
                for (const node of nodes) {
                    const effectiveStyle = node.componentStyle || parentStyle;
                    if (node.path === route.path) {
                        return effectiveStyle;
                    }
                    if (node.children) {
                        const childResult = findStyle(node.children, effectiveStyle);
                        if (childResult) return childResult;
                    }
                }
                return null;
            };
            const style = findStyle(menus);
            currentStyle.value = (style === 'glass') ? 'glass' : 'default';
        }
    };

    const loadMenuConfig = () => {
        request.get("/menu/list").then((res: any) => {
            if (res.code === 200) {
                checkCurrentPageStyle(res.data);
            }
        });
    };

    const pageStyleClass = computed(() => {
        return {
            'transparent-glass-mode': currentStyle.value === 'glass',
            'default-yellow-mode': currentStyle.value !== 'glass'
        };
    });

    return {
        currentStyle,
        pageStyleClass,
        loadMenuConfig
    };
}
