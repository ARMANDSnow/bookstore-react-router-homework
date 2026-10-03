import { useRef, useState } from "react";
import { Button } from "antd";
import { ShoppingCartOutlined } from "@ant-design/icons";

// 卡片只展示书籍与触发回调，登录、购物车及后端请求仍由 App 负责。
export default function BookCard({ book, onBookSelect, onAddToCart }) {
  const [adding, setAdding] = useState(false);
  const addingRef = useRef(false);
  const stock = book.stock;
  const soldOut = stock !== undefined && stock !== null && stock <= 0;
  const price = Number(book.price || 0);
  const originalPrice = Number(book.originalPrice || 0);
  const titleId = `book-title-${book.id}`;

  async function handleAddToCart() {
    if (soldOut || addingRef.current) return;
    addingRef.current = true;
    setAdding(true);
    try {
      await onAddToCart(book);
    } finally {
      addingRef.current = false;
      setAdding(false);
    }
  }

  return (
    <article className="book-card" aria-labelledby={titleId}>
      <button
        type="button"
        className="cover-link"
        onClick={() => onBookSelect(book)}
        aria-label={`查看《${book.title}》详情`}
      >
        <img src={book.image} alt={book.title} width={150} height={220} loading="lazy" />
      </button>
      <div className="book-info">
        <div className="book-kicker">
          <span>{book.categoryName || book.category}</span>
          {book.badge && <><span className="tiny-line" aria-hidden="true" /><span>{book.badge}</span></>}
        </div>
        <h3 className="book-title" id={titleId}>
          <button type="button" className="title-button" onClick={() => onBookSelect(book)}>{book.title}</button>
        </h3>
        <p className="book-author">{book.author} 著</p>
        <p className="book-description">{book.description || book.summary}</p>
        <div className="book-meta-row">
          <span className={`book-stock${soldOut ? " sold-out" : ""}`}>
            {soldOut ? "暂时缺货" : stock == null ? "现货充足" : `库存 ${stock} 本`}
          </span>
          <Button type="link" className="book-detail-link" onClick={() => onBookSelect(book)}>查看详情</Button>
        </div>
        <div className="book-buy">
          <div className="book-price">
            <span className="price-text"><span className="currency">¥</span>{price.toFixed(2)}</span>
            {originalPrice > price && <span className="original-price" aria-label={`原价 ${originalPrice.toFixed(2)} 元`}>¥{originalPrice.toFixed(2)}</span>}
          </div>
          <Button
            className="add-button"
            icon={<ShoppingCartOutlined />}
            onClick={handleAddToCart}
            loading={adding}
            disabled={soldOut || adding}
          >
            {soldOut ? "缺货" : "加入购物车"}
          </Button>
        </div>
      </div>
    </article>
  );
}
