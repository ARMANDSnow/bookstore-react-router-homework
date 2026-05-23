import { useMemo, useState } from "react";
import { Row, Col, Typography, Tabs, Carousel, Button, Empty, Skeleton } from "antd";
import BookCard from "../components/BookCard.jsx";

const { Title, Paragraph } = Typography;

export default function BookListPage({ books, loading, onBookSelect, onAddToCart }) {
  const [activeCategory, setActiveCategory] = useState("all");
  const featuredBook = books.find((book) => book.id === "three-body") || books[0];
  
  const visibleBooks = useMemo(
    () =>
      activeCategory === "all"
        ? books
        : books.filter((book) => book.category === activeCategory),
    [activeCategory, books]
  );

  const categories = useMemo(() => {
    const categoryMap = new Map();
    books.forEach((book) => {
      if (!categoryMap.has(book.category)) {
        categoryMap.set(book.category, book.categoryName);
      }
    });

    return [
      { key: "all", label: "全部书籍" },
      ...Array.from(categoryMap, ([key, label]) => ({ key, label })),
    ];
  }, [books]);

  if (loading) {
    return <Skeleton active paragraph={{ rows: 12 }} />;
  }

  if (!books.length) {
    return <Empty description="暂无书籍数据" />;
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 32 }}>
      {/* Hero Banner Area using Carousel */}
      <Carousel autoplay effect="fade" className="hero-carousel" style={{ borderRadius: 8, overflow: "hidden" }}>
        <div style={{ backgroundColor: "#001529", height: 300, display: "flex" }}>
          <Row align="middle" style={{ height: "100%", width: "100%", padding: "0 48px", background: "linear-gradient(110deg, #20362f 0%, #254f49 52%, #b7791f 100%)" }}>
            <Col span={16}>
              <Title level={1} style={{ color: "#fff", margin: 0 }}>{featuredBook.title}</Title>
              <Paragraph style={{ color: "rgba(255,255,255,0.8)", fontSize: 16, marginTop: 16, maxWidth: 600 }}>
                {featuredBook.summary}
              </Paragraph>
              <Button type="primary" size="large" onClick={() => onBookSelect(featuredBook)}>
                立即了解
              </Button>
            </Col>
            <Col span={8} style={{ textAlign: "center" }}>
              <img 
                src={featuredBook.image} 
                alt={featuredBook.title} 
                style={{ maxHeight: 240, boxShadow: "0 10px 30px rgba(0,0,0,0.5)", borderRadius: 4 }} 
              />
            </Col>
          </Row>
        </div>
      </Carousel>

      {/* Category Tabs */}
      <Tabs
        className="category-tabs"
        activeKey={activeCategory}
        onChange={setActiveCategory}
        items={categories}
        size="large"
        centered
      />

      {/* Book Grid */}
      {visibleBooks.length > 0 ? (
        <Row gutter={[24, 24]}>
          {visibleBooks.map((book) => (
            <Col xs={24} sm={12} md={8} lg={6} key={book.id}>
              <BookCard
                book={book}
                onBookSelect={onBookSelect}
                onAddToCart={onAddToCart}
              />
            </Col>
          ))}
        </Row>
      ) : (
        <Empty description="当前分类暂无书籍" />
      )}
    </div>
  );
}
