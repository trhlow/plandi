package com.plandi.fund.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Mọi truy vấn của module fund. Viết SQL thẳng để điều kiện quyền ({@code fund_id}, {@code account_id}, {@code left_at},
 * {@code status}) luôn nhìn thấy được khi đọc lại.
 */
@Repository
public class FundStore {

    public enum Kind {
        PERSONAL,
        COUPLE
    }

    public record FundRow(long id, Kind kind, String status) {}

    public record ActiveFund(long id, Kind kind, int memberCount) {}

    public record Membership(Kind kind, Instant joinedAt, Instant leftAt) {}

    private final JdbcClient jdbc;

    FundStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long createFund(Kind kind) {
        return jdbc.sql("INSERT INTO fund (kind, status) VALUES (:kind, 'ACTIVE') RETURNING id")
                .param("kind", kind.name())
                .query(Long.class)
                .single();
    }

    public void addMember(long fundId, long accountId, Kind kind) {
        jdbc.sql("INSERT INTO fund_member (fund_id, account_id, fund_kind) VALUES (:fundId, :accountId, :kind)")
                .param("fundId", fundId)
                .param("accountId", accountId)
                .param("kind", kind.name())
                .update();
    }

    /** Thành viên hiện tại (chưa rời) của một quỹ đang hoạt động. */
    public boolean isActiveMember(long accountId, long fundId) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM fund_member m JOIN fund f ON f.id = m.fund_id
                            WHERE m.fund_id = :fundId AND m.account_id = :accountId
                              AND m.left_at IS NULL AND f.status = 'ACTIVE')
                        """)
                .param("fundId", fundId)
                .param("accountId", accountId)
                .query(Boolean.class)
                .single();
    }

    public Optional<FundRow> find(long fundId) {
        return jdbc.sql("SELECT id, kind, status FROM fund WHERE id = :id")
                .param("id", fundId)
                .query((rs, i) ->
                        new FundRow(rs.getLong("id"), Kind.valueOf(rs.getString("kind")), rs.getString("status")))
                .optional();
    }

    public List<ActiveFund> activeFundsOf(long accountId) {
        return jdbc.sql("""
                        SELECT f.id, f.kind,
                               (SELECT count(*) FROM fund_member x WHERE x.fund_id = f.id AND x.left_at IS NULL) AS members
                        FROM fund f JOIN fund_member m ON m.fund_id = f.id
                        WHERE m.account_id = :accountId AND m.left_at IS NULL AND f.status = 'ACTIVE'
                        ORDER BY f.kind = 'PERSONAL' DESC, f.id
                        """)
                .param("accountId", accountId)
                .query((rs, i) ->
                        new ActiveFund(rs.getLong("id"), Kind.valueOf(rs.getString("kind")), rs.getInt("members")))
                .list();
    }

    public Optional<Long> activeFundOf(long accountId, Kind kind) {
        return jdbc.sql("""
                        SELECT m.fund_id FROM fund_member m JOIN fund f ON f.id = m.fund_id
                        WHERE m.account_id = :accountId AND m.fund_kind = :kind
                          AND m.left_at IS NULL AND f.status = 'ACTIVE'
                        """)
                .param("accountId", accountId)
                .param("kind", kind.name())
                .query(Long.class)
                .optional();
    }

    public List<Long> activeMemberIds(long fundId) {
        return jdbc.sql(
                        "SELECT account_id FROM fund_member WHERE fund_id = :fundId AND left_at IS NULL ORDER BY joined_at")
                .param("fundId", fundId)
                .query(Long.class)
                .list();
    }

    public List<Membership> membershipsOf(long accountId) {
        return jdbc.sql("""
                        SELECT f.kind, m.joined_at, m.left_at
                        FROM fund_member m JOIN fund f ON f.id = m.fund_id
                        WHERE m.account_id = :accountId ORDER BY m.joined_at
                        """)
                .param("accountId", accountId)
                .query((rs, i) -> new Membership(
                        Kind.valueOf(rs.getString("kind")),
                        rs.getTimestamp("joined_at").toInstant(),
                        rs.getTimestamp("left_at") == null
                                ? null
                                : rs.getTimestamp("left_at").toInstant()))
                .list();
    }

    public List<Long> fundIdsOf(long accountId) {
        return jdbc.sql("SELECT fund_id FROM fund_member WHERE account_id = :accountId")
                .param("accountId", accountId)
                .query(Long.class)
                .list();
    }

    public void deleteMemberships(long accountId) {
        jdbc.sql("DELETE FROM fund_member WHERE account_id = :accountId")
                .param("accountId", accountId)
                .update();
    }

    /** Quỹ trong danh sách mà không còn thành viên nào (kể cả thành viên cũ). */
    public List<Long> withoutMembers(List<Long> fundIds) {
        if (fundIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT f.id FROM fund f
                        WHERE f.id IN (:ids) AND NOT EXISTS (SELECT 1 FROM fund_member m WHERE m.fund_id = f.id)
                        """).param("ids", fundIds).query(Long.class).list();
    }

    public void deleteFund(long fundId) {
        jdbc.sql("DELETE FROM fund WHERE id = :id").param("id", fundId).update();
    }
}
