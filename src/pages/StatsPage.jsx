import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  DatePicker,
  message,
  Statistic,
  Table,
} from "antd";
import { BarChartOutlined, SearchOutlined } from "@ant-design/icons";
import {
  getCustomerStats,
  getSalesRank,
  getUserSpendRank,
} from "../api/bookstoreApi.js";
import { formatPrice } from "../utils/formatter.js";

const { RangePicker } = DatePicker;

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
    return (
      <section className="page stats-page">
        <header className="page-heading">
          <div>
            <p className="eyebrow">阅读记录</p>
            <h1>统计分析</h1>
            <p className="page-description">查看图书购买记录与消费情况。</p>
          </div>
        </header>
        <div className="state-panel">
          <Alert type="warning" showIcon message="请先登录后查看统计" />
        </div>
      </section>
    );
  }

  const salesColumns = [
    { title: "排名", render: (_, __, index) => index + 1, width: 68 },
    { title: "书名", dataIndex: "bookTitle", width: 210 },
    { title: "销量", dataIndex: "quantity", width: 80, align: "right" },
    { title: "销售额", dataIndex: "totalAmount", render: formatPrice, width: 120, align: "right" },
  ];

  const userColumns = [
    { title: "排名", render: (_, __, index) => index + 1, width: 68 },
    { title: "用户", dataIndex: "username", width: 180 },
    { title: "订单数", dataIndex: "orderCount", width: 90, align: "right" },
    { title: "累计消费", dataIndex: "totalAmount", render: formatPrice, width: 140, align: "right" },
  ];

  const customerColumns = [
    { title: "书名", dataIndex: "bookTitle", width: 260 },
    { title: "购买数量", dataIndex: "quantity", width: 120, align: "right" },
    { title: "金额", dataIndex: "totalAmount", render: formatPrice, width: 140, align: "right" },
  ];

  return (
    <section className="page stats-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">{isAdmin ? "书城经营" : "阅读记录"}</p>
          <h1>统计分析</h1>
          <p className="page-description">
            {isAdmin ? "按指定时间范围查看图书销量和用户消费排行。" : "按指定时间范围查看自己的购书情况。"}
          </p>
        </div>
      </header>
      <div className="paper-panel filter-bar stats-filter">
        <span className="stats-filter-label">统计时间</span>
        <RangePicker value={range} onChange={setRange} aria-label="统计日期范围" />
        <Button type="primary" icon={<SearchOutlined />} onClick={refresh} loading={loading}>
          统计
        </Button>
      </div>

      {isAdmin ? (
        <div className="stats-rank-grid">
          <section className="table-panel stats-table-panel" aria-labelledby="sales-rank-title">
            <h2 id="sales-rank-title" className="stats-section-title"><BarChartOutlined /> 热销榜</h2>
            <Table
              rowKey="bookId"
              loading={loading}
              columns={salesColumns}
              dataSource={salesRank}
              pagination={false}
              scroll={{ x: 478 }}
            />
          </section>
          <section className="table-panel stats-table-panel" aria-labelledby="spending-rank-title">
            <h2 id="spending-rank-title" className="stats-section-title"><BarChartOutlined /> 消费榜</h2>
            <Table
              rowKey="userId"
              loading={loading}
              columns={userColumns}
              dataSource={userRank}
              pagination={false}
              scroll={{ x: 478 }}
            />
          </section>
        </div>
      ) : (
        <>
          <div className="metric-grid stats-metrics">
            <section className="metric-card">
              <Statistic title="购书总本数" value={customerStats?.totalBooks || 0} suffix="本" loading={loading} />
            </section>
            <section className="metric-card">
              <Statistic
                title="购书总金额"
                value={Number(customerStats?.totalAmount || 0)}
                precision={2}
                prefix="¥"
                loading={loading}
              />
            </section>
          </div>
          <section className="table-panel stats-table-panel" aria-labelledby="customer-books-title">
            <h2 id="customer-books-title" className="stats-section-title">购书明细</h2>
            <Table
              rowKey="bookId"
              loading={loading}
              columns={customerColumns}
              dataSource={customerStats?.books || []}
              pagination={false}
              scroll={{ x: 520 }}
            />
          </section>
        </>
      )}
    </section>
  );
}
