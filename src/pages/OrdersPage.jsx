import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Empty,
  Input,
  List,
  message,
  Space,
  Tag,
  Typography,
} from "antd";
import { SearchOutlined, ShoppingOutlined } from "@ant-design/icons";
import { getOrders } from "../api/bookstoreApi.js";
import { formatDateTime, formatPrice } from "../utils/formatter.js";

const { RangePicker } = DatePicker;
const { Title, Text } = Typography;

export default function OrdersPage({ user }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [range, setRange] = useState(null);
  const [bookName, setBookName] = useState("");

  const isAdmin = user?.role === "ADMIN";

  const refresh = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const params = {
        userId: isAdmin ? undefined : user.id,
        startDate: range?.[0]?.format("YYYY-MM-DD"),
        endDate: range?.[1]?.format("YYYY-MM-DD"),
        bookName: bookName.trim(),
      };
      setOrders(await getOrders(params));
    } catch (err) {
      message.error(err.message || "获取订单失败");
    } finally {
      setLoading(false);
    }
  }, [bookName, isAdmin, range, user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (!user) {
    return <Alert type="warning" showIcon message="请先登录后查看订单" />;
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Space direction="vertical" size={2}>
        <Title level={2} style={{ margin: 0 }}>{isAdmin ? "订单管理" : "我的订单"}</Title>
        <Text type="secondary">
          {isAdmin ? "管理员可以查看并过滤系统中所有订单。" : "可以按时间范围和书籍名称过滤历史订单。"}
        </Text>
      </Space>

      <Card variant="borderless" className="profile-card">
        <Space wrap style={{ marginBottom: 16 }}>
          <RangePicker value={range} onChange={setRange} />
          <Input
            allowClear
            placeholder="书籍名称"
            value={bookName}
            onChange={(event) => setBookName(event.target.value)}
            onPressEnter={refresh}
            style={{ width: 220 }}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={refresh}>
            筛选
          </Button>
        </Space>

        {orders.length ? (
          <List
            loading={loading}
            itemLayout="vertical"
            dataSource={orders}
            renderItem={(order) => (
              <List.Item
                key={order.id}
                extra={
                  <Space direction="vertical" align="end">
                    {isAdmin && <Text type="secondary">{order.username}</Text>}
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
                            style={{ width: 32, height: 44, objectFit: "cover", borderRadius: 2 }}
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
        ) : (
          <Empty description={loading ? "正在加载订单" : "没有符合条件的订单"} />
        )}
      </Card>
    </div>
  );
}
