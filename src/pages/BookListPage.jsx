import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { Alert, Button, Empty, Pagination, Select, Skeleton } from "antd";
import BookCard from "../components/BookCard.jsx";
import { fetchCatalog } from "../api/bookstoreApi.js";

function BookSkeletons() {
  return Array.from({ length: 4 }, (_, index) => (
    <div className="book-skeleton" key={index}>
      <Skeleton.Image active />
      <Skeleton active paragraph={{ rows: 3 }} />
    </div>
  ));
}

function positiveInteger(value, fallback) {
  const number = Number(value);
  return Number.isSafeInteger(number) && number > 0 ? number : fallback;
}

export default function BookListPage({ books, loading, onBookSelect, onAddToCart }) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = (searchParams.get("keyword") || "").trim();
  const activeCategory = searchParams.get("category") || "all";
  const sort = ["recommended", "price-low", "price-high"].includes(searchParams.get("sort")) ? searchParams.get("sort") : "recommended";
  const page = positiveInteger(searchParams.get("page"), 1);
  const size = Math.min(100, positiveInteger(searchParams.get("size"), 4));
  const [retry, setRetry] = useState(0);
  const requestKey = `${location.key}:${keyword}:${activeCategory}:${sort}:${page}:${size}:${retry}`;
  const [catalog, setCatalog] = useState(null);
  const catalogTop = useRef(null);
  const current = catalog?.requestKey === requestKey;
  const searching = loading || !current || catalog.status === "pending";
  const result = current && catalog.status === "ready" ? catalog.result : null;
  const paginationInfo = result || (searching ? catalog?.pagination : null);
  const error = current && catalog.status === "error" ? catalog.error : null;

  useEffect(() => {
    const controller = new AbortController();
    if (loading) return () => controller.abort();
    setCatalog((previous) => ({ requestKey, status: "pending", pagination: previous?.result || previous?.pagination }));
    fetchCatalog({ page, size, category: activeCategory === "all" ? undefined : activeCategory, keyword, sort }, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setCatalog({ requestKey, status: "ready", result: data });
      })
      .catch((err) => {
        if (!controller.signal.aborted) setCatalog({ requestKey, status: "error", error: err.message || "暂时无法加载书架" });
      });
    return () => controller.abort();
  }, [requestKey, loading, page, size, activeCategory, keyword, sort]);

  const categories = useMemo(() => {
    const categoryMap = new Map();
    books.forEach((book) => {
      if (!categoryMap.has(book.category)) categoryMap.set(book.category, { key: book.category, label: book.categoryName || book.category, count: 0 });
      categoryMap.get(book.category).count += 1;
    });
    return [{ key: "all", label: "全部书籍", count: books.length }, ...categoryMap.values()];
  }, [books]);

  const featuredBooks = [books.find((book) => book.id === "three-body") || books[0], books.find((book) => book.id === "design") || books[1]]
    .filter((book, index, selected) => book && selected.findIndex((item) => item?.id === book.id) === index);
  const heading = keyword ? "搜索结果" : activeCategory === "all" ? "精选书架" : categories.find((category) => category.key === activeCategory)?.label || "分类书架";
  const showHero = !keyword && activeCategory === "all" && page === 1 && featuredBooks.length > 0;
  const CatalogHeading = showHero ? "h2" : "h1";

  function updateFilters(changes, scroll = false) {
    const next = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => {
      if (value === "all" || value === "recommended" || value === undefined || value === 1) next.delete(key);
      else next.set(key, String(value));
    });
    setSearchParams(next);
    if (scroll) catalogTop.current?.scrollIntoView({ behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
  }

  if (loading) return <div className="catalog-loading" role="status" aria-label="正在加载图书"><Skeleton active paragraph={{ rows: 3 }} /><div className="book-grid"><BookSkeletons /></div></div>;

  return (
    <div className="catalog-page">
      {showHero && (
        <section className="editor-pick" aria-label="本期选书">
          <div><p className="eyebrow">本期选书 · 思考的边界</p><h1>从一个好问题，<br />读到更大的世界。</h1><p className="editor-copy">从想象远方，到理解日常。<br />精选 {books.length} 本好书，陪你认真读。</p></div>
          <div className="hero-books">{featuredBooks.map((book) => <figure className="feature-book" key={book.id}><button className="feature-cover" type="button" onClick={() => onBookSelect(book)} aria-label={`查看本期选书《${book.title}》`}><img referrerPolicy="no-referrer" src={book.image} alt={book.title} width={101} height={148} /></button><figcaption>{book.title}</figcaption></figure>)}</div>
        </section>
      )}
      <div className="catalog-categories" role="group" aria-label="图书分类">
        {categories.map((category) => <button key={category.key} type="button" className={`category-button${activeCategory === category.key ? " active" : ""}`} aria-pressed={activeCategory === category.key} onClick={() => updateFilters({ category: category.key, page: 1 })}>{category.label}<span className="category-count">{category.count}</span></button>)}
      </div>
      <section aria-labelledby="catalog-title" aria-busy={searching} ref={catalogTop} className="catalog-results">
        <div className="catalog-toolbar">
          <div className="catalog-title-wrap"><CatalogHeading id="catalog-title">{heading}</CatalogHeading><span className="result-count" role="status">{searching ? "正在整理书架…" : error ? "书架加载失败" : `共 ${result?.totalItems ?? 0} 本图书`}</span></div>
          <div className="sort-field"><label htmlFor="book-sort">排序</label><Select id="book-sort" aria-label="图书排序" value={sort} onChange={(value) => updateFilters({ sort: value, page: 1 })} options={[{ value: "recommended", label: "默认推荐" }, { value: "price-low", label: "价格从低到高" }, { value: "price-high", label: "价格从高到低" }]} /></div>
        </div>
        <p className="catalog-demo-note">一本一本，慢慢挑选。</p>
        {(keyword || activeCategory !== "all") && <div className="search-summary"><span className="result-count">{keyword ? `搜索“${keyword}”` : "已按分类筛选"}{keyword && activeCategory !== "all" && " · 已叠加分类筛选"}</span><Button type="link" size="small" onClick={() => setSearchParams({})}>清除筛选</Button></div>}
        {error ? <Alert type="error" showIcon title="书架暂时无法加载" description={error} action={<Button onClick={() => setRetry((value) => value + 1)}>重新加载</Button>} /> : <div className="book-grid">
          {searching ? <BookSkeletons /> : result?.items.length ? result.items.map((book) => <BookCard key={book.id} book={book} onBookSelect={onBookSelect} onAddToCart={onAddToCart} />) : <section className="state-panel catalog-empty"><Empty description={page > 1 ? "这一页暂时没有图书" : keyword ? "没有搜到相关书籍" : "当前分类暂无书籍"} /><Button onClick={() => page > 1 ? updateFilters({ page: 1 }) : setSearchParams({})}>{page > 1 ? "回到第一页" : "查看全部图书"}</Button></section>}
        </div>}
        {paginationInfo?.totalItems > 0 && <div className="catalog-pagination" aria-label="书架分页"><span className="result-count">第 {page} 页 · 每页 {size} 本</span><Pagination disabled={searching} current={page} pageSize={size} total={paginationInfo.totalItems} showSizeChanger pageSizeOptions={[4, 8, 12]} responsive onChange={(nextPage, nextSize) => updateFilters({ page: nextSize === size ? nextPage : 1, size: nextSize }, true)} /></div>}
      </section>
    </div>
  );
}
