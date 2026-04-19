const BOOKS = {
  "clean-code": {
    id: "clean-code",
    title: "代码整洁之道",
    author: "Robert C. Martin",
    price: 79,
    originalPrice: 99,
    category: "programming",
    categoryLabel: "编程开发 / 软件工程",
    image: "../images/代码整洁之道.JPG",
    rating: "4.8 / 5.0",
    badge: "限时 8 折",
    summary: "本书围绕“可读性、可维护性、可演进性”展开，适合正在学习程序设计、软件工程与项目实践的读者。内容覆盖命名、函数设计、注释、对象与数据结构、错误处理、测试等核心主题。",
    highlight: "通过大量代码案例解释“坏味道”与“重构思路”，帮助读者建立工程级代码审美。",
    audience: "适合学习 Java、JavaScript、Python 等语言的开发者，也适合作为课程设计与项目作业参考书。",
    review: "“不仅讲语法，更讲判断标准；看完之后写代码会更在意结构与命名。”"
  },
  "design": {
    id: "design",
    title: "设计心理学",
    author: "Donald A. Norman",
    price: 65,
    originalPrice: 82,
    category: "design",
    categoryLabel: "设计创意 / 用户体验",
    image: "../images/设计心理学.JPG",
    rating: "4.7 / 5.0",
    badge: "设计经典",
    summary: "从可见性、反馈、映射和容错等角度解释为什么有些产品天然顺手，有些产品却让人困惑，非常适合界面设计与交互设计入门。",
    highlight: "用大量生活化案例解释好设计如何降低理解成本并提升使用效率。",
    audience: "适合产品、设计、前端开发以及任何对用户体验有兴趣的学习者。",
    review: "“读完之后会重新审视按钮、门把手和页面布局背后的设计逻辑。”"
  },
  "deep-work": {
    id: "deep-work",
    title: "深度工作",
    author: "Cal Newport",
    price: 58,
    originalPrice: 72,
    category: "growth",
    categoryLabel: "个人成长 / 学习方法",
    image: "../images/深度工作.JPG",
    rating: "4.6 / 5.0",
    badge: "效率提升",
    summary: "帮助读者建立专注习惯，减少分心成本，适合备考、写代码、做课程作业和进行长期项目训练。",
    highlight: "强调高质量专注输出的重要性，并提供可执行的训练策略。",
    audience: "适合学生、自学者以及希望提高学习效率和工作专注度的人群。",
    review: "“不是空泛鸡汤，而是可以立即实践的专注方法论。”"
  },
  "three-body": {
    id: "three-body",
    title: "三体",
    author: "刘慈欣",
    price: 49,
    originalPrice: 59,
    category: "literature",
    categoryLabel: "文学社科 / 科幻小说",
    image: "../images/三体.JPG",
    rating: "4.9 / 5.0",
    badge: "现象级作品",
    summary: "以恢弘宇宙视角展开文明碰撞与生存抉择，兼具科学想象力与叙事张力，是中文科幻的重要代表作。",
    highlight: "将宏大宇宙命题与人物命运并置，阅读体验极具冲击力。",
    audience: "适合喜欢科幻、社会议题和宏大叙事的读者。",
    review: "“从地球文明的局限，到宇宙尺度的残酷，这本书都写得极具震撼力。”"
  },
  "js-advanced": {
    id: "js-advanced",
    title: "JavaScript 高级程序设计",
    author: "Nicholas C. Zakas",
    price: 88,
    originalPrice: 108,
    category: "programming",
    categoryLabel: "编程开发 / JavaScript",
    image: "../images/高级程序设计.JPG",
    rating: "4.8 / 5.0",
    badge: "前端进阶",
    summary: "系统讲解 JavaScript 核心概念、执行机制和常见 API，适合作为前端课程、项目开发与面试准备的长期参考书。",
    highlight: "内容覆盖语言基础、浏览器环境、异步编程与工程实践，体系完整。",
    audience: "适合前端初学者、进阶开发者以及准备面试的学生。",
    review: "“内容扎实、覆盖面广，是理解 JavaScript 的高频参考书。”"
  },
  "sapiens": {
    id: "sapiens",
    title: "人类简史",
    author: "尤瓦尔·赫拉利",
    price: 62,
    originalPrice: 78,
    category: "literature",
    categoryLabel: "文学社科 / 历史人文",
    image: "../images/人类简史.JPG",
    rating: "4.7 / 5.0",
    badge: "畅销人文",
    summary: "从认知革命、农业革命到科学革命，重新梳理人类社会的形成与演变，视角开阔且表达通俗。",
    highlight: "将历史、经济、宗教与制度变迁串联起来，帮助读者建立宏观认知框架。",
    audience: "适合对历史、人类文明和社会演化感兴趣的读者。",
    review: "“读完之后看待社会、国家与制度的角度都会更宏观。”"
  }
};

const CART_KEY = "bookstore-cart";
const USER_KEY = "bookstore-user";
const ORDERS_KEY = "bookstore-orders";
let toastTimer = null;

function readCart() {
  return JSON.parse(localStorage.getItem(CART_KEY) || "[]");
}

function writeCart(cart) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

function readOrders() {
  return JSON.parse(localStorage.getItem(ORDERS_KEY) || "[]");
}

function writeOrders(orders) {
  localStorage.setItem(ORDERS_KEY, JSON.stringify(orders));
}

function updateCartCount() {
  const total = readCart().reduce((sum, item) => sum + item.quantity, 0);
  document.querySelectorAll("[data-cart-count]").forEach((node) => {
    node.textContent = String(total);
  });
}

function pushMessage(id, text) {
  const message = document.getElementById(id);
  if (message) {
    message.textContent = text;
  }
}

function showToast(text) {
  let toast = document.getElementById("siteToast");

  if (!toast) {
    toast = document.createElement("div");
    toast.id = "siteToast";
    toast.className = "toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    document.body.appendChild(toast);
  }

  toast.textContent = text;
  toast.classList.add("show");

  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }

  toastTimer = window.setTimeout(() => {
    toast.classList.remove("show");
  }, 1800);
}

function addToCart(bookId) {
  const book = BOOKS[bookId];
  if (!book) {
    return;
  }

  const cart = readCart();
  const existing = cart.find((item) => item.id === bookId);

  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({ ...book, quantity: 1 });
  }

  writeCart(cart);
  updateCartCount();
}

function setupAddToCartButtons() {
  document.querySelectorAll(".add-cart-button").forEach((button) => {
    button.addEventListener("click", () => {
      addToCart(button.dataset.bookId);
      pushMessage("cartMessage", "商品已加入购物车。");
      pushMessage("loginMessage", "已作为游客进入，可直接浏览和加入购物车。");
      showToast("已加入购物车");

      const originalText = button.textContent;
      button.textContent = "已加入";
      button.disabled = true;

      window.setTimeout(() => {
        button.textContent = originalText;
        button.disabled = false;
      }, 900);
    });
  });
}

function setupCategoryFilter() {
  const cards = document.querySelectorAll(".book-card[data-category]");
  const links = document.querySelectorAll("[data-category-link]");

  if (!cards.length || !links.length) {
    return;
  }

  const params = new URLSearchParams(window.location.search);
  const activeCategory = params.get("category") || "all";

  links.forEach((link) => {
    link.classList.toggle("active", link.dataset.categoryLink === activeCategory);
  });

  cards.forEach((card) => {
    const visible = activeCategory === "all" || card.dataset.category === activeCategory;
    card.classList.toggle("is-hidden", !visible);
  });
}

function setupLoginForm() {
  const form = document.getElementById("loginForm");
  if (!form) {
    return;
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();

    if (!username) {
      pushMessage("loginMessage", "请输入用户名。");
      return;
    }

    if (password.length < 6) {
      pushMessage("loginMessage", "密码长度不能少于 6 位。");
      return;
    }

    localStorage.setItem(USER_KEY, username);
    pushMessage("loginMessage", "登录成功，正在跳转到书城首页。");
    window.setTimeout(() => {
      window.location.href = "pages/books.html";
    }, 500);
  });
}

function renderCart() {
  const cartList = document.getElementById("cartList");
  if (!cartList) {
    return;
  }

  const cart = readCart();
  if (!cart.length) {
    cartList.innerHTML = `
      <div class="empty-state">
        <p>购物车还是空的，先去书籍列表页挑选喜欢的图书。</p>
      </div>
    `;
    renderSummary();
    return;
  }

  cartList.innerHTML = cart.map((item) => `
    <article class="cart-item" data-cart-id="${item.id}">
      <div class="cart-item-meta">
        <h3>${item.title}</h3>
        <p>单价：¥${item.price.toFixed(2)}</p>
        <p>小计：¥${(item.price * item.quantity).toFixed(2)}</p>
      </div>
      <div class="cart-controls">
        <button class="qty-button" type="button" data-action="decrease">-</button>
        <span class="qty-value">${item.quantity}</span>
        <button class="qty-button" type="button" data-action="increase">+</button>
        <button class="button button-secondary button-mini" type="button" data-action="remove">删除</button>
      </div>
    </article>
  `).join("");

  cartList.querySelectorAll(".cart-item").forEach((row) => {
    row.addEventListener("click", (event) => {
      const action = event.target.dataset.action;
      if (!action) {
        return;
      }

      const itemId = row.dataset.cartId;
      const cartData = readCart();
      const current = cartData.find((item) => item.id === itemId);

      if (!current) {
        return;
      }

      if (action === "increase") {
        current.quantity += 1;
      }

      if (action === "decrease") {
        current.quantity = Math.max(1, current.quantity - 1);
      }

      if (action === "remove") {
        writeCart(cartData.filter((item) => item.id !== itemId));
        renderCart();
        updateCartCount();
        return;
      }

      writeCart(cartData);
      renderCart();
      updateCartCount();
    });
  });

  renderSummary();
}

function renderSummary() {
  const cart = readCart();
  const count = cart.reduce((sum, item) => sum + item.quantity, 0);
  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const shipping = count > 0 && subtotal < 99 ? 12 : 0;
  const total = subtotal + shipping;

  const countNode = document.getElementById("summaryCount");
  const subtotalNode = document.getElementById("summarySubtotal");
  const shippingNode = document.getElementById("summaryShipping");
  const totalNode = document.getElementById("summaryTotal");

  if (countNode) countNode.textContent = `${count} 件`;
  if (subtotalNode) subtotalNode.textContent = `¥${subtotal.toFixed(2)}`;
  if (shippingNode) shippingNode.textContent = `¥${shipping.toFixed(2)}`;
  if (totalNode) totalNode.textContent = `¥${total.toFixed(2)}`;
}

function setupCheckout() {
  const checkoutButton = document.getElementById("checkoutButton");
  if (!checkoutButton) {
    return;
  }

  checkoutButton.addEventListener("click", () => {
    const cart = readCart();
    if (!cart.length) {
      pushMessage("cartMessage", "购物车为空，无法提交订单。");
      return;
    }

    const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const shipping = subtotal < 99 ? 12 : 0;
    const username = localStorage.getItem(USER_KEY) || "游客用户";
    const orders = readOrders();
    const orderNumber = `ZY${Date.now()}`;

    orders.unshift({
      id: orderNumber,
      createdAt: new Date().toLocaleString("zh-CN", { hour12: false }),
      status: "待发货",
      user: username,
      total: subtotal + shipping,
      items: cart
    });

    writeOrders(orders);
    writeCart([]);
    updateCartCount();
    pushMessage("cartMessage", "订单已提交，正在跳转到订单页。");

    window.setTimeout(() => {
      window.location.href = "orders.html";
    }, 500);
  });
}

function renderOrders() {
  const list = document.getElementById("ordersList");
  if (!list) {
    return;
  }

  const orders = readOrders();
  const seededOrder = list.innerHTML;

  if (!orders.length) {
    list.innerHTML = seededOrder;
    return;
  }

  const dynamicOrders = orders.map((order) => `
    <article class="order-card">
      <header class="order-top">
        <div>
          <h2>订单号 ${order.id}</h2>
          <p>下单时间：${order.createdAt}</p>
        </div>
        <span class="status-tag success">${order.status}</span>
      </header>
      <ul class="order-items">
        ${order.items.map((item) => `<li>《${item.title}》 x ${item.quantity}</li>`).join("")}
      </ul>
      <footer class="order-bottom">
        <p>收货人：${order.user}</p>
        <strong>合计：¥${Number(order.total).toFixed(2)}</strong>
      </footer>
    </article>
  `).join("");

  list.innerHTML = dynamicOrders + seededOrder;
}

function renderDetail() {
  const titleNode = document.getElementById("detailTitle");
  if (!titleNode) {
    return;
  }

  const params = new URLSearchParams(window.location.search);
  const bookId = params.get("book") || "clean-code";
  const book = BOOKS[bookId] || BOOKS["clean-code"];

  document.getElementById("detailBreadcrumb").textContent = book.title;
  document.getElementById("detailImage").src = book.image;
  document.getElementById("detailImage").alt = `${book.title}封面`;
  titleNode.textContent = book.title;
  document.getElementById("detailAuthor").textContent = `${book.author} 著`;
  document.getElementById("detailPrice").textContent = `¥${book.price.toFixed(2)}`;
  document.getElementById("detailOriginalPrice").textContent = `原价 ¥${book.originalPrice.toFixed(2)}`;
  document.getElementById("detailBadge").textContent = book.badge;
  document.getElementById("detailSummary").textContent = book.summary;
  document.getElementById("detailHighlight").textContent = book.highlight;
  document.getElementById("detailAudience").textContent = book.audience;
  document.getElementById("detailReview").textContent = book.review;
  document.getElementById("detailAddCartButton").dataset.bookId = book.id;
  document.title = `知页书城 - ${book.title}`;

  document.getElementById("detailFeatures").innerHTML = `
    <li>分类：${book.categoryLabel}</li>
    <li>评分：${book.rating}</li>
    <li>库存：现货充足</li>
    <li>配送：满 99 元包邮</li>
  `;
}

document.addEventListener("DOMContentLoaded", () => {
  updateCartCount();
  setupCategoryFilter();
  renderDetail();
  setupLoginForm();
  setupAddToCartButtons();
  renderCart();
  renderOrders();
  setupCheckout();
});
