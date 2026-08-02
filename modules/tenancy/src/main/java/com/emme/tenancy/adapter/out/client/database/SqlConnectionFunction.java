package com.emme.tenancy.adapter.out.client.database;

import java.sql.Connection;

@FunctionalInterface
interface SqlConnectionFunction<T> {

  T apply(Connection connection) throws Exception;
}
