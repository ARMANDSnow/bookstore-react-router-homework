import { useEffect, useMemo, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { Button, Empty, Select, Skeleton, message } from "antd";
import BookCard from "../components/BookCard.jsx";
import { searchBooks } from "../api/bookstoreApi.js";

function BookSkeletons() {
  return Array.from({ length: 4 }, (_, index) => (
    <div className="book-skeleton" key={index}>
      <Skeleton.Image active />
      <Skeleton active paragraph={{ rows: 3 }} />
    </div>
  ));
}

export default function BookListPage({ books, loading, onBookSelect, onAddToCart }) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = (searchParams.get("keyword") || "").trim();
  const requestKey = `${location.key}:${keyword}`;
  const [activeCategory, setActiveCategory] = useState("all");
  const [sort, setSort] = useState("recommended");
  const [searchState, setSearchState] = useState(null);
  const currentResult = searchState?.requestKey === requestKey && searchState?.sourceBooks === books;
  const searching = Boolean(keyword) && (!currentResult || searchState.status === "pending");
  const searchResults = keyword && currentResult && searchState.status === "ready" ? searchState.results : null;

  // URL 记录关键字；导航 key 让同词再次提交也重试。只展示当前请求的结果。
  useEffect(() => {
    let active = true;
    if (!keyword) {
      setSearchState(null);
      return () => { active = false; };
    }
    if (loading) return () => { active = false; };
    setSearchState({ requestKey, sourceBooks: books, status: "pending", results: [] });
    (async () => {
      try {
        const result = await searchBooks(keyword);
        if (active) setSearchState({ requestKey, sourceBooks: books, status: "ready", results: result });
      } catch {
        if (!active) return;
        const query = keyword.toLowerCase();
        const results = books.filter((book) =>
          `${book.title || ""} ${book.author || ""}`.toLowerCase().includes(query)
        );
        setSearchState({ requestKey, sourceBooks: books, status: "ready", results });
        message.warning("后端搜索暂不可用，已使用本地过滤结果。");
      }
    })();
    return () => { active = false; };
  }, [keyword, books, loading, requestKey]);

  const categories = useMemo(() => {
    const categoryMap = new Map();
    books.forEach((book) => {
      if (!categoryMap.has(book.category)) {
        categoryMap.set(book.category, { key: book.category, label: book.categoryName || book.category, count: 0 });
      }
      categoryMap.get(book.category).count += 1;
    });
    return [{ key: "all", label: "全部书籍", count: books.length }, ...categoryMap.values()];
  }, [books]);

  const visibleBooks = useMemo(() => {
    const baseBooks = keyword ? searchResults ?? [] : books;
    const filtered = activeCategory === "all" ? baseBooks : baseBooks.filter((book) => book.category === activeCategory);
    if (sort === "recommended") return filtered;
    return [...filtered].sort((first, second) => {
      const difference = Number(first.price) - Number(second.price);
      return sort === "price-low" ? difference : -difference;
    });
  }, [activeCategory, books, keyword, searchResults, sort]);

  const featuredBooks = [
    books.find((book) => book.id === "three-body") || books[0],
    books.find((book) => book.id === "design") || books[1],
  ].filter((book, index, selected) => book && selected.findIndex((item) => item?.id === book.id) === index);
  const heading = keyword ? "搜索结果" : activeCategory === "all" ? "精选书架" : categories.find((category) => category.key === activeCategory)?.label || "精选书架";

  function resetFilters() {
    setActiveCategory("all");
    setSort("recommended");
    setSearchParams({});
  }

  if (loading) {
    return (
      <div className="catalog-loading" role="status" aria-label="正在加载图书">
        <Skeleton active paragraph={{ rows: 3 }} />
        <div className="book-grid">
          <BookSkeletons />
        </div>
      </div>
    );
  }

  if (!books.length) {
    return <section className="state-panel"><Empty description="暂无书籍数据" /></section>;
  }

  return (
    <div className="catalog-page">
      {!keyword && activeCategory === "all" && (
        <section className="editor-pick" aria-label="本期选书">
          <div>
            <p className="eyebrow">本期选书 · 思考的边界</p>
            <h1>从一个好问题，<br />读到更大的世界。</h1>
            <p className="editor-copy">从想象远方，到理解日常。<br />精选 {books.length} 本好书，陪你认真读。</p>
          </div>
          <div className="hero-books">
            {featuredBooks.map((book) => (
              <figure className="feature-book" key={book.id}>
                <button className="feature-cover" type="button" onClick={() => onBookSelect(book)} aria-label={`查看本期选书《${book.title}》`}>
                  <img src={book.image} alt={book.title} width={101} height={148} />
                </button>
                <figcaption>{book.title}</figcaption>
              </figure>
            ))}
          </div>
        </section>
      )}

      <div className="catalog-categories" role="group" aria-label="图书分类">
        {categories.map((category) => (
          <button
            key={category.key}
            type="button"
            className={`category-button${activeCategory === category.key ? " active" : ""}`}
            aria-pressed={activeCategory === category.key}
            onClick={() => setActiveCategory(category.key)}
          >
            {category.label}<span className="category-count">{category.count}</span>
          </button>
        ))}
      </div>

      <section aria-labelledby="catalog-title" aria-busy={searching}>
        <div className="catalog-toolbar">
          <div className="catalog-title-wrap">
            {keyword || activeCategory !== "all" ? <h1 id="catalog-title">{heading}</h1> : <h2 id="catalog-title">{heading}</h2>}
            <span className="result-count" role="status">{searching ? "正在搜索图书…" : `共 ${visibleBooks.length} 本图书`}</span>
          </div>
          <div className="sort-field">
            <label htmlFor="book-sort">排序</label>
            <Select
              id="book-sort"
              aria-label="图书排序"
              value={sort}
              onChange={setSort}
              options={[
                { value: "recommended", label: "默认推荐" },
                { value: "price-low", label: "价格从低到高" },
                { value: "price-high", label: "价格从高到低" },
              ]}
            />
          </div>
        </div>
        {keyword && <div className="search-summary"><span className="result-count">搜索“{keyword}”{activeCategory !== "all" && " · 已叠加分类筛选"}</span><Button type="link" size="small" onClick={resetFilters}>清除筛选</Button></div>}
        <div className="book-grid">
          {searching ? <BookSkeletons /> : visibleBooks.length ? visibleBooks.map((book) => (
            <BookCard key={book.id} book={book} onBookSelect={onBookSelect} onAddToCart={onAddToCart} />
          )) : (
            <section className="state-panel catalog-empty">
              <Empty description={keyword ? "没有搜到相关书籍" : "当前分类暂无书籍"} />
              <Button onClick={resetFilters}>查看全部图书</Button>
            </section>
          )}
        </div>
      </section>
    </div>
  );
}
