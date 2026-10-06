import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Alert, Button, InputNumber, Popconfirm, Skeleton } from "antd";
import { ArrowRightOutlined, DeleteOutlined, MinusOutlined, PlusOutlined, ShoppingCartOutlined } from "@ant-design/icons";
import { formatPrice } from "../utils/formatter.js";

export default function CartPage({ cart, loading, user, onUpdateQuantity, onRemove, onSubmitOrder }) {
  const pendingRef = useRef(new Set());
  const [pendingBookIds, setPendingBookIds] = useState(new Set());

  async function changeQuantity(item, value) {
    if (value == null || pendingRef.current.has(item.bookId)) return;
    // ref 在同一事件周期同步加锁；状态负责在完整接口刷新期间禁用控件。
    pendingRef.current.add(item.bookId);
    setPendingBookIds(new Set(pendingRef.current));
    try {
      await onUpdateQuantity(item.id, value);
    } finally {
      pendingRef.current.delete(item.bookId);
      setPendingBookIds(new Set(pendingRef.current));
    }
  }

  if (!user) {
    return <div className="state-panel"><Alert type="warning" showIcon message="尚未登录" description="登录后可同步并保留购物车，随时继续选书。" action={<Link to="/profile" className="commerce-login-link">去登录 <ArrowRightOutlined /></Link>} /></div>;
  }
  if (loading) {
    return <div className="state-panel" role="status" aria-label="正在加载购物车"><Skeleton active paragraph={{ rows: 8 }} /></div>;
  }
  // 展示金额使用后端返回的单价与数量；真实下单仍由原来的回调交给后端计算。
  const subtotal = cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);
  const shipping = subtotal > 0 && subtotal < 99 ? 12 : 0;
  const total = subtotal + shipping;
  const count = cart.reduce((sum, item) => sum + Number(item.quantity), 0);

  if (!cart.length) {
    return <section className="state-panel cart-empty"><ShoppingCartOutlined aria-hidden="true" /><h1>购物车是空的</h1><p className="page-description">去书架挑一本，给自己留一点阅读时间。</p><Link className="commerce-primary-link" to="/books">浏览图书 <ArrowRightOutlined /></Link></section>;
  }

  return (
    <div className="page cart-page">
      <div className="page-heading"><div><p className="eyebrow">把喜欢的书，带回日常</p><h1>你的书袋</h1><p className="page-description">已选择 {count} 件商品，确认数量后即可提交订单。</p></div><Link className="commerce-secondary-link" to="/books">继续选书 <ArrowRightOutlined /></Link></div>
      <div className="cart-layout">
        <section className="cart-products" aria-labelledby="cart-products-title">
          <h2 id="cart-products-title">已选图书 <span>{count} 件</span></h2>
          <div className="cart-list-heading" aria-hidden="true"><span>商品信息</span><span>单价</span><span>数量</span><span>库存</span><span>小计</span><span>操作</span></div>
          <div className="cart-product-list">
            {cart.map((item) => {
              const pending = pendingBookIds.has(item.bookId);
              return (
              <article className="cart-product" key={item.id} aria-label={`购物车中的《${item.title}》`} aria-busy={pending}>
                <div className="cart-product-book"><Link to={`/books/${item.bookId}`} className="cart-product-cover" aria-label={`查看《${item.title}》详情`}><img src={item.image} alt={`《${item.title}》完整书封`} width="60" height="86" /></Link><div><Link className="cart-product-title" to={`/books/${item.bookId}`}>{item.title}</Link><p>图书 · {item.stock == null ? "库存信息暂不可用" : item.stock <= 0 ? "暂时缺货" : `库存 ${item.stock} 本`}</p></div></div>
                <div className="cart-unit-price"><span className="cart-mobile-label">单价</span>{formatPrice(item.price)}</div>
                <div className="cart-quantity-cell"><span className="cart-mobile-label">数量</span><div className="cart-quantity-control" role="group" aria-label={`《${item.title}》数量`}>
                  <Button type="text" icon={<MinusOutlined />} disabled={pending || item.quantity <= 1} aria-label={`减少《${item.title}》数量`} onClick={() => changeQuantity(item, Number(item.quantity) - 1)} />
                  <InputNumber min={1} max={item.stock ?? 99} value={item.quantity} controls={false} disabled={pending} aria-label={`《${item.title}》数量`} onChange={(value) => changeQuantity(item, value)} />
                  <Button type="text" icon={<PlusOutlined />} disabled={pending || item.quantity >= (item.stock ?? 99)} loading={pending} aria-label={`增加《${item.title}》数量`} onClick={() => changeQuantity(item, Number(item.quantity) + 1)} />
                </div></div>
                <div className="cart-product-stock"><span className="cart-mobile-label">库存</span>{item.stock == null ? "—" : `${item.stock} 本`}</div>
                <div className="cart-product-subtotal"><span className="cart-mobile-label">小计</span>{formatPrice(item.price * item.quantity)}</div>
                <div className="cart-remove"><Popconfirm title="确定要移除该商品吗？" disabled={pending} onConfirm={() => { if (!pendingRef.current.has(item.bookId)) return onRemove(item.id); }} okText="确定" cancelText="取消"><Button type="text" icon={<DeleteOutlined />} disabled={pending} aria-label={`移除《${item.title}》`}>移除</Button></Popconfirm></div>
              </article>
              );
            })}
          </div>
        </section>
        <aside className="paper-panel cart-summary" aria-labelledby="cart-summary-title">
          <h2 id="cart-summary-title">结算明细</h2>
          <dl><div><dt>商品合计 <span>{count} 件</span></dt><dd>{formatPrice(subtotal)}</dd></div><div><dt>运费 {shipping === 0 && <span>满 99 包邮</span>}</dt><dd>{formatPrice(shipping)}</dd></div><div className="cart-summary-total"><dt>应付总额</dt><dd>{formatPrice(total)}</dd></div></dl>
          <Button type="primary" size="large" block disabled={pendingBookIds.size > 0} onClick={onSubmitOrder}>提交订单 <ArrowRightOutlined /></Button>
          <p>商品满 ¥99 免运费，未满 ¥99 运费 ¥12。</p>
        </aside>
      </div>
    </div>
  );
}
