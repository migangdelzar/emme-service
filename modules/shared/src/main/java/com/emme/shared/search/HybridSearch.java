package com.emme.shared.search;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Hybrid retrieval: pgvector cosine + Spanish full-text, fused with Reciprocal Rank Fusion (score =
 * Σ 1/(60 + rank)). PostgreSQL-only — never invoked on the H2 test profile (callers catch and
 * degrade).
 *
 * <p>Explicit tenant_id predicates everywhere: the app connects as the table owner, which bypasses
 * RLS.
 */
@Component
public class HybridSearch {

  public record Scored(UUID id, double score) {}

  private static final int BRANCH_LIMIT = 20;
  private static final int RRF_K = 60;

  private final JdbcClient jdbc;

  public HybridSearch(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<Scored> search(
      SearchTarget t, UUID tenantId, List<Float> queryVec, String question, int k) {
    if (queryVec == null || queryVec.isEmpty()) {
      return keywordOnly(t, tenantId, question, k);
    }
    // %3$s = predicate for the unaliased semantic CTE, %6$s = alias-qualified
    // predicate for the keyword CTE (its table is aliased "t").
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
                t.table(),
                t.tsvColumn(),
                t.extraPredicate(),
                BRANCH_LIMIT,
                RRF_K,
                aliasedPredicate(t));
    return jdbc.sql(sql)
        .param("qvec", toVectorLiteral(queryVec))
        .param("tenantId", tenantId)
        .param("question", question)
        .param("k", k)
        .query((rs, i) -> new Scored(rs.getObject("id", UUID.class), rs.getDouble("score")))
        .list();
  }

  private List<Scored> keywordOnly(SearchTarget t, UUID tenantId, String question, int k) {
    String sql =
        """
                SELECT t.id, ts_rank_cd(t.%2$s, q) AS score
                FROM %1$s t, plainto_tsquery('spanish', :question) q
                WHERE t.tenant_id = :tenantId AND t.%2$s @@ q %3$s
                ORDER BY score DESC, t.id LIMIT :k
                """
            .formatted(t.table(), t.tsvColumn(), aliasedPredicate(t));
    return jdbc.sql(sql)
        .param("tenantId", tenantId)
        .param("question", question)
        .param("k", k)
        .query((rs, i) -> new Scored(rs.getObject("id", UUID.class), rs.getDouble("score")))
        .list();
  }

  /**
   * The enum predicate references bare column names; qualify them for queries that alias the table
   * as "t".
   */
  private static String aliasedPredicate(SearchTarget t) {
    return t.extraPredicate().isEmpty() ? "" : t.extraPredicate().replace("status", "t.status");
  }

  public int updateEmbedding(SearchTarget t, UUID rowId, List<Float> vec) {
    if (vec == null || vec.isEmpty()) return 0;
    String sql = "UPDATE %s SET embedding = CAST(:v AS vector) WHERE id = :id".formatted(t.table());
    return jdbc.sql(sql).param("v", toVectorLiteral(vec)).param("id", rowId).update();
  }

  public List<UUID> idsMissingEmbedding(SearchTarget t, int limit) {
    return jdbc.sql(
            "SELECT id FROM %s WHERE embedding IS NULL ORDER BY id LIMIT :l".formatted(t.table()))
        .param("l", limit)
        .query(UUID.class)
        .list();
  }

  public long countMissingEmbedding(SearchTarget t) {
    return jdbc.sql("SELECT count(*) FROM %s WHERE embedding IS NULL".formatted(t.table()))
        .query(Long.class)
        .single();
  }

  public static String toVectorLiteral(List<Float> vec) {
    StringJoiner sj = new StringJoiner(",", "[", "]");
    for (Float f : vec) sj.add(Float.toString(f));
    return sj.toString();
  }
}
