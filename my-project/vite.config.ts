import type {IncomingMessage, ServerResponse} from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath, URL} from 'node:url';
import {createProxyServer} from 'http-proxy';
import {defineConfig, type ViteDevServer} from 'vite';
import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import {AntDesignVueResolver} from 'unplugin-vue-components/resolvers';
import {formConfigManifestPlugin} from './plugins/formConfigManifestPlugin';

const SPA_COMPATIBLE_API_ROOTS = new Set([
    'coach',
    'course',
    'equipment',
    'reservation',
    'user',
    'venue',
]);

const API_PROXY_ROOTS = new Set([
    'api',
    'admin',
    'coach',
    'course',
    'dict',
    'equipment',
    'form-config',
    'member',
    'menu',
    'reservation',
    'user',
    'venue',
]);

// Ant Design Vue components are injected by the resolver plugin after Vite's
// initial scan, so we pre-bundle the heavy dependency chain up front to avoid
// repeated optimizer invalidation on cold starts.
const PREBUNDLED_DEPS = [
    'ant-design-vue',
    'ant-design-vue/es/locale/zh_CN',
    'ant-design-vue/es/auto-complete',
    'ant-design-vue/es/avatar',
    'ant-design-vue/es/button',
    'ant-design-vue/es/checkbox',
    'ant-design-vue/es/config-provider',
    'ant-design-vue/es/date-picker',
    'ant-design-vue/es/drawer',
    'ant-design-vue/es/dropdown',
    'ant-design-vue/es/form',
    'ant-design-vue/es/grid',
    'ant-design-vue/es/input',
    'ant-design-vue/es/input-number',
    'ant-design-vue/es/layout',
    'ant-design-vue/es/menu',
    'ant-design-vue/es/modal',
    'ant-design-vue/es/pagination',
    'ant-design-vue/es/popconfirm',
    'ant-design-vue/es/radio',
    'ant-design-vue/es/segmented',
    'ant-design-vue/es/select',
    'ant-design-vue/es/space',
    'ant-design-vue/es/spin',
    'ant-design-vue/es/switch',
    'ant-design-vue/es/table',
    'ant-design-vue/es/tabs',
    'ant-design-vue/es/tag',
    'ant-design-vue/es/time-picker',
    'ant-design-vue/es/tooltip',
    'ant-design-vue/es/tree-select',
    'ant-design-vue/es/typography',
    '@ant-design/icons-vue',
    '@ctrl/tinycolor',
    'async-validator',
    'dayjs',
    'dom-align',
    'lodash-es',
    'resize-observer-polyfill',
    'scroll-into-view-if-needed',
    'throttle-debounce',
    'vue-types',
];

const INDEX_HTML_PATH = fileURLToPath(new URL('./index.html', import.meta.url));
const backendProxy = createProxyServer({
    target: 'http://localhost:9090',
    changeOrigin: true,
});

backendProxy.on('error', (error, _req, res) => {
    const response = res as ServerResponse;
    if (response.headersSent) {
        response.end();
        return;
    }

    response.statusCode = 502;
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.end(JSON.stringify({
        message: 'Backend proxy error',
        detail: error.message,
    }));
});

const isHtmlNavigation = (req: IncomingMessage) => {
    const accept = req.headers.accept || '';
    return accept.includes('text/html') || accept.includes('application/xhtml+xml');
};

const getApiRoot = (pathname: string) => {
    const match = pathname.match(/^\/([^/]+)/);
    if (!match) {
        return null;
    }

    return match[1];
};

const shouldProxyApiRequest = (pathname: string, req: IncomingMessage) => {
    const root = getApiRoot(pathname);
    if (!root || !API_PROXY_ROOTS.has(root)) {
        return false;
    }

    if (
        SPA_COMPATIBLE_API_ROOTS.has(root) &&
        pathname === `/${root}` &&
        req.method === 'GET' &&
        isHtmlNavigation(req)
    ) {
        return false;
    }

    return true;
};

const spaCompatibleRoutePlugin = () => ({
    name: 'spa-compatible-route-proxy',
    apply: 'serve' as const,
    configureServer(server: ViteDevServer) {
        server.middlewares.use(async (
            req: IncomingMessage & { url?: string; originalUrl?: string },
            res: ServerResponse,
            next: (error?: Error) => void
        ) => {
            if (!req.url) {
                next();
                return;
            }

            const url = new URL(req.url, 'http://localhost');
            const root = getApiRoot(url.pathname);

            if (!root || !API_PROXY_ROOTS.has(root) || shouldProxyApiRequest(url.pathname, req)) {
                if (root && API_PROXY_ROOTS.has(root)) {
                    backendProxy.web(req, res);
                    return;
                }

                next();
                return;
            }

            try {
                const template = await readFile(INDEX_HTML_PATH, 'utf-8');
                const html = await server.transformIndexHtml('/', template);
                res.statusCode = 200;
                res.setHeader('Content-Type', 'text/html');
                res.end(html);
            } catch (error) {
                next(error instanceof Error ? error : new Error(String(error)));
            }
        });
    },
});

const isAntDependencyModule = (id: string) => (
    id.includes('/ant-design-vue/')
    || id.includes('/@ant-design/')
    || id.includes('/@babel/runtime/')
    || id.includes('/@ctrl/tinycolor/')
    || id.includes('/@emotion/')
    || id.includes('/@simonwep/pickr/')
    || id.includes('/array-tree-filter/')
    || id.includes('/async-validator/')
    || id.includes('/dayjs/')
    || id.includes('/dom-align/')
    || id.includes('/dom-scroll-into-view/')
    || id.includes('/lodash-es/')
    || id.includes('/resize-observer-polyfill/')
    || id.includes('/scroll-into-view-if-needed/')
    || id.includes('/shallow-equal/')
    || id.includes('/stylis/')
    || id.includes('/throttle-debounce/')
    || id.includes('/vue-types/')
    || id.includes('/warning/')
);

const createManualChunks = (id: string) => {
    if (!id.includes('/node_modules/')) {
        return undefined;
    }

    if (id.includes('/echarts/') || id.includes('/zrender/') || id.includes('/@unovis/')) {
        return 'vendor-charts';
    }

    if (isAntDependencyModule(id)) {
        return undefined;
    }

    if (
        id.includes('/reka-ui/') ||
        id.includes('/lucide-vue-next/') ||
        id.includes('/embla-carousel-vue/') ||
        id.includes('/vue-sonner/') ||
        id.includes('/@vueuse/')
    ) {
        return 'vendor-ui';
    }

    if (
        id.includes('/vue/') ||
        id.includes('/vue-router/') ||
        id.includes('/vuex/')
    ) {
        return 'vendor-vue';
    }

    return 'vendor-misc';
};

export default defineConfig({
    plugins: [
        vue(),
        Components({
            dts: false,
            resolvers: [
                AntDesignVueResolver({
                    importStyle: false,
                }),
            ],
        }),
        formConfigManifestPlugin(),
        spaCompatibleRoutePlugin(),
    ],
    envPrefix: ['VITE_', 'VUE_APP_'],
    optimizeDeps: {
        force: true,
        holdUntilCrawlEnd: true,
        include: PREBUNDLED_DEPS,
    },
    build: {
        cssCodeSplit: false,
        rollupOptions: {
            output: {
                manualChunks: createManualChunks,
            },
        },
    },
    server: {
        host: '0.0.0.0',
        port: 8080,
        strictPort: true,
        headers: {
            'Cache-Control': 'no-store',
        },
    },
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
});
