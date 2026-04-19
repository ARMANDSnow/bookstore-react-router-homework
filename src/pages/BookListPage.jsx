import { useMemo, useState } from "react";
import BookCard from "../components/BookCard.jsx";
import CategoryFilter from "../components/CategoryFilter.jsx";
import HeroBanner from "../components/HeroBanner.jsx";

export default function BookListPage({ books, onBookSelect, onAddToCart }) {
  const [activeCategory, setActiveCategory] = useState("all");
  const featuredBook = books.find((book) => book.id === "three-body") || books[0];
  const visibleBooks = useMemo(
    () =>
      activeCategory === "all"
        ? books
        : books.filter((book) => book.category === activeCategory),
    [activeCategory, books]
  );

  return (
    <>
      <HeroBanner featuredBook={featuredBook} onBookSelect={onBookSelect} />
      <CategoryFilter activeCategory={activeCategory} onChange={setActiveCategory} />
      <section className="books-grid" aria-label="书籍列表">
        {visibleBooks.map((book) => (
          <BookCard
            key={book.id}
            book={book}
            onBookSelect={onBookSelect}
            onAddToCart={onAddToCart}
          />
        ))}
      </section>
    </>
  );
}
