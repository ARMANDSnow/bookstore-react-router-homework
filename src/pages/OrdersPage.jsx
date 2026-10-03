import { useCallback, useEffect, useState } from "react";
import { Alert, Button, DatePicker, Empty, Input, message, Skeleton, Spin, Tag } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import { getOrders } from "../api/bookstoreApi.js";
import { formatDateTime, formatPrice } from "../utils/formatter.js";

const { RangePicker } = DatePicker;

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

  useEffect(() => { refresh(); }, [refresh]);
  if (!user) {
    return <div className="state-panel"><Alert type="warning" showIcon message="请先登录后查看订单" /></div>;
  }

  return (
    <div className="page orders-page">
      <div className="page-heading"><div><p className="eyebrow">每一次选择，都是阅读的起点</p><h1>{isAdmin ? "订单管理" : "我的订单"}</h1><p className="page-description">{isAdmin ? "查看系统中的全部订单，按日期和书名查找。" : "查看历史订单，按下单日期和书名查找。"}</p></div></div>
      <section className="filter-bar orders-filters" aria-label="订单筛选">
        <div className="orders-filter-field orders-date-field"><label htmlFor="orders-date-range">下单日期</label><RangePicker id="orders-date-range" value={range} onChange={setRange} className="orders-date-range" classNames={{ popup: { root: "orders-date-popup" } }} /></div>
        <div className="orders-filter-field orders-book-field"><label htmlFor="orders-book-name">书籍名称</label><Input id="orders-book-name" allowClear placeholder="输入书名" value={bookName} onChange={(event) => setBookName(event.target.value)} onPressEnter={refresh} /></div>
        <Button type="primary" icon={<SearchOutlined />} onClick={refresh}>筛选订单</Button>
      </section>
      <Spin spinning={loading && orders.length > 0}>
        {orders.length ? (
          <section className="orders-list" aria-label="订单列表">
            <p className="orders-result-count">共 {orders.length} 笔订单</p>
            {orders.map((order) => (
              <article className="paper-panel orders-card" key={order.id} aria-labelledby={`order-title-${order.id}`}>
                <header className="orders-card-heading"><div><h2 id={`order-title-${order.id}`}>订单 #{order.id}</h2><p><span>下单时间</span><time dateTime={order.createdAt}>{formatDateTime(order.createdAt)}</time></p></div><div className="orders-card-status">{isAdmin && <span className="orders-owner">{order.username}</span>}<Tag className={order.status === "PAID" ? "orders-status-paid" : "orders-status-pending"}>{order.status === "PAID" ? "已支付" : "待支付"}</Tag></div></header>
                <div className="orders-item-heading" aria-hidden="true"><span>图书明细</span><span>单价</span><span>数量</span><span>小计</span></div>
                <ul className="orders-item-list">{order.items.map((item) => <li className="orders-item" key={item.id}><div className="orders-item-book">{item.bookImage && <img src={item.bookImage} alt={`《${item.bookTitle}》完整书封`} width="42" height="60" />}<span>{item.bookTitle}</span></div><div className="orders-item-unit"><span className="orders-mobile-label">单价</span>{formatPrice(item.unitPrice)}</div><div className="orders-item-quantity"><span className="orders-mobile-label">数量</span>× {item.quantity}</div><div className="orders-item-subtotal"><span className="orders-mobile-label">小计</span>{formatPrice(item.unitPrice * item.quantity)}</div></li>)}</ul>
                <div className="orders-card-total"><span>订单总额</span><strong>{formatPrice(order.totalAmount)}</strong></div>
              </article>
            ))}
          </section>
        ) : loading ? <div className="state-panel" role="status" aria-label="正在加载订单"><Skeleton active paragraph={{ rows: 6 }} /></div> : <div className="state-panel"><Empty description="没有符合条件的订单" /></div>}
      </Spin>
    </div>
  );
}
