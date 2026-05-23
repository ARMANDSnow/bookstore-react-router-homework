// ============================================================================
//  cartService —— 在 HTTP 层之上做一层薄薄的业务封装，
//  目前只是直接转发到 api 层；保留这个文件是为了之后扩展（缓存、去抖、
//  本地与远程合并等）时不用改组件代码。
// ============================================================================

import {
  addToCart as apiAddToCart,
  clearCart as apiClearCart,
  getCart as apiGetCart,
  placeOrder as apiPlaceOrder,
  removeCartItem as apiRemoveItem,
  updateCartItem as apiUpdateItem,
} from "../api/bookstoreApi.js";

export function listCart(userId) {
  return apiGetCart(userId);
}

export function addBookToCart(userId, bookId, quantity = 1) {
  return apiAddToCart(userId, bookId, quantity);
}

export function changeQuantity(itemId, quantity) {
  return apiUpdateItem(itemId, quantity);
}

export function removeItem(itemId) {
  return apiRemoveItem(itemId);
}

export function clearAll(userId) {
  return apiClearCart(userId);
}

export function checkout(userId) {
  return apiPlaceOrder(userId);
}
