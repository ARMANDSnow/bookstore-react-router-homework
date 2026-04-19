const categories = [
  { value: "all", label: "全部" },
  { value: "programming", label: "编程开发" },
  { value: "design", label: "设计创意" },
  { value: "literature", label: "文学社科" },
  { value: "growth", label: "个人成长" },
];

export default function CategoryFilter({ activeCategory, onChange }) {
  return (
    <section className="filter-bar">
      <header className="section-header compact">
        <p className="eyebrow">Catalog</p>
        <h2>书籍列表</h2>
      </header>
      <div className="filter-pills">
        {categories.map((category) => (
          <button
            className={`pill ${activeCategory === category.value ? "active" : ""}`}
            type="button"
            key={category.value}
            onClick={() => onChange(category.value)}
          >
            {category.label}
          </button>
        ))}
      </div>
    </section>
  );
}
