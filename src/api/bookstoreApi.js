// ============================================================================
//  Bookstore HTTP 层
//  - 所有出去的 HTTP 调用统一走 request()，统一加 JSON header、统一解包 ApiResponse
//  - 后端响应体形如 { code, message, data }；code !== 0 视为业务错误，throw 出去
// ============================================================================

const API_BASE_URL = "/api/v1";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  });

  // 204 No Content
  if (response.status === 204) {
    return null;
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    // 后端返回空 body
  }

  if (!response.ok) {
    const msg = (payload && payload.message) || `请求失败：${response.status}`;
    throw new Error(msg);
  }

  // 统一 ApiResponse 解包：{ code, message, data }
  if (payload && Object.prototype.hasOwnProperty.call(payload, "code")) {
    if (payload.code !== 0) {
      throw new Error(payload.message || "服务端返回了业务错误");
    }
    return payload.data;
  }
  // 兼容老接口
  return payload;
}

// ------------------------------ 书籍 ------------------------------
export function fetchBooks() {
  return request("/books");
}

export function fetchBookById(id) {
  return request(`/book/${id}`);
}

// ------------------------------ 用户 ------------------------------
export function registerUser(user) {
  return request("/users/register", {
    method: "POST",
    body: JSON.stringify(user),
  });
}

export function loginUser({ username, password }) {
  return request("/users/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

// ------------------------------ 购物车 ------------------------------
export function getCart(userId) {
  return request(`/cart?userId=${encodeURIComponent(userId)}`);
}

export function addToCart(userId, bookId, quantity = 1) {
  return request("/cart/items", {
    method: "POST",
    body: JSON.stringify({ userId, bookId, quantity }),
  });
}

export function updateCartItem(itemId, quantity) {
  return request(`/cart/items/${itemId}`, {
    method: "PUT",
    body: JSON.stringify({ quantity }),
  });
}

export function removeCartItem(itemId) {
  return request(`/cart/items/${itemId}`, { method: "DELETE" });
}

export function clearCart(userId) {
  return request(`/cart?userId=${encodeURIComponent(userId)}`, {
    method: "DELETE",
  });
}

// ------------------------------ 订单 ------------------------------
export function placeOrder(userId) {
  return request(`/orders?userId=${encodeURIComponent(userId)}`, {
    method: "POST",
  });
}

export function getOrders(userId) {
  return request(`/orders?userId=${encodeURIComponent(userId)}`);
}

export function getOrderById(orderId) {
  return request(`/orders/${orderId}`);
}
