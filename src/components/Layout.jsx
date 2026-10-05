import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button, Drawer, Input } from "antd";
import {
  ShoppingCartOutlined,
  UserOutlined,
  BookOutlined,
  LogoutOutlined,
  TeamOutlined,
  DatabaseOutlined,
  OrderedListOutlined,
  BarChartOutlined,
  MenuOutlined,
  CommentOutlined,
} from "@ant-design/icons";

export default function Layout({ cartCount, children, user, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const keyword = new URLSearchParams(location.search).get("keyword") || "";
  const [searchValue, setSearchValue] = useState(keyword);
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    setSearchValue(keyword);
  }, [keyword]);

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname, location.search]);

  const links = [
    { path: "/books", label: "图书目录", icon: <BookOutlined /> },
    { path: "/assistant", label: "阅读助手", icon: <CommentOutlined /> },
    { path: "/policies", label: "服务政策", icon: <BookOutlined /> },
    { path: "/cart", label: "购物车", icon: <ShoppingCartOutlined /> },
    { path: "/orders", label: isAdmin ? "订单管理" : "我的订单", icon: <OrderedListOutlined /> },
    { path: "/stats", label: "统计分析", icon: <BarChartOutlined /> },
    ...(isAdmin ? [
      { path: "/admin/users", label: "用户管理", icon: <TeamOutlined /> },
      { path: "/admin/books", label: "书籍管理", icon: <DatabaseOutlined /> },
    ] : []),
    { path: "/profile", label: "个人信息", icon: <UserOutlined /> },
  ];

  function searchBooks(value) {
    const query = (value || "").trim();
    navigate(query ? `/books?keyword=${encodeURIComponent(query)}` : "/books");
  }

  function navigationLink(link) {
    const active = location.pathname === link.path ||
      (link.path === "/books" && location.pathname.startsWith("/books/"));
    return (
      <Link
        key={link.path}
        to={link.path}
        className={`nav-link${active ? " active" : ""}`}
        aria-current={active ? "page" : undefined}
        onClick={() => setMenuOpen(false)}
      >
        {link.icon}
        <span>{link.label}</span>
        {link.path === "/cart" && cartCount > 0 && (
          <span className="nav-count" aria-label={`购物车 ${cartCount} 本图书`}>{cartCount}</span>
        )}
      </Link>
    );
  }

  return (
    <div className="bookstore-app">
      <a className="skip-link" href="#main-content">跳到主要内容</a>
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/books" aria-label="知页书城，返回图书目录">
            <span className="brand-mark" aria-hidden="true"><BookOutlined /></span>
            <span>
              <strong className="brand-name">知页书城</strong>
              <span className="brand-tag">把好书带回家</span>
            </span>
          </Link>

          <div className="site-search" role="search" aria-label="搜索图书">
            <label className="search-label" htmlFor="book-search">搜索图书</label>
            <Input.Search
              id="book-search"
              value={searchValue}
              onChange={(event) => setSearchValue(event.target.value)}
              onSearch={searchBooks}
              placeholder="搜索书名 / 作者"
              allowClear
              enterButton="搜索"
              size="large"
            />
          </div>

          <div className="header-account">
            {user ? (
              <>
                <Link to="/profile" className="account-chip" aria-label={`查看 ${user.username} 的个人信息`}>
                  <span className="account-avatar" aria-hidden="true">{String(user.username || "读者").slice(0, 1).toUpperCase()}</span>
                  <span className="account-copy">
                    <strong>{user.username}</strong>
                    <small>{isAdmin ? "管理员账户" : "读者账户"}</small>
                  </span>
                </Link>
                <Button className="header-logout" type="text" icon={<LogoutOutlined />} onClick={onLogout}>退出登录</Button>
              </>
            ) : (
              <Button className="header-login" onClick={() => navigate("/profile")} icon={<UserOutlined />}>去登录</Button>
            )}
            <Button
              className="mobile-menu-toggle"
              icon={<MenuOutlined />}
              onClick={() => setMenuOpen(true)}
              aria-label="打开导航菜单"
            />
          </div>
        </div>
        <nav className="top-nav" aria-label="主要导航">
          {links.map(navigationLink)}
          {!isAdmin && <span className="nav-tail">每一本，都是新世界的入口</span>}
        </nav>
      </header>

      <Drawer title="知页书城" open={menuOpen} onClose={() => setMenuOpen(false)} placement="right">
        <nav className="mobile-nav" aria-label="移动端导航">
          {links.map(navigationLink)}
        </nav>
        {user && <Button type="text" icon={<LogoutOutlined />} onClick={() => { setMenuOpen(false); onLogout(); }}>退出登录</Button>}
      </Drawer>

      <main id="main-content" className="site-main" tabIndex={-1}>
        {children}
      </main>
      <footer className="site-footer">
        <div className="site-footer-inner">
          <span><strong>知页书城</strong> · 把好书带回家</span>
          <span>从技术实践到人文思考，找到适合你的那一本。</span>
        </div>
      </footer>
    </div>
  );
}
