export default function HeroBanner({ featuredBook, onBookSelect }) {
  return (
    <section className="hero-banner">
      <article className="banner-copy">
        <p className="eyebrow">Spring Reading List</p>
        <h1>找到下一本值得收藏的书。</h1>
        <p>
          从计算机科学到叙事文学，从设计思维到个人成长，围绕好内容和好体验组织你的阅读清单。
        </p>
        <div className="hero-actions">
          <button className="button button-primary" type="button" onClick={() => onBookSelect(featuredBook)}>
            查看精选图书
          </button>
          <span className="hero-note">当前精选：《{featuredBook.title}》</span>
        </div>
      </article>
      <aside className="banner-panel" aria-label="书城数据">
        <div className="banner-stat">
          <span className="stat-number">128</span>
          <span className="stat-label">本周热销</span>
        </div>
        <div className="banner-stat">
          <span className="stat-number">4.9</span>
          <span className="stat-label">综合好评</span>
        </div>
        <div className="banner-stat">
          <span className="stat-number">24h</span>
          <span className="stat-label">发货响应</span>
        </div>
      </aside>
    </section>
  );
}
