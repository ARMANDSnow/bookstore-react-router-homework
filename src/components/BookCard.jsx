export default function BookCard({ book, onBookSelect, onAddToCart }) {
  return (
    <article className="book-card">
      <button
        className="book-card-hitarea"
        type="button"
        onClick={() => onBookSelect(book)}
        aria-label={`查看${book.title}详情`}
      >
        <figure className="book-cover">
          <img src={book.image} alt={`${book.title}封面`} />
        </figure>
        <div className="book-meta">
          <p className="book-tag">{book.categoryName}</p>
          <h3>{book.title}</h3>
          <p>{book.author}</p>
          <p className="book-desc">{book.description}</p>
        </div>
      </button>
      <footer className="book-footer">
        <div>
          <strong className="book-price">¥{book.price.toFixed(2)}</strong>
          <span className="book-rating">{book.rating}</span>
        </div>
        <div className="card-actions">
          <button className="text-link" type="button" onClick={() => onBookSelect(book)}>
            查看详情
          </button>
          <button className="button button-mini button-primary" type="button" onClick={() => onAddToCart(book)}>
            加入购物车
          </button>
        </div>
      </footer>
    </article>
  );
}
