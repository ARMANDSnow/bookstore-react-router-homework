import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  DatePicker,
  message,
  Row,
  Col,
  Space,
  Statistic,
  Table,
  Typography,
} from "antd";
import { BarChartOutlined, SearchOutlined } from "@ant-design/icons";
import {
  getCustomerStats,
  getSalesRank,
  getUserSpendRank,
} from "../api/bookstoreApi.js";
import { formatPrice } from "../utils/formatter.js";

const { RangePicker } = DatePicker;
const { Title, Text } = Typography;

export default function StatsPage({ user }) {
  const [range, setRange] = useState(null);
  const [loading, setLoading] = useState(false);
  const [salesRank, setSalesRank] = useState([]);
  const [userRank, setUserRank] = useState([]);
  const [customerStats, setCustomerStats] = useState(null);

  const isAdmin = user?.role === "ADMIN";

  const buildParams = useCallback(() => ({
    startDate: range?.[0]?.format("YYYY-MM-DD"),
    endDate: range?.[1]?.format("YYYY-MM-DD"),
  }), [range]);

  const refresh = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const params = buildParams();
      if (isAdmin) {
        const [sales, users] = await Promise.all([
          getSalesRank(params),
          getUserSpendRank(params),
        ]);
        setSalesRank(sales);
        setUserRank(users);
      } else {
        setCustomerStats(await getCustomerStats({ ...params, userId: user.id }));
      }
    } catch (err) {
      message.error(err.message || "获取统计数据失败");
    } finally {
      setLoading(false);
    }
  }, [buildParams, isAdmin, user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (!user) {
    return <Alert type="warning" showIcon message="请先登录后查看统计" />;
  }

  const salesColumns = [
    { title: "排名", render: (_, __, index) => index + 1, width: 80 },
    { title: "书名", dataIndex: "bookTitle" },
    { title: "销量", dataIndex: "quantity" },
    { title: "销售额", dataIndex: "totalAmount", render: formatPrice },
  ];

  const userColumns = [
    { title: "排名", render: (_, __, index) => index + 1, width: 80 },
    { title: "用户", dataIndex: "username" },
    { title: "订单数", dataIndex: "orderCount" },
    { title: "累计消费", dataIndex: "totalAmount", render: formatPrice },
  ];

  const customerColumns = [
    { title: "书名", dataIndex: "bookTitle" },
    { title: "购买数量", dataIndex: "quantity" },
    { title: "金额", dataIndex: "totalAmount", render: formatPrice },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Space direction="vertical" size={2}>
        <Title level={2} style={{ margin: 0 }}>统计分析</Title>
        <Text type="secondary">
          {isAdmin ? "按指定时间范围查看图书销量和用户消费排行。" : "按指定时间范围查看自己的购书情况。"}
        </Text>
      </Space>

      <Card variant="borderless" className="profile-card">
        <Space wrap>
          <RangePicker value={range} onChange={setRange} />
          <Button type="primary" icon={<SearchOutlined />} onClick={refresh}>
            统计
          </Button>
        </Space>
      </Card>

      {isAdmin ? (
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card
              title={<Space><BarChartOutlined />热销榜</Space>}
              variant="borderless"
              className="profile-card"
            >
              <Table
                rowKey="bookId"
                loading={loading}
                columns={salesColumns}
                dataSource={salesRank}
                pagination={false}
              />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card
              title={<Space><BarChartOutlined />消费榜</Space>}
              variant="borderless"
              className="profile-card"
            >
              <Table
                rowKey="userId"
                loading={loading}
                columns={userColumns}
                dataSource={userRank}
                pagination={false}
              />
            </Card>
          </Col>
        </Row>
      ) : (
        <Row gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Card variant="borderless" className="profile-card">
              <Statistic title="购书总本数" value={customerStats?.totalBooks || 0} suffix="本" />
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card variant="borderless" className="profile-card">
              <Statistic
                title="购书总金额"
                value={Number(customerStats?.totalAmount || 0)}
                precision={2}
                prefix="¥"
                valueStyle={{ color: "#cf1322" }}
              />
            </Card>
          </Col>
          <Col xs={24}>
            <Card title="购书明细" variant="borderless" className="profile-card">
              <Table
                rowKey="bookId"
                loading={loading}
                columns={customerColumns}
                dataSource={customerStats?.books || []}
                pagination={false}
              />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}
