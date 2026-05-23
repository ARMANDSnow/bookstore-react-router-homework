import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Layout as AntLayout, Menu, Badge, Button, Space, Avatar } from "antd";
import {
  HomeOutlined,
  ShoppingCartOutlined,
  UserOutlined,
  BookOutlined,
  LogoutOutlined,
} from "@ant-design/icons";

const { Header, Sider, Content } = AntLayout;

export default function Layout({ cartCount, children, user, onLogout }) {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();

  const getSelectedKey = () => {
    if (location.pathname.startsWith("/books")) return "books";
    if (location.pathname.startsWith("/cart")) return "cart";
    if (location.pathname.startsWith("/profile")) return "profile";
    return "books";
  };

  const menuItems = [
    {
      key: "books",
      icon: <HomeOutlined />,
      label: <Link to="/books">主页</Link>,
    },
    {
      key: "cart",
      icon: <ShoppingCartOutlined />,
      label: (
        <Link to="/cart">
          购物车{" "}
          <Badge
            count={cartCount}
            size="small"
            offset={[10, 0]}
            style={{ backgroundColor: "#1890ff" }}
          />
        </Link>
      ),
    },
    {
      key: "profile",
      icon: <UserOutlined />,
      label: <Link to="/profile">个人信息</Link>,
    },
  ];

  return (
    <AntLayout style={{ minHeight: "100vh" }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={(value) => setCollapsed(value)}
        theme="light"
        style={{
          boxShadow: "2px 0 8px 0 rgba(29,35,41,.05)",
          zIndex: 10,
        }}
      >
        <div
          className="app-logo"
          style={{
            height: 64,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: collapsed ? 24 : 20,
            fontWeight: "bold",
            color: "#1f4f49",
            borderBottom: "1px solid rgba(47, 111, 100, 0.12)",
            overflow: "hidden",
            whiteSpace: "nowrap",
          }}
        >
          <BookOutlined style={{ marginRight: collapsed ? 0 : 8 }} />
          {!collapsed && "知页书城"}
        </div>
        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[getSelectedKey()]}
          items={menuItems}
          style={{ borderRight: 0, marginTop: 16 }}
        />
      </Sider>
      <AntLayout>
        <Header
          style={{
            padding: "0 24px",
            background: "#fff",
            boxShadow: "0 1px 4px rgba(0,21,41,.08)",
            display: "flex",
            alignItems: "center",
            justifyContent: "flex-end",
          }}
        >
          <Space size={16} align="center">
            {user ? (
              <>
                <Avatar
                  size="small"
                  icon={<UserOutlined />}
                  style={{ backgroundColor: "#2f6f64" }}
                />
                <span>欢迎回来，{user.username}</span>
                <Button
                  size="small"
                  icon={<LogoutOutlined />}
                  onClick={onLogout}
                >
                  退出登录
                </Button>
              </>
            ) : (
              <>
                <span>未登录</span>
                <Link to="/profile">
                  <Button size="small" type="primary">
                    去登录
                  </Button>
                </Link>
              </>
            )}
          </Space>
        </Header>
        <Content
          style={{
            margin: "24px 16px",
            padding: 24,
            background: "#fff",
            borderRadius: 8,
            overflow: "initial",
          }}
        >
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  );
}
