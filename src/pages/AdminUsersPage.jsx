import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  message,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import { StopOutlined, CheckCircleOutlined } from "@ant-design/icons";
import { listUsers, setUserEnabled } from "../api/bookstoreApi.js";
import { formatDateTime } from "../utils/formatter.js";

const { Title, Text } = Typography;

export default function AdminUsersPage({ user }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  const isAdmin = user?.role === "ADMIN";

  const refresh = useCallback(async () => {
    if (!isAdmin) return;
    setLoading(true);
    try {
      setUsers(await listUsers());
    } catch (err) {
      message.error(err.message || "获取用户列表失败");
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function toggleEnabled(record) {
    try {
      await setUserEnabled(record.id, !record.enabled);
      message.success(record.enabled ? "用户已禁用" : "用户已解禁");
      await refresh();
    } catch (err) {
      message.error(err.message || "操作失败");
    }
  }

  if (!isAdmin) {
    return <Alert type="warning" showIcon message="只有管理员可以访问用户管理" />;
  }

  const columns = [
    { title: "ID", dataIndex: "id", width: 80 },
    { title: "用户名", dataIndex: "username" },
    { title: "邮箱", dataIndex: "email" },
    {
      title: "角色",
      dataIndex: "role",
      render: (role) => (
        <Tag color={role === "ADMIN" ? "gold" : "blue"}>
          {role === "ADMIN" ? "管理员" : "顾客"}
        </Tag>
      ),
    },
    {
      title: "状态",
      dataIndex: "enabled",
      render: (enabled) => (
        <Tag color={enabled ? "green" : "red"}>{enabled ? "正常" : "已禁用"}</Tag>
      ),
    },
    {
      title: "注册时间",
      dataIndex: "createdAt",
      render: formatDateTime,
    },
    {
      title: "操作",
      key: "action",
      render: (_, record) => (
        <Popconfirm
          title={record.enabled ? "确定禁用该用户吗？" : "确定解禁该用户吗？"}
          onConfirm={() => toggleEnabled(record)}
          okText="确定"
          cancelText="取消"
        >
          <Button
            danger={record.enabled}
            type={record.enabled ? "default" : "primary"}
            icon={record.enabled ? <StopOutlined /> : <CheckCircleOutlined />}
          >
            {record.enabled ? "禁用" : "解禁"}
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Space direction="vertical" size={2}>
        <Title level={2} style={{ margin: 0 }}>用户管理</Title>
        <Text type="secondary">管理员可以禁用或解禁顾客账号，被禁用用户将无法登录系统。</Text>
      </Space>
      <Card variant="borderless" className="profile-card">
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={users}
          pagination={{ pageSize: 8 }}
          scroll={{ x: 900 }}
        />
      </Card>
    </div>
  );
}
