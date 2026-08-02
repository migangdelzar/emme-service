package com.emme.shared.persistence.jdbc;

/** Signals a failure raised while executing work on a managed JDBC connection. */
public final class JdbcConnectionExecutionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public JdbcConnectionExecutionException(Throwable cause) {
    super("JDBC connection operation failed", cause);
  }
}
