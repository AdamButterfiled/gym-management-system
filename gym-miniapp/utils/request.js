const DEFAULT_BASE_URL = 'http://localhost:9090'
const BASE_URL_STORAGE_KEY = 'gym_api_base_url'

function normalizeBaseUrl(baseUrl) {
  return typeof baseUrl === 'string' ? baseUrl.trim().replace(/\/+$/, '') : ''
}

function getRuntimeBaseUrl() {
  const globalScope = typeof globalThis !== 'undefined' ? globalThis : {}
  const appConfig = globalScope.GYM_MINIAPP_CONFIG
  let appBaseUrl = ''
  try {
    const appGlobalData = typeof getApp === 'function' ? getApp()?.globalData : null
    appBaseUrl = appGlobalData?.BASE_URL || appGlobalData?.baseUrl
  } catch (error) {
    appBaseUrl = ''
  }
  const globalBaseUrl = appConfig?.BASE_URL || appConfig?.baseUrl || appBaseUrl || globalScope.__GYM_API_BASE_URL__

  let storedBaseUrl = ''
  try {
    storedBaseUrl = uni.getStorageSync(BASE_URL_STORAGE_KEY)
  } catch (error) {
    storedBaseUrl = ''
  }

  return normalizeBaseUrl(globalBaseUrl || storedBaseUrl) || DEFAULT_BASE_URL
}

function buildRequestUrl(url, params) {
  const requestUrl = /^https?:\/\//.test(url) ? url : `${getRuntimeBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`
  return `${requestUrl}${stringifyParams(params)}`
}

export function getUser() {
  return uni.getStorageSync('gym_user') || null
}

export function setUser(user) {
  uni.setStorageSync('gym_user', user)
}

export function clearUser() {
  uni.removeStorageSync('gym_user')
}

function stringifyParams(params = {}) {
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  if (!entries.length) return ''
  return `?${entries.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')}`
}

export function request({ url, method = 'GET', data, params }) {
  const user = getUser()
  return new Promise((resolve, reject) => {
    uni.request({
      url: buildRequestUrl(url, params),
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        Authorization: user?.token ? `Bearer ${user.token}` : ''
      },
      success: ({ data: res }) => {
        if (res && res.code === 200) {
          resolve(res)
          return
        }
        uni.showToast({ title: res?.msg || '请求失败', icon: 'none' })
        reject(res)
      },
      fail: (error) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(error)
      }
    })
  })
}

export function relaunchByRole(role) {
  if (role === 'COACH') {
    uni.reLaunch({ url: '/pages/coach/home/index' })
    return
  }
  uni.reLaunch({ url: '/pages/member/home/index' })
}
