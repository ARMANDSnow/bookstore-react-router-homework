import { useCallback, useEffect, useState } from "react";
import {
  Form,
  Input,
  Button,
  message,
  Tabs,
  Descriptions,
  List,
  Tag,
  Typography,
  Empty,
  Skeleton,
  Space,
} from "antd";
import {
  LockOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
  LogoutOutlined,
  ShoppingOutlined,
} from "@ant-design/icons";
import { login, register } from "../services/authService.js";
import { getOrders } from "../api/bookstoreApi.js";
import { formatDateTime, formatPrice } from "../utils/formatter.js";

const { Text } = Typography;

function LoginPanel({ onSuccess }) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleLogin = async (values) => {
    setSubmitting(true);
    try {
      // login() 属于 authService：内部先调后端登录接口，再把返回用户写入 localStorage。
      const user = await login(values);
      message.success(`登录成功，欢迎 ${user.username}`);
      form.resetFields();
      onSuccess?.(user);
    } catch (err) {
      message.error(err.message || "登录失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Form form={form} layout="vertical" onFinish={handleLogin} className="account-form">
      <Form.Item
        label="用户名"
        name="username"
        rules={[{ required: true, message: "请输入用户名" }]}
      >
        <Input
          prefix={<UserOutlined />}
          placeholder="demo"
          size="large"
          autoComplete="username"
        />
      </Form.Item>
      <Form.Item
        label="密码"
        name="password"
        rules={[{ required: true, message: "请输入密码" }]}
      >
        <Input.Password
          prefix={<LockOutlined />}
          placeholder="123456"
          size="large"
          autoComplete="current-password"
        />
      </Form.Item>
      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          block
          size="large"
          loading={submitting}
        >
          登录
        </Button>
      </Form.Item>
      <Text type="secondary" className="account-demo-note">
        演示账号：demo / 123456
      </Text>
    </Form>
  );
}

function RegisterPanel() {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleRegister = async (values) => {
    setSubmitting(true);
    try {
      // 注册成功不自动登录，便于答辩时分别演示"注册"和"登录"两个接口。
      const user = await register(values);
      message.success(`注册成功：${user.username}，可前往「登录」标签登录`);
      form.resetFields();
    } catch (err) {
      message.error(err.message || "注册失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Form form={form} layout="vertical" onFinish={handleRegister} className="account-form">
      <Form.Item
        label="用户名"
        name="username"
        rules={[{ required: true, message: "请输入用户名" }]}
      >
        <Input prefix={<UserOutlined />} placeholder="请输入用户名" size="large" />
      </Form.Item>
      <Form.Item
        label="密码"
        name="password"
        dependencies={["confirmPassword"]}
        rules={[
          { required: true, message: "请输入密码" },
          { min: 6, message: "密码至少 6 位" },
        ]}
      >
        <Input.Password
          prefix={<LockOutlined />}
          placeholder="密码至少 6 位"
          size="large"
        />
      </Form.Item>
      <Form.Item
        label="重复密码"
        name="confirmPassword"
        dependencies={["password"]}
        rules={[
          { required: true, message: "请再次输入密码" },
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue("password") === value) {
                return Promise.resolve();
              }
              return Promise.reject(new Error("两次输入的密码不一致"));
            },
          }),
        ]}
      >
        <Input.Password
          prefix={<LockOutlined />}
          placeholder="请再次输入密码"
          size="large"
          autoComplete="new-password"
        />
      </Form.Item>
      <Form.Item
        label="电子邮箱"
        name="email"
        rules={[
          { required: true, message: "请输入邮箱" },
          { type: "email", message: "邮箱格式不正确" },
        ]}
      >
        <Input prefix={<MailOutlined />} placeholder="请输入邮箱" size="large" />
      </Form.Item>
      <Form.Item label="手机号码" name="phone">
        <Input prefix={<PhoneOutlined />} placeholder="选填" size="large" />
      </Form.Item>
      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          block
          size="large"
          loading={submitting}
        >
          注册
        </Button>
      </Form.Item>
    </Form>
  );
}

function OrderHistory({ userId }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      // 订单历史直接从后端读取：GET /api/v1/orders?userId=...
      // 返回的 OrderDto 已经包含 items，前端不需要再逐条查订单明细。
      const data = await getOrders(userId);
      setOrders(data);
    } catch (err) {
      message.error(err.message || "获取订单失败");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (loading) return <div className="state-panel"><Skeleton active paragraph={{ rows: 4 }} /></div>;
  if (!orders.length)
    return <div className="state-panel"><Empty description="还没有订单，快去下单吧" /></div>;

  return (
    <List
      className="account-order-list"
      itemLayout="vertical"
      dataSource={orders}
      renderItem={(order) => (
        <List.Item key={order.id}>
          <div className="account-order-heading">
            <div>
              <h3><ShoppingOutlined /> 订单 #{order.id}</h3>
              <p className="account-order-date">下单时间：{formatDateTime(order.createdAt)}</p>
            </div>
            <Space className="account-order-total" direction="vertical" align="end">
              <Tag color={order.status === "PAID" ? "green" : "orange"}>
                {order.status === "PAID" ? "已支付" : "待支付"}
              </Tag>
              <Text strong className="price-text">
                {formatPrice(order.totalAmount)}
              </Text>
            </Space>
          </div>
          <List
            className="account-order-items"
            size="small"
            dataSource={order.items}
            renderItem={(item) => (
              <List.Item key={item.id}>
                <div className="account-order-book">
                  {item.bookImage && (
                    <img
                      src={item.bookImage}
                      alt={item.bookTitle}
                      className="account-order-cover"
                    />
                  )}
                  <span className="account-order-book-title">{item.bookTitle}</span>
                  <Text type="secondary" className="account-order-quantity">× {item.quantity}</Text>
                  <Text className="account-order-unit-price">{formatPrice(item.unitPrice)}</Text>
                </div>
              </List.Item>
            )}
          />
        </List.Item>
      )}
    />
  );
}

export default function ProfilePage({ user, onLogout }) {
  if (user) {
    // 已登录态：展示用户 DTO（不含 password）和订单历史。
    return (
      <section className="page account-page">
        <header className="page-heading">
          <div>
            <p className="eyebrow">我的书城</p>
            <h1>个人信息</h1>
            <p className="page-description">管理账户信息，回顾每一次选书。</p>
          </div>
          <div className="page-actions">
            <Button danger icon={<LogoutOutlined />} onClick={onLogout}>
              退出登录
            </Button>
          </div>
        </header>
        <section className="paper-panel account-overview" aria-labelledby="account-overview-title">
          <h2 id="account-overview-title" className="account-section-title">账户信息</h2>
          <Descriptions column={{ xs: 1, sm: 2 }} layout="vertical" size="small">
            <Descriptions.Item label="用户名">{user.username}</Descriptions.Item>
            <Descriptions.Item label="角色">
              <Tag color={user.role === "ADMIN" ? "gold" : "blue"}>
                {user.role === "ADMIN" ? "管理员" : "顾客"}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="账号状态">
              <Tag color={user.enabled === false ? "red" : "green"}>
                {user.enabled === false ? "已禁用" : "正常"}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="邮箱">{user.email || "—"}</Descriptions.Item>
            <Descriptions.Item label="手机号">{user.phone || "—"}</Descriptions.Item>
            <Descriptions.Item label="注册时间">
              {formatDateTime(user.createdAt)}
            </Descriptions.Item>
          </Descriptions>
        </section>
        <section className="paper-panel account-orders" aria-labelledby="account-orders-title">
          <h2 id="account-orders-title" className="account-section-title">我的订单</h2>
          <OrderHistory userId={user.id} />
        </section>
      </section>
    );
  }

  // 未登录态：同一页面内用 Tabs 切换登录/注册，两个表单都走 Service 层而不是直接 fetch。
  return (
    <section className="page account-page account-auth-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">我的书城</p>
          <h1>欢迎来到知页书城</h1>
          <p className="page-description">登录后保存购物车，查看您的订单与购书记录。</p>
        </div>
      </header>
      <section className="paper-panel account-auth">
        <Tabs
          className="account-auth-tabs"
          defaultActiveKey="login"
          items={[
            {
              key: "login",
              label: "登录",
              children: <LoginPanel />,
            },
            {
              key: "register",
              label: "注册",
              children: <RegisterPanel />,
            },
          ]}
        />
      </section>
    </section>
  );
}
