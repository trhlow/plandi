-- Quỹ là cuốn sổ ghi ngân sách và chi tiêu, không chứa tiền thật. Giao diện gọi là "Ngân sách cá nhân/chung".
CREATE TABLE fund (
    id         BIGSERIAL PRIMARY KEY,
    kind       TEXT NOT NULL,
    status     TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at  TIMESTAMPTZ
);

CREATE TABLE fund_member (
    fund_id    BIGINT NOT NULL REFERENCES fund (id),
    account_id BIGINT NOT NULL,
    fund_kind  TEXT NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at    TIMESTAMPTZ,
    PRIMARY KEY (fund_id, account_id)
);

-- Tối đa một quỹ PERSONAL và một quỹ COUPLE đang hoạt động cho mỗi tài khoản, chặn ngay ở tầng DB.
CREATE UNIQUE INDEX fund_member_one_active_per_kind ON fund_member (account_id, fund_kind) WHERE left_at IS NULL;
CREATE INDEX fund_member_account_idx ON fund_member (account_id);
