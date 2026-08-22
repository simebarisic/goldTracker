const API = "/api";

async function request(path, options) {
  const res = await fetch(`${API}${path}`, options);
  if (!res.ok) {
    let message = `Greska (${res.status})`;
    try {
      const body = await res.json();
      if (body.error) message = body.error;
    } catch {
      // odgovor nije JSON, zadrzi generičku poruku
    }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

export const api = {
  getProducts: (showRemoved) => request(`/products?removed=${showRemoved ? 1 : 0}`),
  getProductHistory: (slug) => request(`/products/history?slug=${encodeURIComponent(slug)}`),
  getChanges: (limit = 100) => request(`/changes?limit=${limit}`),
  getPages: () => request("/pages"),
  addPage: (label, url) =>
    request("/pages", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ label, url }),
    }),
  togglePageActive: (id, active) =>
    request(`/pages/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ active }),
    }),
  deletePage: (id) => request(`/pages/${id}`, { method: "DELETE" }),
  getRuns: () => request("/runs"),
  runNow: () => request("/run-now", { method: "POST" }),
};
