import { Card, Button, Typography, Tag, Space } from "antd";
import { ShoppingCartOutlined, EyeOutlined } from "@ant-design/icons";

const { Text } = Typography;

export default function BookCard({ book, onBookSelect, onAddToCart }) {
  return (
    <Card
      hoverable
      className="book-card-container"
      cover={
        <img
          alt={book.title}
          src={book.image}
          onClick={() => onBookSelect(book)}
          style={{ cursor: "pointer", borderBottom: "1px solid #f0f0f0" }}
        />
      }
      actions={[
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => onBookSelect(book)}
          key="view"
        >
          查看详情
        </Button>,
        <Button
          type="text"
          icon={<ShoppingCartOutlined />}
          onClick={() => onAddToCart(book)}
          key="add"
          style={{ color: "#2f6f64" }}
        >
          加入购物车
        </Button>,
      ]}
      style={{ borderRadius: 8, overflow: "hidden" }}
      styles={{ body: { padding: 16 } }}
    >
      <Card.Meta
        title={
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span
              onClick={() => onBookSelect(book)}
              style={{ cursor: "pointer", flex: 1, overflow: "hidden", textOverflow: "ellipsis" }}
            >
              {book.title}
            </span>
            {book.badge && <Tag color="volcano">{book.badge}</Tag>}
          </div>
        }
        description={
          <Space orientation="vertical" size={4} style={{ width: "100%" }}>
            <Text type="secondary">{book.author} 著</Text>
            <div style={{ marginTop: 8 }}>
              <span className="price-text">¥{book.price.toFixed(2)}</span>
              {book.originalPrice > book.price && (
                <span className="original-price">¥{book.originalPrice.toFixed(2)}</span>
              )}
            </div>
          </Space>
        }
      />
    </Card>
  );
}
