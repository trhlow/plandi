-- Chỉ thêm dòng, không sửa: lịch sử là bằng chứng người dùng đã đồng ý. Trạng thái hiện tại là dòng mới nhất mỗi purpose.
CREATE TABLE consent_event (
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    purpose        TEXT NOT NULL,
    granted        BOOLEAN NOT NULL,
    policy_version TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX consent_event_account_idx ON consent_event (account_id, purpose, created_at DESC);
