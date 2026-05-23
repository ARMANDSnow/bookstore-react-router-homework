import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { useCallback, useEffect, useMemo, useState } from "react";
import { message } from "antd";

import booksData from "./data/Data.json";
import Layout from "./components/Layout.jsx";
import BookListPage from "./pages/BookListPage.jsx";
import BookDetailPage from "./pages/BookDetailPage.jsx";
import CartPage from "./pages/CartPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";

import { fetchBooks } from "./api/bookstoreApi.js";
import {
  addBookToCart,
  changeQuantity,
  checkout,
  listCart,
  removeItem,
} from "./services/cartService.js";
import { currentUser, logout } from "./services/authService.js";

export default function App() {
  // -------------------------- books --------------------------
  const [books, setBooks] = useState([]);
  const [booksLoading, setBooksLoading] = useState(true);
  const [selectedBook, setSelectedBook] = useState(null);

  // -------------------------- user ---------------------------
  const [user, setUser] = useState(() => currentUser());

  // -------------------------- cart ---------------------------
  const [cart, setCart] = useState([]);
  const [cartLoading, setCartLoading] = useState(false);

  const navigate = useNavigate();

  // 监听登录态变化（authService 用 CustomEvent 通知）
  useEffect(() => {
    const handler = () => setUser(currentUser());
    window.addEventListener("bookstore:user-change", handler);
    return () => window.removeEventListener("bookstore:user-change", handler);
  }, []);

  // 拉书籍列表
  useEffect(() => {
    let ignore = false;
    (async () => {
      try {
        const remote = await fetchBooks();
        if (!ignore) setBooks(remote);
      } catch (err) {
        if (!ignore) {
          setBooks(booksData);
          message.warning("后端服务暂不可用，已使用本地书籍数据展示。");
        }
      } finally {
        if (!ignore) setBooksLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, []);

  // 登录态变化时拉购物车
  const refreshCart = useCallback(async () => {
    if (!user) {
      setCart([]);
      return;
    }
    try {
      setCartLoading(true);
      const data = await listCart(user.id);
      setCart(data);
    } catch (err) {
      message.error(err.message || "获取购物车失败");
    } finally {
      setCartLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  const cartCount = useMemo(
    () => cart.reduce((sum, item) => sum + (item.quantity || 0), 0),
    [cart]
  );

  // -------------------------- 交互回调 --------------------------
  function openBookDetail(book) {
    setSelectedBook(book);
    navigate(`/books/${book.id}`, { state: { book } });
  }

  async function handleAddToCart(book) {
    if (!user) {
      message.warning("请先登录后再加入购物车");
      navigate("/profile");
      return;
    }
    try {
      await addBookToCart(user.id, book.id, 1);
      await refreshCart();
      message.success(`${book.title} 已加入购物车`);
    } catch (err) {
      message.error(err.message || "加入购物车失败");
    }
  }

  async function handleUpdateQuantity(itemId, quantity) {
    if (quantity < 1) return;
    try {
      await changeQuantity(itemId, quantity);
      await refreshCart();
    } catch (err) {
      message.error(err.message || "更新数量失败");
    }
  }

  async function handleRemoveItem(itemId) {
    try {
      await removeItem(itemId);
      await refreshCart();
      message.info("商品已从购物车中移除");
    } catch (err) {
      message.error(err.message || "移除失败");
    }
  }

  async function handleSubmitOrder() {
    if (!user) {
      message.warning("请先登录");
      return false;
    }
    if (!cart.length) return false;
    try {
      await checkout(user.id);
      await refreshCart();
      message.success("结算成功！已生成订单，可在「个人信息」查看。");
      navigate("/profile");
      return true;
    } catch (err) {
      message.error(err.message || "下单失败");
      return false;
    }
  }

  function handleLogout() {
    logout();
    setCart([]);
    message.success("已退出登录");
    navigate("/profile");
  }

  return (
    <Layout cartCount={cartCount} user={user} onLogout={handleLogout}>
      <Routes>
        <Route path="/" element={<Navigate to="/books" replace />} />
        <Route
          path="/books"
          element={
            <BookListPage
              books={books}
              loading={booksLoading}
              onBookSelect={openBookDetail}
              onAddToCart={handleAddToCart}
            />
          }
        />
        <Route
          path="/books/:bookId"
          element={
            <BookDetailPage
              books={books}
              loading={booksLoading}
              selectedBook={selectedBook}
              onBookSelect={openBookDetail}
              onAddToCart={handleAddToCart}
            />
          }
        />
        <Route
          path="/cart"
          element={
            <CartPage
              cart={cart}
              loading={cartLoading}
              user={user}
              onUpdateQuantity={handleUpdateQuantity}
              onRemove={handleRemoveItem}
              onSubmitOrder={handleSubmitOrder}
            />
          }
        />
        <Route
          path="/profile"
          element={<ProfilePage user={user} onLogout={handleLogout} />}
        />
      </Routes>
    </Layout>
  );
}
