import { useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { fmtEUR } from "../format";

export default function ProductsTab({ refreshKey }) {
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState("");
  const [showRemoved, setShowRemoved] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api
      .getProducts(showRemoved)
      .then(setProducts)
      .finally(() => setLoading(false));
  }, [showRemoved, refreshKey]);

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    return products.filter((p) => p.name.toLowerCase().includes(q));
  }, [products, search]);

  return (
    <section>
      <div className="toolbar">
        <input
          type="text"
          placeholder="Pretrazi proizvode..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <label>
          <input
            type="checkbox"
            checked={showRemoved}
            onChange={(e) => setShowRemoved(e.target.checked)}
          />{" "}
          Prikazi uklonjene
        </label>
      </div>

      {!loading && filtered.length === 0 && (
        <p style={{ color: "#999" }}>
          Nema proizvoda za prikaz. Pokreni "Provjeri sada" ili provjeri stranice za pracenje.
        </p>
      )}

      <div className="grid">
        {filtered.map((p) => (
          <div className={`product-card ${p.removed ? "removed" : ""}`} key={p.slug}>
            <span className={`stock-badge ${p.inStock ? "in" : "out"}`}>
              {p.inStock ? "Dostupno" : "Nedostupno"}
            </span>
            {p.imageUrl && <img src={p.imageUrl} alt="" />}
            <div className="category">{p.pageLabel || ""}</div>
            <div className="name">{p.name}</div>
            <div className="price">{fmtEUR(p.price)}</div>
          </div>
        ))}
      </div>
    </section>
  );
}
