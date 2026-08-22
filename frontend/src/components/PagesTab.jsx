import { useEffect, useState } from "react";
import { api } from "../api";

export default function PagesTab() {
  const [pages, setPages] = useState([]);
  const [label, setLabel] = useState("");
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");

  const load = () => api.getPages().then(setPages);

  useEffect(() => {
    load();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await api.addPage(label, url);
      setLabel("");
      setUrl("");
      load();
    } catch (err) {
      setError(err.message);
    }
  };

  const toggleActive = async (id, active) => {
    await api.togglePageActive(id, active);
    load();
  };

  const remove = async (id) => {
    if (!confirm("Obrisati ovu stranicu iz pracenja?")) return;
    await api.deletePage(id);
    load();
  };

  return (
    <section>
      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Naziv (npr. Zlatne poluge)"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          required
        />
        <input
          type="url"
          placeholder="https://www.centarzlata.com/kategorija/..."
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
        />
        <button type="submit">Dodaj stranicu</button>
      </form>
      {error && <p style={{ color: "#c62828" }}>Greska: {error}</p>}

      <table className="data-table">
        <thead>
          <tr>
            <th>Naziv</th>
            <th>URL</th>
            <th>Aktivna</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {pages.map((p) => (
            <tr key={p.id}>
              <td>{p.label}</td>
              <td style={{ maxWidth: 340, overflow: "hidden", textOverflow: "ellipsis" }}>
                <a href={p.url} target="_blank" rel="noreferrer">
                  {p.url}
                </a>
              </td>
              <td>
                <input
                  type="checkbox"
                  checked={p.active}
                  onChange={(e) => toggleActive(p.id, e.target.checked)}
                />
              </td>
              <td>
                <button className="small" onClick={() => remove(p.id)}>
                  Obrisi
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
