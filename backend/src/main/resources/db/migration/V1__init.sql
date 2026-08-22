CREATE TABLE tracked_pages (
    id BIGSERIAL PRIMARY KEY,
    label TEXT NOT NULL,
    url TEXT NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    page_label TEXT,
    price DOUBLE PRECISION,
    currency TEXT NOT NULL DEFAULT 'EUR',
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    image_url TEXT,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_slug TEXT NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE changes_log (
    id BIGSERIAL PRIMARY KEY,
    type TEXT NOT NULL,
    product_slug TEXT,
    product_name TEXT,
    old_value TEXT,
    new_value TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE run_log (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'running',
    pages_checked INTEGER DEFAULT 0,
    products_found INTEGER DEFAULT 0,
    changes_found INTEGER DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_price_history_slug ON price_history (product_slug);
CREATE INDEX idx_changes_log_detected_at ON changes_log (detected_at DESC);
CREATE INDEX idx_products_removed ON products (removed);
