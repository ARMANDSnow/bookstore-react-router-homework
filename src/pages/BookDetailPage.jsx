import { useEffect, useState } from "react";
import { useLocation, useParams, useNavigate } from "react-router-dom";
import { Row, Col, Typography, Button, Descriptions, Breadcrumb, Divider, Image, Card, Tag, Space, Skeleton } from "antd";
import { ShoppingCartOutlined, PayCircleOutlined, LeftOutlined } from "@ant-design/icons";
import BookCard from "../components/BookCard.jsx";
import { fetchBookById } from "../api/bookstoreApi.js";

const { Title, Paragraph, Text } = Typography;

export default function BookDetailPage({ books, loading, selectedBook, onBookSelect, onAddToCart }) {
  const { bookId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();

  // ---------------- 迭代三：详情数据改为优先走后端接口 ----------------
  // remoteBook    GET /api/v1/book/{id} 返回的 BookDto（null = 还没拿到 / 拿不到）
  // detailLoading 详情请求进行中（与列表的 loading 区分开）
  const [remoteBook, setRemoteBook] = useState(null);
  const [detailLoading, setDetailLoading] = useState(true);

  useEffect(() => {
    // ignore 竞态保护（与 App.jsx 拉书籍列表的写法一致）：
    // 快速切换路由时，旧请求的响应晚到不应覆盖新页面的 state；
    // cleanup 里把 ignore 置 true，晚到的响应直接丢弃
    let ignore = false;
    setRemoteBook(null);
    setDetailLoading(true);
    (async () => {
      try {
        // 真实后端请求（DevTools Network 面板可见 GET /api/v1/book/{id}）
        const data = await fetchBookById(bookId);
        if (!ignore) setRemoteBook(data);
      } catch {
        // 拿不到远端数据（后端未启动 / 书不存在）时静默降级：
        // 下方 localBook 三级兜底继续工作，页面不至于白屏；
        // "后端不可用"的提示已由 App.jsx 拉列表失败时统一弹过，这里不重复打扰
      } finally {
        if (!ignore) setDetailLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [bookId]);

  // 本地三级兜底（迭代二原有逻辑）：路由 state 带过来的书 → 全局选中的书 → 列表里找
  const localBook =
    state?.book ||
    (selectedBook?.id === bookId ? selectedBook : null) ||
    books.find((item) => item.id === bookId);

  // 远端优先：优先展示数据库里的最新数据，本地数据只作后端不可用时的降级
  const book = remoteBook || localBook;

  // 列表还在加载、或详情请求未返回且本地也没有可先展示的数据 → 骨架屏
  if (loading || (detailLoading && !book)) {
    return <Skeleton active paragraph={{ rows: 10 }} />;
  }

  // 远端和本地都拿不到（如地址栏乱输的 id）→ 未找到
  if (!book) {
    return (
      <div style={{ textAlign: "center", padding: "100px 0" }}>
        <Title level={2}>没有找到这本书</Title>
        <Button type="primary" onClick={() => navigate("/books")}>
          返回列表
        </Button>
      </div>
    );
  }

  const relatedBooks = books.filter((item) => item.category === book.category && item.id !== book.id).slice(0, 4);
  const stock = book.stock;
  const soldOut = stock !== undefined && stock !== null && stock <= 0;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate("/books")}>首页</a> },
          { title: "书籍详情" },
          { title: book.title },
        ]}
      />

      <Card variant="borderless" className="detail-card" style={{ boxShadow: "0 2px 8px rgba(0,0,0,0.06)" }}>
        <Row gutter={[48, 24]}>
          <Col xs={24} md={8} style={{ textAlign: "center" }}>
            <Image
              src={book.image}
              alt={book.title}
              style={{ width: "100%", maxWidth: 300, boxShadow: "0 8px 24px rgba(0,0,0,0.15)", borderRadius: 8 }}
            />
          </Col>
          <Col xs={24} md={16}>
            <Title level={1} style={{ marginBottom: 8 }}>{book.title}</Title>
            <Text type="secondary" style={{ fontSize: 16 }}>{book.author} 著</Text>
            
            <div className="detail-price-panel" style={{ margin: "24px 0", padding: "16px 24px", background: "#fcf8e3", borderRadius: 8 }}>
              <Space align="baseline" size="large">
                <span className="price-text" style={{ fontSize: 28 }}>¥{book.price.toFixed(2)}</span>
                <span className="original-price" style={{ fontSize: 16 }}>定价: ¥{book.originalPrice.toFixed(2)}</span>
                {book.badge && <Tag color="red">{book.badge}</Tag>}
              </Space>
            </div>

            <Paragraph style={{ fontSize: 16, lineHeight: 1.8 }}>
              {book.summary}
            </Paragraph>

            <Descriptions column={2} style={{ marginTop: 24 }}>
              <Descriptions.Item label="分类">{book.categoryLabel}</Descriptions.Item>
              <Descriptions.Item label="ISBN">{book.isbn || "—"}</Descriptions.Item>
              <Descriptions.Item label="出版社">{book.publisher || "—"}</Descriptions.Item>
              <Descriptions.Item label="豆瓣评分">
                <Text type="warning" strong>{book.rating}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="库存状态">
                <Tag color={soldOut ? "red" : "green"}>
                  {soldOut ? "暂时缺货" : stock == null ? "现货充足" : `库存 ${stock} 本`}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="配送服务">满 99 元包邮</Descriptions.Item>
            </Descriptions>

            <Space size="middle" style={{ marginTop: 32 }}>
              <Button 
                type="primary" 
                size="large" 
                icon={<ShoppingCartOutlined />} 
                onClick={() => onAddToCart(book)}
                disabled={soldOut}
              >
                {soldOut ? "暂时缺货" : "加入购物车"}
              </Button>
              <Button 
                size="large" 
                icon={<PayCircleOutlined />} 
                onClick={() => navigate("/cart")}
              >
                立即结算
              </Button>
              <Button type="link" icon={<LeftOutlined />} onClick={() => navigate("/books")}>
                返回列表
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={8}>
          <Card title="内容亮点" variant="borderless" className="info-card" style={{ height: "100%" }}>
            <Paragraph>{book.highlight}</Paragraph>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="适合人群" variant="borderless" className="info-card" style={{ height: "100%" }}>
            <Paragraph>{book.audience}</Paragraph>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="读者评价" variant="borderless" className="info-card" style={{ height: "100%" }}>
            <Paragraph>{book.review}</Paragraph>
          </Card>
        </Col>
      </Row>

      {relatedBooks.length > 0 && (
        <>
          <Divider titlePlacement="left">同类推荐</Divider>
          <Row gutter={[24, 24]}>
            {relatedBooks.map((item) => (
              <Col xs={24} sm={12} md={8} lg={6} key={item.id}>
                <BookCard book={item} onBookSelect={onBookSelect} onAddToCart={onAddToCart} />
              </Col>
            ))}
          </Row>
        </>
      )}
    </div>
  );
}
