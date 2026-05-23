// ============================================================================
//  authService —— 业务封装层，区别于 api 纯 HTTP 层。
//  组件不直接调 fetch，也不直接读 localStorage，所有「登录态相关」逻辑都走这里。
// ============================================================================

import { loginUser, registerUser } from "../api/bookstoreApi.js";
import { clearUser, getCurrentUser, setCurrentUser } from "../utils/auth.js";

export async function login(credentials) {
  const user = await loginUser(credentials);
  setCurrentUser(user);
  return user;
}

export async function register(form) {
  const user = await registerUser(form);
  return user;
}

export function logout() {
  clearUser();
}

export function currentUser() {
  return getCurrentUser();
}
