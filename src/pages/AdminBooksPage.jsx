import { useMemo, useState } from "react";
import {
  Alert,
  Button,
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
} from "antd";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { createBook, deleteBook, updateBook } from "../api/bookstoreApi.js";
import { formatPrice } from "../utils/formatter.js";

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
    return <div className="state-panel"><Alert type="warning" showIcon message="只有管理员可以访问书籍管理" /></div>;
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
          className="admin-book-cover"
        />
      ),
    },
    { title: "书名", dataIndex: "title", width: 210 },
    { title: "作者", dataIndex: "author", width: 140 },
    { title: "ISBN", dataIndex: "isbn", width: 160 },
    { title: "出版社", dataIndex: "publisher", width: 170 },
    {
      title: "库存",
      dataIndex: "stock",
      width: 90,
      render: (stock) => <Tag color={stock > 0 ? "green" : "red"}>{stock ?? 0}</Tag>,
    },
    {
      title: "售价",
      dataIndex: "price",
      width: 110,
      align: "right",
      render: formatPrice,
    },
    {
      title: "操作",
      key: "action",
      fixed: "right",
      width: 160,
      render: (_, record) => (
        <Space className="admin-row-actions">
          <Button icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除这本书吗？"
            onConfirm={() => handleDelete(record)}
            okText="确定"
            cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />} aria-label={`删除 ${record.title}`} title="删除书籍" />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <section className="page admin-books-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">书城管理</p>
          <h1>书籍管理</h1>
          <p className="page-description">维护书籍、封面、ISBN、出版社和库存信息。</p>
        </div>
        <div className="page-actions">
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>添加新书</Button>
        </div>
      </header>

      <section className="table-panel admin-table-panel" aria-label="书籍列表">
        <div className="filter-bar admin-books-filter">
          <label htmlFor="admin-books-search" className="admin-filter-label">搜索书籍</label>
          <Input.Search
            id="admin-books-search"
            allowClear
            placeholder="按书名、作者或 ISBN 搜索"
            onSearch={setKeyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="admin-book-search"
          />
        </div>
        <Table
          className="admin-books-table"
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={filteredBooks}
          pagination={{ pageSize: 6 }}
          scroll={{ x: 1228 }}
        />
      </section>

      <Modal
        className="book-editor-modal"
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
          className="book-editor-form"
        >
          <div className="book-form-section">
            <h3>基本信息</h3>
            <Form.Item
              label="书籍 ID"
              name="id"
              rules={[{ required: !editingBook, message: "请输入书籍 ID" }]}
            >
              <Input disabled={!!editingBook} placeholder="例如：new-book" />
            </Form.Item>
            <div className="book-form-grid">
              <Form.Item
                label="书名"
                name="title"
                rules={[{ required: true, message: "请输入书名" }]}
              >
                <Input />
              </Form.Item>
              <Form.Item
                label="作者"
                name="author"
                rules={[{ required: true, message: "请输入作者" }]}
              >
                <Input />
              </Form.Item>
            </div>
            <div className="book-form-grid">
              <Form.Item label="ISBN" name="isbn">
                <Input />
              </Form.Item>
              <Form.Item label="出版社" name="publisher">
                <Input />
              </Form.Item>
            </div>
          </div>
          <div className="book-form-section">
            <h3>价格与库存</h3>
            <div className="book-form-grid book-form-grid-three">
              <Form.Item
                label="售价"
                name="price"
                rules={[{ required: true, message: "请输入售价" }]}
              >
                <InputNumber min={0} precision={2} />
              </Form.Item>
              <Form.Item label="定价" name="originalPrice">
                <InputNumber min={0} precision={2} />
              </Form.Item>
              <Form.Item
                label="库存"
                name="stock"
                rules={[{ required: true, message: "请输入库存" }]}
              >
                <InputNumber min={0} precision={0} />
              </Form.Item>
            </div>
          </div>
          <div className="book-form-section">
            <h3>封面与分类</h3>
            <Form.Item
              label="封面地址"
              name="image"
              rules={[{ required: true, message: "请输入封面地址" }]}
            >
              <Input placeholder="/images/三体.JPG" />
            </Form.Item>
            <div className="book-form-grid book-form-grid-three">
              <Form.Item label="分类编码" name="category">
                <Input />
              </Form.Item>
              <Form.Item label="分类名称" name="categoryName">
                <Input />
              </Form.Item>
              <Form.Item label="分类标签" name="categoryLabel">
                <Input />
              </Form.Item>
            </div>
            <div className="book-form-grid">
              <Form.Item label="评分" name="rating">
                <Input />
              </Form.Item>
              <Form.Item label="角标" name="badge">
                <Input />
              </Form.Item>
            </div>
          </div>
          <div className="book-form-section">
            <h3>内容介绍</h3>
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
          </div>
        </Form>
      </Modal>
    </section>
  );
}
