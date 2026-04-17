/**
 * Theme Settings Vuex Module
 * Manages global theme configuration persisted in localStorage.
 */

export type StyleMode = 'glass' | 'traditional';
export type DarkMode = 'light' | 'dark' | 'auto';

export interface ThemeSettingsState {
    /** 风格模式: 'glass' = 透明极简白, 'traditional' = 经典页壳风格 */
    styleMode: StyleMode;
    /** 主题色 */
    themeColor: string;
    /** 是否启用标题/工作台数字艺术字 */
    artisticTitles: boolean;
    /** 是否启用全局艺术字（登录页除外） */
    globalArtFont: boolean;
    /** 是否启用圆角 */
    borderRadius: boolean;
    /** 是否启用彩色标签 */
    colorfulTags: boolean;
    /** 是否启用记账本式内页卡片页壳 */
    ledgerShell: boolean;
    /** 暗黑模式: 'light' = 白天, 'dark' = 黑夜, 'auto' = 跟随系统 */
    darkMode: DarkMode;
    /** 实际暗黑状态 (resolve auto to actual light/dark) */
    isDark: boolean;
}

const STORAGE_KEY = 'theme-settings';
const DEFAULT_THEME_COLOR = '#111111';
const LEGACY_THEME_COLOR = '#f7b500';

function loadFromStorage(): Partial<ThemeSettingsState> {
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) return JSON.parse(saved);
    } catch (e) {
        // ignore parse errors
    }
    return {};
}

function saveToStorage(state: ThemeSettingsState) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
        styleMode: state.styleMode,
        themeColor: state.themeColor,
        artisticTitles: state.artisticTitles,
        globalArtFont: state.globalArtFont,
        borderRadius: state.borderRadius,
        colorfulTags: state.colorfulTags,
        ledgerShell: state.ledgerShell,
        darkMode: state.darkMode,
    }));
}

const saved = loadFromStorage();
const resolvedThemeColor = !saved.themeColor || saved.themeColor === LEGACY_THEME_COLOR
    ? DEFAULT_THEME_COLOR
    : saved.themeColor;

// Detect system preference
function getSystemDarkPreference(): boolean {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function resolveIsDark(mode: DarkMode): boolean {
    if (mode === 'dark') return true;
    if (mode === 'light') return false;
    return getSystemDarkPreference();
}

const themeSettingsModule = {
    namespaced: true,
    state: (): ThemeSettingsState => ({
        styleMode: (saved.styleMode as StyleMode) || 'glass',
        themeColor: resolvedThemeColor,
        artisticTitles: saved.artisticTitles !== undefined ? Boolean(saved.artisticTitles) : true,
        globalArtFont: saved.globalArtFont !== undefined ? Boolean(saved.globalArtFont) : false,
        borderRadius: saved.borderRadius !== undefined ? saved.borderRadius : true,
        colorfulTags: saved.colorfulTags !== undefined ? Boolean(saved.colorfulTags) : false,
        ledgerShell: saved.ledgerShell !== undefined ? Boolean(saved.ledgerShell) : false,
        darkMode: (saved.darkMode as DarkMode) || 'light',
        isDark: resolveIsDark((saved.darkMode as DarkMode) || 'light'),
    }),
    getters: {
        styleMode: (state: ThemeSettingsState) => state.styleMode,
        themeColor: (state: ThemeSettingsState) => state.themeColor,
        artisticTitles: (state: ThemeSettingsState) => state.artisticTitles,
        globalArtFont: (state: ThemeSettingsState) => state.globalArtFont,
        borderRadius: (state: ThemeSettingsState) => state.borderRadius,
        colorfulTags: (state: ThemeSettingsState) => state.colorfulTags,
        ledgerShell: (state: ThemeSettingsState) => state.ledgerShell,
        darkMode: (state: ThemeSettingsState) => state.darkMode,
        isDark: (state: ThemeSettingsState) => state.isDark,
        isGlass: (state: ThemeSettingsState) => state.styleMode === 'glass',
        isTraditional: (state: ThemeSettingsState) => state.styleMode === 'traditional',
    },
    mutations: {
        SET_STYLE_MODE(state: ThemeSettingsState, mode: StyleMode) {
            state.styleMode = mode;
            saveToStorage(state);
        },
        SET_THEME_COLOR(state: ThemeSettingsState, color: string) {
            state.themeColor = color;
            saveToStorage(state);
        },
        SET_ARTISTIC_TITLES(state: ThemeSettingsState, enabled: boolean) {
            state.artisticTitles = enabled;
            saveToStorage(state);
        },
        SET_GLOBAL_ART_FONT(state: ThemeSettingsState, enabled: boolean) {
            state.globalArtFont = enabled;
            saveToStorage(state);
        },
        SET_BORDER_RADIUS(state: ThemeSettingsState, enabled: boolean) {
            state.borderRadius = enabled;
            saveToStorage(state);
        },
        SET_COLORFUL_TAGS(state: ThemeSettingsState, enabled: boolean) {
            state.colorfulTags = enabled;
            saveToStorage(state);
        },
        SET_LEDGER_SHELL(state: ThemeSettingsState, enabled: boolean) {
            state.ledgerShell = enabled;
            saveToStorage(state);
        },
        SET_DARK_MODE(state: ThemeSettingsState, mode: DarkMode) {
            state.darkMode = mode;
            state.isDark = resolveIsDark(mode);
            saveToStorage(state);
        },
        SET_IS_DARK(state: ThemeSettingsState, isDark: boolean) {
            state.isDark = isDark;
        },
    },
    actions: {
        /** Initialize system dark mode listener */
        initSystemDarkListener({ commit, state }: any) {
            if (window.matchMedia) {
                const mql = window.matchMedia('(prefers-color-scheme: dark)');
                mql.addEventListener('change', (e: MediaQueryListEvent) => {
                    if (state.darkMode === 'auto') {
                        commit('SET_IS_DARK', e.matches);
                    }
                });
            }
        },
    }
};

export default themeSettingsModule;
