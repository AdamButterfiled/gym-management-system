const { defineConfig } = require('@vue/cli-service')

const SPA_COMPATIBLE_API_ROOTS = new Set([
  'coach',
  'course',
  'equipment',
  'reservation',
  'user',
  'venue'
])

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
  'venue'
])

const isHtmlNavigation = (req) => {
  const accept = req.headers.accept || ''
  return accept.includes('text/html') || accept.includes('application/xhtml+xml')
}

const shouldProxyApiRequest = (pathname, req) => {
  const match = pathname.match(/^\/([^/]+)/)
  if (!match) {
    return false
  }

  const root = match[1]
  if (!API_PROXY_ROOTS.has(root)) {
    return false
  }

  if (SPA_COMPATIBLE_API_ROOTS.has(root)
    && pathname === `/${root}`
    && req.method === 'GET'
    && isHtmlNavigation(req)) {
    return false
  }

  return true
}

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    historyApiFallback: {
      disableDotRule: true,
      rewrites: [
        { from: /./, to: '/index.html' }
      ]
    },
    proxy: {
      '^/(api|admin|coach|course|dict|equipment|form-config|member|menu|reservation|user|venue)(/.*)?$': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        bypass: (req) => {
          if (!shouldProxyApiRequest(req.url, req)) {
            return '/index.html'
          }
          return undefined
        }
      }
    }
  }
})
