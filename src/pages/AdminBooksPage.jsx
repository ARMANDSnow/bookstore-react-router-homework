import { useMemo, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Form,
  Image,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { createBook, deleteBook, updateBook } from "../api/bookstoreApi.js";
import { formatPrice } from "../utils/formatter.js";

const { Title, Text } = Typography;
const { TextArea } = Input;

const defaultBookValues = {
  id: "",
  title: "",
  author: "",
  isbn: "",
  publisher: "",
  stock: 10,
  price: 0,
  originalPrice: 0,
  category: "general",
  categoryName: "综合图书",
  categoryLabel: "综合图书",
  image: "/images/三体.JPG",
  rating: "暂无评分",
  badge: "",
  description: "",
  summary: "",
  highlight: "",
  audience: "",
  review: "",
};

export default function AdminBooksPage({ user, books, loading, onBooksChanged }) {
  const [keyword, setKeyword] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const isAdmin = user?.role === "ADMIN";

  const filteredBooks = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    if (!kw) return books;
    return books.filter((book) =>
      [book.title, book.author, book.isbn].some((value) =>
        String(value || "").toLowerCase().includes(kw)
      )
    );
  }, [books, keyword]);

  function openCreate() {
    setEditingBook(null);
    form.setFieldsValue(defaultBookValues);
    setModalOpen(true);
  }

  function openEdit(record) {
    setEditingBook(record);
    form.setFieldsValue({ ...defaultBookValues, ...record });
    setModalOpen(true);
  }

  async function handleSubmit(values) {
    setSubmitting(true);
    try {
      if (editingBook) {
        await updateBook(editingBook.id, values);
        message.success("书籍信息已更新");
      } else {
        await createBook(values);
        message.success("新书已添加");
      }
      setModalOpen(false);
      await onBooksChanged?.();
    } catch (err) {
      message.error(err.message || "保存失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(record) {
    try {
      await deleteBook(record.id);
      message.success("书籍已删除");
      await onBooksChanged?.();
    } catch (err) {
      message.error(err.message || "删除失败");
    }
  }

  if (!isAdmin) {
    return <Alert type="warning" showIcon message="只有管理员可以访问书籍管理" />;
  }

  const columns = [
    {
      title: "封面",
      dataIndex: "image",
      width: 88,
      render: (image, record) => (
        <Image
          src={image}
          alt={record.title}
          width={48}
          height={66}
          style={{ objectFit: "cover", borderRadius: 4 }}
        />
      ),
    },
    { title: "书名", dataIndex: "title" },
    { title: "作者", dataIndex: "author" },
    { title: "ISBN", dataIndex: "isbn" },
    { title: "出版社", dataIndex: "publisher" },
    {
      title: "库存",
      dataIndex: "stock",
      render: (stock) => <Tag color={stock > 0 ? "green" : "red"}>{stock ?? 0}</Tag>,
    },
    {
      title: "售价",
      dataIndex: "price",
      render: formatPrice,
    },
    {
      title: "操作",
      key: "action",
      fixed: "right",
      width: 160,
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除这本书吗？"
            onConfirm={() => handleDelete(record)}
            okText="确定"
            cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Space direction="vertical" size={2}>
        <Title level={2} style={{ margin: 0 }}>书籍管理</Title>
        <Text type="secondary">维护数据库中的书籍、封面、ISBN、出版社和库存信息。</Text>
      </Space>

      <Card variant="borderless" className="profile-card">
        <Space style={{ marginBottom: 16, width: "100%", justifyContent: "space-between" }} wrap>
          <Input.Search
            allowClear
            placeholder="按书名、作者或 ISBN 搜索"
            onSearch={setKeyword}
            onChange={(event) => setKeyword(event.target.value)}
            style={{ width: 320 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            添加新书
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={filteredBooks}
          pagination={{ pageSize: 6 }}
          scroll={{ x: 1100 }}
        />
      </Card>

      <Modal
        title={editingBook ? "编辑书籍" : "添加新书"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={submitting}
        width={760}
        okText="保存"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          style={{ maxHeight: "70vh", overflowY: "auto", paddingRight: 8 }}
        >
          <Form.Item
            label="书籍 ID"
            name="id"
            rules={[{ required: !editingBook, message: "请输入书籍 ID" }]}
          >
            <Input disabled={!!editingBook} placeholder="例如：new-book" />
          </Form.Item>
          <Space size={16} style={{ display: "flex" }} align="start">
            <Form.Item
              label="书名"
              name="title"
              rules={[{ required: true, message: "请输入书名" }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
            <Form.Item
              label="作者"
              name="author"
              rules={[{ required: true, message: "请输入作者" }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>
          <Space size={16} style={{ display: "flex" }} align="start">
            <Form.Item label="ISBN" name="isbn" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item label="出版社" name="publisher" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space size={16} style={{ display: "flex" }} align="start">
            <Form.Item
              label="售价"
              name="price"
              rules={[{ required: true, message: "请输入售价" }]}
              style={{ flex: 1 }}
            >
              <InputNumber min={0} precision={2} style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item label="定价" name="originalPrice" style={{ flex: 1 }}>
              <InputNumber min={0} precision={2} style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item
              label="库存"
              name="stock"
              rules={[{ required: true, message: "请输入库存" }]}
              style={{ flex: 1 }}
            >
              <InputNumber min={0} precision={0} style={{ width: "100%" }} />
            </Form.Item>
          </Space>
          <Form.Item
            label="封面地址"
            name="image"
            rules={[{ required: true, message: "请输入封面地址" }]}
          >
            <Input placeholder="/images/三体.JPG" />
          </Form.Item>
          <Space size={16} style={{ display: "flex" }} align="start">
            <Form.Item label="分类编码" name="category" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item label="分类名称" name="categoryName" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item label="分类标签" name="categoryLabel" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space size={16} style={{ display: "flex" }} align="start">
            <Form.Item label="评分" name="rating" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item label="角标" name="badge" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Form.Item label="短简介" name="description">
            <Input />
          </Form.Item>
          <Form.Item label="详细简介" name="summary">
            <TextArea rows={3} />
          </Form.Item>
          <Form.Item label="内容亮点" name="highlight">
            <TextArea rows={2} />
          </Form.Item>
          <Form.Item label="适合人群" name="audience">
            <TextArea rows={2} />
          </Form.Item>
          <Form.Item label="读者评价" name="review">
            <TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
