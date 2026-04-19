import { NavLink } from "react-router-dom";

export default function Layout({ cartCount, children }) {
  return (
    <>
      <header className="site-header">
        <div className="brand-block">
          <NavLink className="brand" to="/books">
            知页书城
          </NavLink>
          <p className="brand-text">React Router 重构版</p>
        </div>
        <nav className="site-nav" aria-label="主导航">
          <NavLink to="/books">首页</NavLink>
          <NavLink to="/cart">
            购物车 <span className="cart-count">{cartCount}</span>
          </NavLink>
          <NavLink to="/orders">订单</NavLink>
        </nav>
      </header>
      <main className="page-shell">{children}</main>
    </>
  );
}
