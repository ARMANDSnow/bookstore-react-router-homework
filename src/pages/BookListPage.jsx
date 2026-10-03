import { useMemo, useState } from "react";
import { Row, Col, Typography, Tabs, Carousel, Button, Empty, Skeleton, Input, message } from "antd";
import BookCard from "../components/BookCard.jsx";
import { searchBooks } from "../api/bookstoreApi.js";

const { Title, Paragraph, Text } = Typography;

export default function BookListPage({ books, loading, onBookSelect, onAddToCart }) {
  const [activeCategory, setActiveCategory] = useState("all");

  // ---------------------- 迭代三：关键字搜索状态 ----------------------
  // keyword       当前"已生效"的关键字（用于展示结果条数，不是输入框的实时值）
  // searchResults null 表示"非搜索态"（区别于 [] ——"搜了但没结果"），两种态渲染不同
  // searching     搜索请求进行中，驱动搜索按钮的 loading 动画
  const [keyword, setKeyword] = useState("");
  const [searchResults, setSearchResults] = useState(null);
  const [searching, setSearching] = useState(false);

  const featuredBook = books.find((book) => book.id === "three-body") || books[0];

  // 搜索与分类过滤的叠加（"与"关系）：
  //   - 搜索：走后端 SQL（LIKE '%kw%'），由数据库完成过滤 —— 适合数据量大、条件复杂的场景
  //   - 分类：在前端内存里 filter —— 数据已在手上，切 Tab 零网络开销
  // 两种"过滤位置"的取舍正好是前后端分工的教学对照点
  const baseBooks = searchResults ?? books;
  const visibleBooks = useMemo(
    () =>
      activeCategory === "all"
        ? baseBooks
        : baseBooks.filter((book) => book.category === activeCategory),
    [activeCategory, baseBooks]
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

  // 提交搜索（回车 / 点搜索按钮 / 点清空按钮时触发）
  async function handleSearch(value) {
    const kw = (value || "").trim();
    if (!kw) {
      // 空关键字 = 退出搜索态，恢复全量列表（allowClear 清空时也走这里）
      setKeyword("");
      setSearchResults(null);
      return;
    }
    setSearching(true);
    try {
      const result = await searchBooks(kw); // 后端 SQL 模糊搜索
      setKeyword(kw);
      setSearchResults(result);
    } catch {
      // 后端不可用时降级为前端内存过滤（与全站"后端挂了退本地"的策略一致），保证可演示
      setKeyword(kw);
      setSearchResults(
        books.filter(
          (book) => book.title.includes(kw) || book.author.includes(kw)
        )
      );
      message.warning("后端搜索暂不可用，已使用本地过滤结果。");
    } finally {
      setSearching(false);
    }
  }

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

      {/* 迭代三：关键字搜索框（标题/作者，后端 LIKE 模糊匹配） */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
        <Input.Search
          placeholder="搜索书名 / 作者"
          allowClear
          enterButton="搜索"
          size="large"
          loading={searching}
          onSearch={handleSearch}
          style={{ maxWidth: 420 }}
        />
        {searchResults !== null && (
          <Text type="secondary">
            搜索 “{keyword}”：共 {searchResults.length} 本
            {activeCategory !== "all" && "（当前分类下再筛选）"}
          </Text>
        )}
      </div>

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
        <Empty description={searchResults !== null ? "没有搜到相关书籍" : "当前分类暂无书籍"} />
      )}
    </div>
  );
}
