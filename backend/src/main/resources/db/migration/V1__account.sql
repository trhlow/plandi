CREATE TABLE account (
    id                  BIGSERIAL PRIMARY KEY,
    email               TEXT NOT NULL,
    password_hash       TEXT NOT NULL,
    display_name        TEXT NOT NULL,
    area                TEXT NOT NULL,
    role                TEXT NOT NULL DEFAULT 'USER',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX account_email_uq ON account (email);
