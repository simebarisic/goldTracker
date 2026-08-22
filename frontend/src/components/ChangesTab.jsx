import { useEffect, useState } from "react";
import { api } from "../api";
import { fmtDate, fmtEUR } from "../format";

const LABELS = {
  new_product: ["Novi proizvod", "badge-new"],
  price_change: ["Promjena cijene", "badge-price"],
  removed_product: ["Uklonjen", "badge-removed"],
  out_of_stock: ["Nedostupno", "badge-stock"],
  back_in_stock: ["Ponovno dostupno", "badge-stock"],
};

export default function ChangesTab({ refreshKey }) {
  const [rows, setRows] = useState([]);

  useEffect(() => {
    api.getChanges().then(setRows);
  }, [refreshKey]);

  return (
    <section>
      <table className="data-table">
        <thead>
          <tr>
            <th>Datum</th>
            <th>Tip</th>
            <th>Proizvod</th>
            <th>Staro</th>
            <th>Novo</th>
            <th>Razlika</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => {
            const [label, cls] = LABELS[r.type] || [r.type, ""];
            const isPrice = r.type === "price_change";
            const old = r.oldValue ? (isPrice ? fmtEUR(r.oldValue) : r.oldValue) : "—";
            const nw = r.newValue ? (isPrice ? fmtEUR(r.newValue) : r.newValue) : "—";

            let diffCell = "—";
            let diffCls = "";
            if (isPrice) {
              const diff = parseFloat(r.newValue) - parseFloat(r.oldValue);
              const sign = diff > 0 ? "+" : "";
              diffCls = diff > 0 ? "diff-up" : diff < 0 ? "diff-down" : "diff-flat";
              diffCell = `${sign}${fmtEUR(diff)}`;
            }

            return (
              <tr key={r.id}>
                <td>{fmtDate(r.detectedAt)}</td>
                <td>
                  <span className={`badge-type ${cls}`}>{label}</span>
                </td>
                <td>{r.productName || r.productSlug || ""}</td>
                <td>{old}</td>
                <td>{nw}</td>
                <td className={diffCls}>{diffCell}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}
