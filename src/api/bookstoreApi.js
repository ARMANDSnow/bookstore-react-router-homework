// ============================================================================
//  Bookstore HTTP 层
//  - 所有出去的 HTTP 调用统一走 request()，统一加 JSON header、统一解包 ApiResponse
//  - 后端响应体形如 { code, message, data }；code !== 0 视为业务错误，throw 出去
// ============================================================================

const API_BASE_URL = "/api/v1";

async function request(path, options = {}, baseUrl = API_BASE_URL) {
  const response = await fetch(`${baseUrl}${path}`, {
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
    const error = new Error(msg);
    error.status = response.status;
    error.code = payload?.code;
    throw error;
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

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, value);
    }
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

// ------------------------------ 书籍 ------------------------------
export function fetchBooks() {
  return request("/books");
}

export function fetchCatalog(params, signal) {
  return request(`/books${buildQuery(params)}`, { signal }, "/api");
}

export function fetchBookById(id) {
  return request(`/books/${encodeURIComponent(id)}`, {}, "/api");
}

// 迭代三新增：关键字搜索（后端 GET /api/v1/books?keyword=xxx，SQL LIKE 模糊匹配标题/作者）
// encodeURIComponent：把中文、空格、& 等字符转义进 URL，防止 query string 被截断
export function searchBooks(keyword) {
  return request(`/books?keyword=${encodeURIComponent(keyword)}`);
}

export function createBook(book) {
  return request("/books", {
    method: "POST",
    body: JSON.stringify(book),
  }, "/api");
}

export function updateBook(id, book) {
  return request(`/book/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(book),
  });
}

export function deleteBook(id) {
  return request(`/book/${encodeURIComponent(id)}`, { method: "DELETE" });
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

export function listUsers() {
  return request("/users");
}

export function setUserEnabled(userId, enabled) {
  return request(`/users/${encodeURIComponent(userId)}/enabled?enabled=${enabled}`, {
    method: "PUT",
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

export function getOrders(params) {
  if (typeof params === "number" || typeof params === "string") {
    return request(`/orders?userId=${encodeURIComponent(params)}`);
  }
  return request(`/orders${buildQuery(params)}`);
}

export function getOrderById(orderId) {
  return request(`/orders/${orderId}`);
}

export function getSalesRank(params) {
  return request(`/orders/sales-rank${buildQuery(params)}`);
}

export function getUserSpendRank(params) {
  return request(`/orders/user-spending-rank${buildQuery(params)}`);
}

export function getCustomerStats(params) {
  return request(`/orders/customer-stats${buildQuery(params)}`);
}
