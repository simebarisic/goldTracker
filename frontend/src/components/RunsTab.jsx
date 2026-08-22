import { useEffect, useState } from "react";
import { api } from "../api";
import { fmtDate } from "../format";

export default function RunsTab({ refreshKey }) {
  const [rows, setRows] = useState([]);

  useEffect(() => {
    api.getRuns().then(setRows);
  }, [refreshKey]);

  return (
    <section>
      <table className="data-table">
        <thead>
          <tr>
            <th>Pokrenuto</th>
            <th>Status</th>
            <th>Stranica</th>
            <th>Proizvoda</th>
            <th>Promjena</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td>{fmtDate(r.startedAt)}</td>
              <td>{r.status}</td>
              <td>{r.pagesChecked ?? "—"}</td>
              <td>{r.productsFound ?? "—"}</td>
              <td>{r.changesFound ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
