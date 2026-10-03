import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import App from "./App.jsx";
import "./styles.css";

dayjs.locale("zh-cn");

const bookstoreTheme = {
  token: {
    colorPrimary: "#244d3b",
    colorSuccess: "#244d3b",
    colorInfo: "#244d3b",
    colorWarning: "#9e5130",
    colorError: "#9d3332",
    colorText: "#202b24",
    colorTextSecondary: "#5b675f",
    colorBorder: "#d7dbd0",
    colorBgContainer: "#fffdf7",
    colorBgElevated: "#fffdf7",
    colorBgLayout: "#f7f4eb",
    borderRadius: 4,
    controlHeight: 40,
    controlHeightLG: 44,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif',
    boxShadow: "0 8px 24px rgba(32, 43, 36, 0.08)",
    boxShadowSecondary: "0 8px 24px rgba(32, 43, 36, 0.08)",
  },
  components: {
    Button: { primaryShadow: "none" },
    Card: { headerFontSize: 18 },
    Table: { headerBg: "#efeee4", rowHoverBg: "#f5f4ec" },
    Tabs: { horizontalItemGutter: 24 },
  },
};

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ConfigProvider theme={bookstoreTheme} locale={zhCN}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>
);
