package com.emme.shared.search.postgres;

import com.emme.shared.search.HybridSearch;
import com.emme.shared.search.SearchTarget;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL implementation of {@link HybridSearch} using pgvector, FTS, and RRF. */
@Component
public final class PostgresHybridSearch implements HybridSearch {

  private static final int BRANCH_LIMIT = 20;
  private static final int RRF_K = 60;

  private final JdbcClient jdbc;

  public PostgresHybridSearch(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Scored> search(
      SearchTarget target, UUID tenantId, List<Float> queryVector, String queryText, int limit) {
    if (queryVector == null || queryVector.isEmpty()) {
      return keywordOnly(target, tenantId, queryText, limit);
    }
    String sql =
        """
                WITH semantic AS (
                    SELECT id, RANK() OVER (ORDER BY embedding <=> CAST(:qvec AS vector)) AS r
                    FROM %1$s
                    WHERE tenant_id = :tenantId AND embedding IS NOT NULL %3$s
                    ORDER BY embedding <=> CAST(:qvec AS vector)
                    LIMIT %4$d
                ),
                keyword AS (
                    SELECT t.id, RANK() OVER (ORDER BY ts_rank_cd(t.%2$s, q) DESC) AS r
                    FROM %1$s t, plainto_tsquery('spanish', :question) q
                    WHERE t.tenant_id = :tenantId AND t.%2$s @@ q %6$s
                    ORDER BY r
                    LIMIT %4$d
                ),
                fused AS (SELECT id, r FROM semantic UNION ALL SELECT id, r FROM keyword)
                SELECT id, SUM(1.0 / (%5$d + r)) AS score
                FROM fused GROUP BY id ORDER BY score DESC, id LIMIT :k
                """
            .formatted(
                target.table(),
                target.tsvColumn(),
                target.extraPredicate(),
                BRANCH_LIMIT,
                RRF_K,
                aliasedPredicate(target));
    return jdbc.sql(sql)
        .param("qvec", toVectorLiteral(queryVector))
        .param("tenantId", tenantId)
        .param("question", queryText)
        .param("k", limit)
        .query((rs, i) -> new Scored(rs.getObject("id", UUID.class), rs.getDouble("score")))
        .list();
  }

  private List<Scored> keywordOnly(
      SearchTarget target, UUID tenantId, String queryText, int limit) {
    String sql =
        """
                SELECT t.id, ts_rank_cd(t.%2$s, q) AS score
                FROM %1$s t, plainto_tsquery('spanish', :question) q
                WHERE t.tenant_id = :tenantId AND t.%2$s @@ q %3$s
                ORDER BY score DESC, t.id LIMIT :k
                """
            .formatted(target.table(), target.tsvColumn(), aliasedPredicate(target));
    return jdbc.sql(sql)
        .param("tenantId", tenantId)
        .param("question", queryText)
        .param("k", limit)
        .query((rs, i) -> new Scored(rs.getObject("id", UUID.class), rs.getDouble("score")))
        .list();
  }

  private static String aliasedPredicate(SearchTarget target) {
    return target.extraPredicate().isEmpty()
        ? ""
        : target.extraPredicate().replace("status", "t.status");
  }

  @Override
  public int updateEmbedding(SearchTarget target, UUID tenantId, UUID rowId, List<Float> vector) {
    if (vector == null || vector.isEmpty()) return 0;
    String sql =
        "UPDATE %s SET embedding = CAST(:v AS vector) WHERE tenant_id = :tenantId AND id = :id"
            .formatted(target.table());
    return jdbc.sql(sql)
        .param("v", toVectorLiteral(vector))
        .param("tenantId", tenantId)
        .param("id", rowId)
        .update();
  }

  @Override
  public List<UUID> idsMissingEmbedding(SearchTarget target, UUID tenantId, int limit) {
    return jdbc.sql(
            "SELECT id FROM %s WHERE tenant_id = :tenantId AND embedding IS NULL ORDER BY id LIMIT :l"
                .formatted(target.table()))
        .param("tenantId", tenantId)
        .param("l", limit)
        .query(UUID.class)
        .list();
  }

  @Override
  public long countMissingEmbedding(SearchTarget target, UUID tenantId) {
    return jdbc.sql(
            "SELECT count(*) FROM %s WHERE tenant_id = :tenantId AND embedding IS NULL"
                .formatted(target.table()))
        .param("tenantId", tenantId)
        .query(Long.class)
        .single();
  }

  public static String toVectorLiteral(List<Float> vector) {
    StringJoiner joiner = new StringJoiner(",", "[", "]");
    for (Float value : vector) joiner.add(Float.toString(value));
    return joiner.toString();
  }
}
