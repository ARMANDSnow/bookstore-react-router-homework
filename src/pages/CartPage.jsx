import { Link } from "react-router-dom";

export default function CartPage({ cart, onUpdateQuantity, onRemove, onSubmitOrder }) {
  const count = cart.reduce((sum, item) => sum + item.quantity, 0);
  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const shipping = count > 0 && subtotal < 99 ? 12 : 0;
  const total = subtotal + shipping;

  return (
    <section className="cart-layout">
      <article className="cart-panel">
        <header className="section-header compact">
          <p className="eyebrow">Cart</p>
          <h1>购物车</h1>
          <p>可以调整数量、删除图书，也可以继续回到列表挑选。</p>
        </header>
        <div className="cart-list">
          {cart.length === 0 ? (
            <div className="empty-state">
              <p>购物车还是空的，先去书籍列表页挑选喜欢的图书。</p>
              <Link className="button button-primary" to="/books">
                去选书
              </Link>
            </div>
          ) : (
            cart.map((item) => (
              <article className="cart-item" key={item.id}>
                <div className="cart-item-meta">
                  <h3>{item.title}</h3>
                  <p>单价：¥{item.price.toFixed(2)}</p>
                  <p>小计：¥{(item.price * item.quantity).toFixed(2)}</p>
                </div>
                <div className="cart-controls">
                  <button className="qty-button" type="button" onClick={() => onUpdateQuantity(item.id, -1)}>
                    -
                  </button>
                  <span className="qty-value">{item.quantity}</span>
                  <button className="qty-button" type="button" onClick={() => onUpdateQuantity(item.id, 1)}>
                    +
                  </button>
                  <button className="button button-secondary button-mini" type="button" onClick={() => onRemove(item.id)}>
                    删除
                  </button>
                </div>
              </article>
            ))
          )}
        </div>
      </article>
      <aside className="summary-panel">
        <header className="section-header compact">
          <p className="eyebrow">Summary</p>
          <h2>订单摘要</h2>
        </header>
        <div className="summary-list">
          <p>
            <span>商品数量</span>
            <strong>{count} 件</strong>
          </p>
          <p>
            <span>商品金额</span>
            <strong>¥{subtotal.toFixed(2)}</strong>
          </p>
          <p>
            <span>运费</span>
            <strong>¥{shipping.toFixed(2)}</strong>
          </p>
          <p className="summary-total">
            <span>应付合计</span>
            <strong>¥{total.toFixed(2)}</strong>
          </p>
        </div>
        <div className="summary-actions">
          <button className="button button-primary" type="button" onClick={onSubmitOrder}>
            提交订单
          </button>
          <Link className="button button-secondary" to="/books">
            继续选书
          </Link>
        </div>
      </aside>
    </section>
  );
}
