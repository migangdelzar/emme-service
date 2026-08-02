package com.emme.tenancy.adapter.out.client.database;

import java.sql.Connection;

@FunctionalInterface
interface SqlConnectionConsumer {

  void accept(Connection connection) throws Exception;
}
