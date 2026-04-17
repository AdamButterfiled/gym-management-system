declare global {
  interface Window {
    __GMS_BOOT__?: {
      ready?: () => void;
      update?: (message: string) => void;
      fail?: (title?: string, detail?: unknown, description?: string) => void;
    };
  }
}

let appBootReady = false;

const getBootBridge = () => (typeof window !== 'undefined' ? window.__GMS_BOOT__ : undefined);

export const markAppBootReady = () => {
  appBootReady = true;
  getBootBridge()?.ready?.();
};

export const updateAppBootMessage = (message: string) => {
  getBootBridge()?.update?.(message);
};

export const reportAppBootFailure = (
  error: unknown,
  title = '页面启动失败',
  description = '页面初始化过程中发生异常，请重试。'
) => {
  console.error('[app-boot]', error);

  if (!appBootReady) {
    getBootBridge()?.fail?.(title, error, description);
  }
};
