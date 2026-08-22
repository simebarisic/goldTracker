export function fmtEUR(v) {
  if (v == null) return "—";
  return new Intl.NumberFormat("hr-HR", { style: "currency", currency: "EUR" }).format(v);
}

export function fmtDate(v) {
  if (!v) return "—";
  return new Date(v).toLocaleString("hr-HR");
}
