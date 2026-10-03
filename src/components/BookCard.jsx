import { Card, Button, Typography, Tag, Space } from "antd";
import { ShoppingCartOutlined, EyeOutlined } from "@ant-design/icons";

const { Text } = Typography;

// 纯展示组件：只根据 book props 渲染卡片，不自己请求数据、不保存业务状态。
// 查看详情/加入购物车都通过回调交给父组件 App.jsx 处理，便于复用和测试。
export default function BookCard({ book, onBookSelect, onAddToCart }) {
  const stock = book.stock;
  const soldOut = stock !== undefined && stock !== null && stock <= 0;
  const price = Number(book.price || 0);
  const originalPrice = Number(book.originalPrice || 0);

  return (
    <Card
      hoverable
      className="book-card-container"
      cover={
        <img
          alt={book.title}
          src={book.image}
          // 点击封面等价于"查看详情"，由父组件负责路由跳转。
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
          // 加车需要用户信息和后端调用，放在父组件统一处理。
          onClick={() => onAddToCart(book)}
          key="add"
          style={{ color: "#2f6f64" }}
          disabled={soldOut}
        >
          {soldOut ? "缺货" : "加入购物车"}
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
          <Space direction="vertical" size={4} style={{ width: "100%" }}>
            <Text type="secondary">{book.author} 著</Text>
            <Text type={soldOut ? "danger" : "secondary"}>
              {soldOut ? "暂时缺货" : stock == null ? "现货充足" : `库存 ${stock} 本`}
            </Text>
            <div style={{ marginTop: 8 }}>
              <span className="price-text">¥{price.toFixed(2)}</span>
              {originalPrice > price && (
                <span className="original-price">¥{originalPrice.toFixed(2)}</span>
              )}
            </div>
          </Space>
        }
      />
    </Card>
  );
}
