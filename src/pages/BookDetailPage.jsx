import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { Alert, Breadcrumb, Button, Image, Skeleton, Tag } from "antd";
import { LeftOutlined, ShoppingCartOutlined } from "@ant-design/icons";
import BookCard from "../components/BookCard.jsx";
import { fetchBookById } from "../api/bookstoreApi.js";
import { formatPrice } from "../utils/formatter.js";

export default function BookDetailPage({ books, loading, selectedBook, onBookSelect, onAddToCart }) {
  const { bookId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const [remoteBook, setRemoteBook] = useState(null);
  const [detailLoading, setDetailLoading] = useState(true);
  const [detailError, setDetailError] = useState(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    // 路由切换后忽略旧请求，避免较晚返回的数据覆盖当前书籍。
    let ignore = false;
    setRemoteBook(null);
    setDetailError(null);
    setDetailLoading(true);
    (async () => {
      try {
        const data = await fetchBookById(bookId);
        if (!ignore) setRemoteBook(data);
      } catch (error) {
        if (!ignore) setDetailError({ bookId, message: error.message, status: error.status });
      } finally {
        if (!ignore) setDetailLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [bookId, retry]);

  const localBook =
    (state?.book?.id === bookId ? state.book : null) ||
    (selectedBook?.id === bookId ? selectedBook : null) ||
    books.find((item) => item.id === bookId);
  const error = detailError?.bookId === bookId ? detailError : null;
  const currentRemote = remoteBook?.id === bookId ? remoteBook : null;
  const book = error?.status === 404 ? null : currentRemote || localBook;

  if (loading || (detailLoading && !book)) {
    return <div className="state-panel" role="status" aria-label="正在加载图书详情"><Skeleton active paragraph={{ rows: 10 }} /></div>;
  }

  if (!book) {
    return (
      <section className="state-panel">
        <h1>{error && error.status !== 404 ? "图书详情暂时无法加载" : "没有找到这本书"}</h1>
        <p className="page-description">{error && error.status !== 404 ? "请稍后重试，或回到书架继续选书。" : "回到书架，看看其他值得阅读的书。"}</p>
        {error && error.status !== 404 && <Button onClick={() => setRetry((value) => value + 1)}>重新加载</Button>}
        <Button type="primary" onClick={() => navigate("/books")}>返回图书目录</Button>
      </section>
    );
  }

  const relatedBooks = books.filter((item) => item.category === book.category && item.id !== book.id).slice(0, 4);
  const stock = book.stock;
  const soldOut = stock !== undefined && stock !== null && stock <= 0;

  return (
    <div className="page detail-page">
      <Breadcrumb items={[
        { title: <Link to="/books">图书目录</Link> },
        { title: "书籍详情" },
        { title: book.title },
      ]} />
      {error && <Alert showIcon type="warning" title="当前展示上次加载的信息，库存暂未更新" description="连接恢复后重新加载，再加入购物车。" action={<Button onClick={() => setRetry((value) => value + 1)}>重新加载</Button>} />}
      <section className="detail-layout" aria-labelledby="detail-title">
        <div className="detail-cover">
          <Image className="detail-cover-image" src={book.image} alt={`《${book.title}》完整书封`} />
          <p>{book.publisher || "精选图书"}</p>
        </div>
        <div className="detail-info">
          <p className="eyebrow">{book.categoryName || book.categoryLabel}</p>
          <h1 id="detail-title">{book.title}</h1>
          <p className="detail-author">{book.author} 著</p>
          <div className="detail-price-row">
            <span className="price-text detail-price">{formatPrice(book.price)}</span>
            {book.originalPrice != null && <span className="original-price">定价 {formatPrice(book.originalPrice)}</span>}
            {book.badge && <Tag className="detail-badge">{book.badge}</Tag>}
          </div>
          <p className="detail-summary">{book.summary}</p>
          <dl className="detail-metadata">
            <div><dt>分类</dt><dd>{book.categoryLabel}</dd></div>
            <div><dt>ISBN</dt><dd>{book.isbn || "—"}</dd></div>
            <div><dt>出版社</dt><dd>{book.publisher || "—"}</dd></div>
            <div><dt>豆瓣评分</dt><dd>{book.rating || "—"}</dd></div>
            <div><dt>库存状态</dt><dd><Tag className={soldOut ? "detail-stock detail-stock-empty" : "detail-stock"}>{soldOut ? "暂时缺货" : stock == null ? "现货充足" : `库存 ${stock} 本`}</Tag></dd></div>
            <div><dt>配送服务</dt><dd>满 99 元包邮</dd></div>
          </dl>
          <div className="page-actions detail-actions">
            <Button type="primary" size="large" icon={<ShoppingCartOutlined />} onClick={() => onAddToCart(book)} disabled={soldOut || !currentRemote} loading={detailLoading}>{soldOut ? "暂时缺货" : "加入购物车"}</Button>
            <Button size="large" onClick={() => navigate("/cart")}>立即结算</Button>
            <Button type="link" icon={<LeftOutlined />} onClick={() => navigate("/books")}>返回图书目录</Button>
          </div>
        </div>
      </section>
      <div className="detail-notes">
        <section><h2>内容亮点</h2><p>{book.highlight}</p></section>
        <section><h2>适合人群</h2><p>{book.audience}</p></section>
        <section><h2>读者评价</h2><p>{book.review}</p></section>
      </div>
      {relatedBooks.length > 0 && (
        <section className="detail-related" aria-labelledby="related-title">
          <div className="page-heading"><h2 id="related-title">同类推荐</h2></div>
          <div className="detail-related-grid">{relatedBooks.map((item) => <BookCard key={item.id} book={item} onBookSelect={onBookSelect} onAddToCart={onAddToCart} />)}</div>
        </section>
      )}
    </div>
  );
}
