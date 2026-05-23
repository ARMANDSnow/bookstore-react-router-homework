import { Link } from "react-router-dom";
import {
  Table,
  Button,
  InputNumber,
  Popconfirm,
  Card,
  Row,
  Col,
  Statistic,
  Typography,
  Space,
  Skeleton,
  Alert,
} from "antd";
import {
  DeleteOutlined,
  ShoppingCartOutlined,
  CreditCardOutlined,
} from "@ant-design/icons";
import { formatPrice } from "../utils/formatter.js";

const { Title, Text } = Typography;

export default function CartPage({
  cart,
  loading,
  user,
  onUpdateQuantity,
  onRemove,
  onSubmitOrder,
}) {
  if (!user) {
    return (
      <div style={{ maxWidth: 600, margin: "60px auto" }}>
        <Alert
          type="warning"
          showIcon
          message="尚未登录"
          description="请先去「个人信息」登录，登录后购物车数据会自动从后端加载并随你保留。"
          action={
            <Link to="/profile">
              <Button type="primary">去登录</Button>
            </Link>
          }
        />
      </div>
    );
  }

  if (loading) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  const subtotal = cart.reduce(
    (sum, item) => sum + Number(item.price) * item.quantity,
    0
  );
  const shipping = subtotal > 0 && subtotal < 99 ? 12 : 0;
  const total = subtotal + shipping;

  const columns = [
    {
      title: "商品信息",
      dataIndex: "title",
      key: "title",
      render: (text, record) => (
        <Space>
          <img
            src={record.image}
            alt={text}
            style={{
              width: 50,
              height: 70,
              objectFit: "cover",
              borderRadius: 4,
            }}
          />
          <Link to={`/books/${record.bookId}`} style={{ fontWeight: 500 }}>
            {text}
          </Link>
        </Space>
      ),
    },
    {
      title: "单价",
      dataIndex: "price",
      key: "price",
      render: (price) => <Text type="danger">{formatPrice(price)}</Text>,
    },
    {
      title: "数量",
      key: "quantity",
      render: (_, record) => (
        <InputNumber
          min={1}
          max={99}
          value={record.quantity}
          onChange={(value) => onUpdateQuantity(record.id, value)}
        />
      ),
    },
    {
      title: "小计",
      key: "subtotal",
      render: (_, record) => (
        <Text strong>{formatPrice(record.price * record.quantity)}</Text>
      ),
    },
    {
      title: "操作",
      key: "action",
      render: (_, record) => (
        <Popconfirm
          title="确定要移除该商品吗？"
          onConfirm={() => onRemove(record.id)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="text" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ];

  if (!cart.length) {
    return (
      <div style={{ textAlign: "center", padding: "100px 0" }}>
        <ShoppingCartOutlined
          style={{ fontSize: 64, color: "#d9d9d9", marginBottom: 24 }}
        />
        <Title level={3}>购物车是空的</Title>
        <Text type="secondary" style={{ display: "block", marginBottom: 24 }}>
          快去挑选几本好书吧！
        </Text>
        <Link to="/books">
          <Button type="primary" size="large">
            去逛逛
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      <Title level={2}>我的购物车</Title>

      <Table
        columns={columns}
        dataSource={cart}
        rowKey="id"
        pagination={false}
        className="cart-table"
        style={{ boxShadow: "0 2px 8px rgba(0,0,0,0.06)" }}
      />

      <Row justify="end">
        <Col xs={24} md={10} lg={8}>
          <Card
            className="cart-summary-card"
            style={{ boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 16,
              }}
            >
              <Text>商品总价</Text>
              <Text>{formatPrice(subtotal)}</Text>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 16,
              }}
            >
              <Text>
                运费{" "}
                {shipping === 0 && <Text type="success">(满99包邮)</Text>}
              </Text>
              <Text>{formatPrice(shipping)}</Text>
            </div>
            <hr
              style={{
                border: 0,
                borderTop: "1px solid #f0f0f0",
                margin: "16px 0",
              }}
            />
            <Statistic
              title={
                <span style={{ fontSize: 16, fontWeight: "bold" }}>
                  应付总额
                </span>
              }
              value={total}
              precision={2}
              prefix="¥"
              valueStyle={{ color: "#cf1322", fontWeight: "bold" }}
            />
            <Button
              type="primary"
              size="large"
              block
              style={{ marginTop: 24, height: 48, fontSize: 18 }}
              icon={<CreditCardOutlined />}
              onClick={onSubmitOrder}
            >
              提交订单
            </Button>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
