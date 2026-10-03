const STORAGE_KEY = "react-bookstore-user";

// 登录态工具层（Util）：
// 只负责 localStorage 的读写和"用户状态变化"事件通知，不直接调用后端接口。
// 这样组件不用散落 localStorage.getItem/setItem，答辩时可说明这是前端分层里的 Util。
export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    // localStorage 里如果被手工写坏了 JSON，不能让整个 React 应用崩溃，直接当作未登录处理。
    return null;
  }
}

export function setCurrentUser(user) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  // App.jsx 监听这个自定义事件后会刷新 user state，从而让 Header/购物车等界面同步更新。
  window.dispatchEvent(new CustomEvent("bookstore:user-change"));
}

export function clearUser() {
  localStorage.removeItem(STORAGE_KEY);
  // 退出登录同样发事件，避免多个组件各自维护一份不一致的登录状态。
  window.dispatchEvent(new CustomEvent("bookstore:user-change"));
}
