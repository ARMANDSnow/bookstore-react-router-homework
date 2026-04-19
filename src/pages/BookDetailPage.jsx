import { Link, useLocation, useParams } from "react-router-dom";
import BookCard from "../components/BookCard.jsx";

export default function BookDetailPage({ books, selectedBook, onBookSelect, onAddToCart }) {
  const { bookId } = useParams();
  const { state } = useLocation();
  const book =
    state?.book ||
    (selectedBook?.id === bookId ? selectedBook : null) ||
    books.find((item) => item.id === bookId);

  if (!book) {
    return (
      <section className="empty-state">
        <h1>没有找到这本书</h1>
        <p>请返回书籍列表重新选择。</p>
        <Link className="button button-primary" to="/books">
          返回列表
        </Link>
      </section>
    );
  }

  const relatedBooks = books.filter((item) => item.category === book.category && item.id !== book.id).slice(0, 3);

  return (
    <>
      <nav className="breadcrumb" aria-label="面包屑">
        <Link to="/books">首页</Link>
        <span>/</span>
        <span>{book.title}</span>
      </nav>
      <section className="detail-layout">
        <figure className="detail-cover">
          <img src={book.image} alt={`${book.title}封面`} />
        </figure>
        <article className="detail-content">
          <header className="section-header compact">
            <p className="eyebrow">Book Detail</p>
            <h1>{book.title}</h1>
            <p>{book.author} 著</p>
          </header>
          <div className="price-box">
            <strong>¥{book.price.toFixed(2)}</strong>
            <span>原价 ¥{book.originalPrice.toFixed(2)}</span>
            <mark>{book.badge}</mark>
          </div>
          <p className="detail-summary">{book.summary}</p>
          <ul className="feature-list">
            <li>分类：{book.categoryLabel}</li>
            <li>评分：{book.rating}</li>
            <li>库存：现货充足</li>
            <li>配送：满 99 元包邮</li>
          </ul>
          <div className="detail-actions">
            <button className="button button-primary" type="button" onClick={() => onAddToCart(book)}>
              加入购物车
            </button>
            <Link className="button button-secondary" to="/cart">
              立即结算
            </Link>
            <Link className="text-link" to="/books">
              返回列表
            </Link>
          </div>
        </article>
      </section>
      <section className="detail-sections">
        <article className="info-card">
          <header className="section-header compact">
            <h2>内容亮点</h2>
          </header>
          <p>{book.highlight}</p>
        </article>
        <article className="info-card">
          <header className="section-header compact">
            <h2>适合人群</h2>
          </header>
          <p>{book.audience}</p>
        </article>
        <article className="info-card">
          <header className="section-header compact">
            <h2>读者评价</h2>
          </header>
          <p>{book.review}</p>
        </article>
      </section>
      {relatedBooks.length > 0 && (
        <section className="related-section">
          <header className="section-header compact">
            <p className="eyebrow">Related</p>
            <h2>同类推荐</h2>
          </header>
          <div className="books-grid related-grid">
            {relatedBooks.map((item) => (
              <BookCard key={item.id} book={item} onBookSelect={onBookSelect} onAddToCart={onAddToCart} />
            ))}
          </div>
        </section>
      )}
    </>
  );
}
