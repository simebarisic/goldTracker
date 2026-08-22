import { useState } from "react";
import { api } from "./api";
import ProductsTab from "./components/ProductsTab";
import ChangesTab from "./components/ChangesTab";
import PagesTab from "./components/PagesTab";
import RunsTab from "./components/RunsTab";

const TABS = [
  { key: "products", label: "Proizvodi" },
  { key: "changes", label: "Promjene" },
  { key: "pages", label: "Stranice za pracenje" },
  { key: "runs", label: "Povijest provjera" },
];

export default function App() {
  const [activeTab, setActiveTab] = useState("products");
  const [running, setRunning] = useState(false);
  const [runStatus, setRunStatus] = useState("");
  // mijenja se nakon svakog "Provjeri sada" da tabovi znaju osvjeziti podatke
  const [refreshKey, setRefreshKey] = useState(0);

  const runNow = async () => {
    setRunning(true);
    setRunStatus("Provjera u tijeku... (moze potrajati koju minutu)");
    try {
      const data = await api.runNow();
      setRunStatus(`Zavrseno: ${data.productsFound} proizvoda, ${data.changes.length} promjena.`);
      setRefreshKey((k) => k + 1);
    } catch (err) {
      setRunStatus("Greska prilikom provjere.");
    } finally {
      setRunning(false);
    }
  };

  return (
    <>
      <header>
        <h1>🪙 Gold Tracker</h1>
        <p className="subtitle">Pracenje proizvoda i cijena — Centar Zlata</p>
        <div className="header-actions">
          <button onClick={runNow} disabled={running}>
            Provjeri sada
          </button>
          <span>{runStatus}</span>
        </div>
      </header>

      <nav className="tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab-btn ${activeTab === t.key ? "active" : ""}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </nav>

      <main>
        {activeTab === "products" && <ProductsTab refreshKey={refreshKey} />}
        {activeTab === "changes" && <ChangesTab refreshKey={refreshKey} />}
        {activeTab === "pages" && <PagesTab />}
        {activeTab === "runs" && <RunsTab refreshKey={refreshKey} />}
      </main>
    </>
  );
}
