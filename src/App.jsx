import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";
import booksData from "./data/Data.json";
import Layout from "./components/Layout.jsx";
import BookListPage from "./pages/BookListPage.jsx";
import BookDetailPage from "./pages/BookDetailPage.jsx";
import CartPage from "./pages/CartPage.jsx";
import OrdersPage from "./pages/OrdersPage.jsx";

const CART_KEY = "react-bookstore-cart";
const ORDERS_KEY = "react-bookstore-orders";

function getStoredArray(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || "[]");
  } catch {
    return [];
  }
}

export default function App() {
  const [books] = useState(booksData);
  const [selectedBook, setSelectedBook] = useState(null);
  const [cart, setCart] = useState(() => getStoredArray(CART_KEY));
  const [orders, setOrders] = useState(() => getStoredArray(ORDERS_KEY));
  const navigate = useNavigate();

  const cartCount = useMemo(
    () => cart.reduce((sum, item) => sum + item.quantity, 0),
    [cart]
  );

  function persistCart(nextCart) {
    setCart(nextCart);
    localStorage.setItem(CART_KEY, JSON.stringify(nextCart));
  }

  function persistOrders(nextOrders) {
    setOrders(nextOrders);
    localStorage.setItem(ORDERS_KEY, JSON.stringify(nextOrders));
  }

  function openBookDetail(book) {
    setSelectedBook(book);
    navigate(`/books/${book.id}`, { state: { book } });
  }

  function addToCart(book) {
    const exists = cart.find((item) => item.id === book.id);
    const nextCart = exists
      ? cart.map((item) =>
          item.id === book.id ? { ...item, quantity: item.quantity + 1 } : item
        )
      : [...cart, { ...book, quantity: 1 }];

    persistCart(nextCart);
  }

  function updateQuantity(bookId, delta) {
    const nextCart = cart
      .map((item) =>
        item.id === bookId
          ? { ...item, quantity: Math.max(1, item.quantity + delta) }
          : item
      )
      .filter((item) => item.quantity > 0);

    persistCart(nextCart);
  }

  function removeFromCart(bookId) {
    persistCart(cart.filter((item) => item.id !== bookId));
  }

  function submitOrder() {
    if (!cart.length) {
      return false;
    }

    const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const shipping = subtotal > 0 && subtotal < 99 ? 12 : 0;
    const nextOrders = [
      {
        id: `ZY${Date.now()}`,
        createdAt: new Date().toLocaleString("zh-CN", { hour12: false }),
        status: "待发货",
        total: subtotal + shipping,
        items: cart,
      },
      ...orders,
    ];

    persistOrders(nextOrders);
    persistCart([]);
    navigate("/orders");
    return true;
  }

  return (
    <Layout cartCount={cartCount}>
      <Routes>
        <Route path="/" element={<Navigate to="/books" replace />} />
        <Route
          path="/books"
          element={
            <BookListPage
              books={books}
              onBookSelect={openBookDetail}
              onAddToCart={addToCart}
            />
          }
        />
        <Route
          path="/books/:bookId"
          element={
            <BookDetailPage
              books={books}
              selectedBook={selectedBook}
              onBookSelect={openBookDetail}
              onAddToCart={addToCart}
            />
          }
        />
        <Route
          path="/cart"
          element={
            <CartPage
              cart={cart}
              onUpdateQuantity={updateQuantity}
              onRemove={removeFromCart}
              onSubmitOrder={submitOrder}
            />
          }
        />
        <Route path="/orders" element={<OrdersPage orders={orders} />} />
      </Routes>
    </Layout>
  );
}
