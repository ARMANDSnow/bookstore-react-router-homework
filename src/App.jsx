import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { message } from "antd";

import booksData from "./data/Data.json";
import Layout from "./components/Layout.jsx";
import BookListPage from "./pages/BookListPage.jsx";
import BookDetailPage from "./pages/BookDetailPage.jsx";
import CartPage from "./pages/CartPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import AdminUsersPage from "./pages/AdminUsersPage.jsx";
import AdminBooksPage from "./pages/AdminBooksPage.jsx";
import OrdersPage from "./pages/OrdersPage.jsx";
import StatsPage from "./pages/StatsPage.jsx";

import { fetchBooks } from "./api/bookstoreApi.js";
import {
  addBookToCart,
  changeQuantity,
  checkout,
  listCart,
  removeItem,
} from "./services/cartService.js";
import { currentUser, logout } from "./services/authService.js";
const AssistantPage = lazy(() => import("./pages/AssistantPage.jsx"));
const PolicyPage = lazy(() => import("./pages/PolicyPage.jsx"));
const GuidePage = lazy(() => import("./pages/GuidePage.jsx"));

export default function App() {
  // -------------------------- books --------------------------
  // App 作为顶层容器持有"跨页面共享状态"：
  // 书籍列表给列表页/详情页共用，购物车数量给 Layout 角标共用，用户信息给所有业务动作共用。
  // 子页面只通过 props 拿数据和回调，符合 React 单向数据流。
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

  const loadBooks = useCallback(async () => {
    setBooksLoading(true);
    // ignore 是 React 异步请求常见的竞态保护：
    // 组件卸载或 effect 重跑后，旧请求晚到也不会再 setState。
    try {
      const remote = await fetchBooks();
      setBooks(remote);
    } catch (err) {
      // 兜底策略：后端未启动时用 Data.json，让前端界面仍可演示。
      // 真正联调时 fetchBooks 成功，数据来自 Spring Boot + MySQL。
      setBooks(booksData);
      message.warning("后端服务暂不可用，已使用本地书籍数据展示。");
    } finally {
      setBooksLoading(false);
    }
  }, []);

  // 拉书籍列表
  useEffect(() => {
    let ignore = false;
    (async () => {
      if (!ignore) await loadBooks();
    })();
    return () => {
      ignore = true;
    };
  }, [loadBooks]);

  // 登录态变化时拉购物车
  const refreshCart = useCallback(async () => {
    if (!user) {
      // 未登录没有用户 id，后端购物车接口也无法定位用户；前端直接清空展示。
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
    // 派生状态：购物车角标数量由 cart 数组计算得出，不单独存一份，避免状态不一致。
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
      // 加车是需要用户身份的动作；本项目教学简化为 localStorage 用户 + userId 参数。
      message.warning("请先登录后再加入购物车");
      navigate("/profile");
      return;
    }
    if (book.stock !== undefined && book.stock !== null && book.stock <= 0) {
      message.warning("该书库存不足，暂时无法加入购物车");
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
      // checkout 调后端 POST /api/v1/orders?userId=...
      // 后端在一个事务里生成订单、写明细、清空购物车；前端成功后只需刷新购物车并跳转。
      await checkout(user.id);
      await refreshCart();
      await loadBooks();
      message.success("结算成功！已生成订单，可在「个人信息」查看。");
      navigate("/orders");
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
        <Route path="/orders" element={<OrdersPage user={user} />} />
        <Route path="/stats" element={<StatsPage user={user} />} />
        <Route path="/assistant" element={<Suspense fallback={<div className="assistant-route-loading" role="status">正在打开阅读助手…</div>}><AssistantPage books={books} /></Suspense>} />
        <Route path="/policies" element={<Suspense fallback={<div role="status">正在打开服务政策…</div>}><PolicyPage /></Suspense>} />
        <Route path="/guide" element={<Suspense fallback={<div role="status">正在打开购书向导…</div>}><GuidePage /></Suspense>} />
        <Route
          path="/admin/users"
          element={<AdminUsersPage user={user} />}
        />
        <Route
          path="/admin/books"
          element={
            <AdminBooksPage
              user={user}
              books={books}
              loading={booksLoading}
              onBooksChanged={loadBooks}
            />
          }
        />
      </Routes>
    </Layout>
  );
}
