CREATE TABLE api_keys (
    id              BIGSERIAL PRIMARY KEY,
    key_hash        VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(100),
    tier            VARCHAR(20) DEFAULT 'FREE',
    rate_limit      INT DEFAULT 100,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active       BOOLEAN DEFAULT TRUE
);

ALTER TABLE urls ADD CONSTRAINT fk_urls_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id);

-- Seed a default API key for testing
-- Key: sk_test_snaplink_dev_key_123
INSERT INTO api_keys (key_hash, name, tier, rate_limit) VALUES (
    'e06a0eef35c211de1a5aa4e6651cde0f17f2709d895a4f3dd8c439ae4ba6f9ad',
    'Development Test Key',
    'FREE',
    100
);
