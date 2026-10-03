import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  message,
  Popconfirm,
  Table,
  Tag,
} from "antd";
import { StopOutlined, CheckCircleOutlined } from "@ant-design/icons";
import { listUsers, setUserEnabled } from "../api/bookstoreApi.js";
import { formatDateTime } from "../utils/formatter.js";

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
    return <div className="state-panel"><Alert type="warning" showIcon message="只有管理员可以访问用户管理" /></div>;
  }

  const columns = [
    { title: "ID", dataIndex: "id", width: 68 },
    { title: "用户名", dataIndex: "username", width: 170 },
    { title: "邮箱", dataIndex: "email", width: 250 },
    {
      title: "角色",
      dataIndex: "role",
      width: 92,
      render: (role) => (
        <Tag color={role === "ADMIN" ? "gold" : "blue"}>
          {role === "ADMIN" ? "管理员" : "顾客"}
        </Tag>
      ),
    },
    {
      title: "状态",
      dataIndex: "enabled",
      width: 100,
      render: (enabled) => (
        <Tag color={enabled ? "green" : "red"}>{enabled ? "正常" : "已禁用"}</Tag>
      ),
    },
    {
      title: "注册时间",
      dataIndex: "createdAt",
      width: 180,
      render: formatDateTime,
    },
    {
      title: "操作",
      key: "action",
      width: 110,
      fixed: "right",
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
    <section className="page admin-users-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">书城管理</p>
          <h1>用户管理</h1>
          <p className="page-description">管理员可以禁用或解禁顾客账号，被禁用用户将无法登录系统。</p>
        </div>
      </header>
      <section className="table-panel admin-table-panel" aria-label="用户列表">
        <Table
          className="admin-users-table"
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={users}
          pagination={{ pageSize: 8 }}
          scroll={{ x: 970 }}
        />
      </section>
    </section>
  );
}
