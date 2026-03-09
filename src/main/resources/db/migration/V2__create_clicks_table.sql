CREATE TABLE clicks (
    id              BIGSERIAL PRIMARY KEY,
    url_id          BIGINT REFERENCES urls(id) NOT NULL,
    clicked_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    referrer        TEXT,
    country         VARCHAR(2),
    device_type     VARCHAR(20),
    browser         VARCHAR(50)
);

CREATE INDEX idx_clicks_url_id ON clicks(url_id);
CREATE INDEX idx_clicks_clicked_at ON clicks(clicked_at);
