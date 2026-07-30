package com.emme.shared.search;

/**
 * Tables that carry an in-row {@code embedding vector} + generated tsvector column. Table/column
 * names are enum-bound constants — never user input — so they can be interpolated into SQL safely.
 */
public enum SearchTarget {
  DOCUMENT_CHUNK("document_chunk", "content_tsv", ""),
  CATALOG_ITEM("catalog_item", "search_tsv", "AND status = 'ACTIVE'"),
  CATALOG_ITEM_IMAGE("catalog_item_image", "caption_tsv", "");

  private final String table;
  private final String tsvColumn;
  private final String extraPredicate;

  SearchTarget(String table, String tsvColumn, String extraPredicate) {
    this.table = table;
    this.tsvColumn = tsvColumn;
    this.extraPredicate = extraPredicate;
  }

  public String table() {
    return table;
  }

  public String tsvColumn() {
    return tsvColumn;
  }

  public String extraPredicate() {
    return extraPredicate;
  }
}
