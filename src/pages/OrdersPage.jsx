export default function OrdersPage({ orders }) {
  const fallbackOrders = [
    {
      id: "ZY20260419001",
      createdAt: "2026/4/19 10:30:00",
      status: "已完成",
      total: 137,
      items: [
        { title: "三体", quantity: 1 },
        { title: "深度工作", quantity: 1 },
      ],
    },
  ];
  const allOrders = orders.length ? orders : fallbackOrders;

  return (
    <>
      <section className="orders-header">
        <header className="section-header compact">
          <p className="eyebrow">Orders</p>
          <h1>我的订单</h1>
          <p>提交购物车后，新订单会展示在这里。</p>
        </header>
      </section>
      <section className="orders-grid" aria-label="订单列表">
        {allOrders.map((order) => (
          <article className="order-card" key={order.id}>
            <header className="order-top">
              <div>
                <h2>订单号 {order.id}</h2>
                <p>下单时间：{order.createdAt}</p>
              </div>
              <span className="status-tag success">{order.status}</span>
            </header>
            <ul className="order-items">
              {order.items.map((item) => (
                <li key={`${order.id}-${item.title}`}>
                  《{item.title}》 x {item.quantity}
                </li>
              ))}
            </ul>
            <footer className="order-bottom">
              <p>收货人：游客用户</p>
              <strong>合计：¥{Number(order.total).toFixed(2)}</strong>
            </footer>
          </article>
        ))}
      </section>
    </>
  );
}
