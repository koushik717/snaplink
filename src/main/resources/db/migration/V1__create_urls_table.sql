CREATE TABLE urls (
    id              BIGSERIAL PRIMARY KEY,
    short_code      VARCHAR(10) NOT NULL UNIQUE,
    original_url    TEXT NOT NULL,
    api_key_id      BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE,
    is_active       BOOLEAN DEFAULT TRUE,
    click_count     BIGINT DEFAULT 0
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_api_key ON urls(api_key_id);
CREATE INDEX idx_urls_created_at ON urls(created_at);
