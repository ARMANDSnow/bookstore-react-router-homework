const STORAGE_KEY = "react-bookstore-user";

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setCurrentUser(user) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  window.dispatchEvent(new CustomEvent("bookstore:user-change"));
}

export function clearUser() {
  localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new CustomEvent("bookstore:user-change"));
}
