import { useCallback, useEffect, useState } from "react";
import {
  Form,
  Input,
  Button,
  Card,
  message,
  Tabs,
  Descriptions,
  List,
  Tag,
  Typography,
  Empty,
  Skeleton,
  Space,
  Divider,
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

const { Title, Text } = Typography;

function LoginPanel({ onSuccess }) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleLogin = async (values) => {
    setSubmitting(true);
    try {
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
    <Form form={form} layout="vertical" onFinish={handleLogin}>
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
      <Text type="secondary" style={{ display: "block", textAlign: "center" }}>
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
    <Form form={form} layout="vertical" onFinish={handleRegister}>
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

  if (loading) return <Skeleton active paragraph={{ rows: 4 }} />;
  if (!orders.length)
    return <Empty description="还没有订单，快去下单吧" />;

  return (
    <List
      itemLayout="vertical"
      dataSource={orders}
      renderItem={(order) => (
        <List.Item
          key={order.id}
          extra={
            <Space direction="vertical" align="end">
              <Tag color={order.status === "PAID" ? "green" : "orange"}>
                {order.status === "PAID" ? "已支付" : "待支付"}
              </Tag>
              <Text strong style={{ fontSize: 18, color: "#cf1322" }}>
                {formatPrice(order.totalAmount)}
              </Text>
            </Space>
          }
        >
          <List.Item.Meta
            avatar={<ShoppingOutlined style={{ fontSize: 24, color: "#2f6f64" }} />}
            title={`订单 #${order.id}`}
            description={`下单时间：${formatDateTime(order.createdAt)}`}
          />
          <List
            size="small"
            dataSource={order.items}
            renderItem={(item) => (
              <List.Item key={item.id}>
                <Space>
                  {item.bookImage && (
                    <img
                      src={item.bookImage}
                      alt={item.bookTitle}
                      style={{
                        width: 32,
                        height: 44,
                        objectFit: "cover",
                        borderRadius: 2,
                      }}
                    />
                  )}
                  <span>{item.bookTitle}</span>
                  <Text type="secondary">× {item.quantity}</Text>
                  <Text>{formatPrice(item.unitPrice)}</Text>
                </Space>
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
    return (
      <div style={{ maxWidth: 800, margin: "0 auto", padding: "24px 0" }}>
        <Card
          title="账户信息"
          variant="borderless"
          style={{ boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}
          extra={
            <Button danger icon={<LogoutOutlined />} onClick={onLogout}>
              退出登录
            </Button>
          }
        >
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="用户名">{user.username}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{user.email || "—"}</Descriptions.Item>
            <Descriptions.Item label="手机号">{user.phone || "—"}</Descriptions.Item>
            <Descriptions.Item label="注册时间">
              {formatDateTime(user.createdAt)}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <Divider />

        <Card
          title="我的订单"
          variant="borderless"
          style={{ boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}
        >
          <OrderHistory userId={user.id} />
        </Card>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: "0 auto", padding: "24px 0" }}>
      <Card
        title={<Title level={4} style={{ margin: 0 }}>欢迎来到知页书城</Title>}
        variant="borderless"
        style={{ boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}
      >
        <Tabs
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
      </Card>
    </div>
  );
}
