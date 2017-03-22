/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;

public class TestJdbc extends JdbcConnector {
    private static final Log LOG = LogFactory.getLog(TestJdbc.class.getName());

    /**
     * Test simple non-statement related class.  setSchema is tested elsewhere because there's work
     * to do for that one.  Similarly with getMetadata.
     *
     * @throws SQLException
     */
    @Test
    public void nonStatementCalls() throws SQLException {
        conn.clearWarnings();

        boolean isAutoCommit = conn.getAutoCommit();
        LOG.debug("Auto commit is " + isAutoCommit);

        String catalog = conn.getCatalog();
        LOG.debug("Catalog is " + catalog);

        String schema = conn.getSchema();
        LOG.debug("Schema is " + schema);

        int txnIsolation = conn.getTransactionIsolation();
        LOG.debug("Transaction Isolation is " + txnIsolation);

        SQLWarning warning = conn.getWarnings();
        while (warning != null) {
            LOG.debug("Found a warning: " + warning.getMessage());
            warning = warning.getNextWarning();
        }

        boolean closed = conn.isClosed();
        LOG.debug("Is closed? " + closed);

        boolean readOnly = conn.isReadOnly();
        LOG.debug("Is read only?" + readOnly);

        // Hive doesn't support catalogs, so setting this to whatever should be fine.  If we have
        // non-Hive systems trying to pass this setting it to a non-valid catalog name may cause
        // issues, so we may need to make this value configurable or something.
        conn.setCatalog("fred");
    }

    /**
     * Test simple DatabaseMetaData calls.  getColumns is tested elsewhere, as we need to call
     * that on a valid table.  Same with getFunctions.
     *
     * @throws SQLException
     */
    @Test
    public void databaseMetaDataCalls() throws SQLException {
        DatabaseMetaData md = conn.getMetaData();

        boolean boolrc = md.allTablesAreSelectable();
        LOG.debug("All tables are selectable? " + boolrc);

        String strrc = md.getCatalogSeparator();
        LOG.debug("Catalog separator " + strrc);

        strrc = md.getCatalogTerm();
        LOG.debug("Catalog term " + strrc);

        ResultSet rs = md.getCatalogs();
        while (rs.next()) {
            strrc = rs.getString(1);
            LOG.debug("Found catalog " + strrc);
        }

        Connection c = md.getConnection();

        int intrc = md.getDatabaseMajorVersion();
        LOG.debug("DB major version is " + intrc);

        intrc = md.getDatabaseMinorVersion();
        LOG.debug("DB minor version is " + intrc);

        strrc = md.getDatabaseProductName();
        LOG.debug("DB product name is " + strrc);

        strrc = md.getDatabaseProductVersion();
        LOG.debug("DB product version is " + strrc);

        intrc = md.getDefaultTransactionIsolation();
        LOG.debug("Default transaction isolation is " + intrc);

        intrc = md.getDriverMajorVersion();
        LOG.debug("Driver major version is " + intrc);

        intrc = md.getDriverMinorVersion();
        LOG.debug("Driver minor version is " + intrc);

        strrc = md.getDriverName();
        LOG.debug("Driver name is " + strrc);

        strrc = md.getDriverVersion();
        LOG.debug("Driver version is " + strrc);

        strrc = md.getExtraNameCharacters();
        LOG.debug("Extra name characters is " + strrc);

        strrc = md.getIdentifierQuoteString();
        LOG.debug("Identifier quote string is " + strrc);

        // In Hive 1.2 this always returns an empty RS
        rs = md.getImportedKeys("a", "b", "d");

        // In Hive 1.2 this always returns an empty RS
        rs = md.getIndexInfo("a", "b", "d", true, true);

        intrc = md.getJDBCMajorVersion();
        LOG.debug("JDBC major version is " + intrc);

        intrc = md.getJDBCMinorVersion();
        LOG.debug("JDBC minor version is " + intrc);

        intrc = md.getMaxColumnNameLength();
        LOG.debug("Maximum column name length is " + intrc);

        strrc = md.getNumericFunctions();
        LOG.debug("Numeric functions are " + strrc);

        // In Hive 1.2 this always returns an empty RS
        rs = md.getPrimaryKeys("a", "b", "d");

        // In Hive 1.2 this always returns an empty RS
        rs = md.getProcedureColumns("a", "b", "d", "e");

        strrc = md.getProcedureTerm();
        LOG.debug("Procedures are called " + strrc);

        // In Hive 1.2 this always returns an empty RS
        rs = md.getProcedures("a", "b", "d");

        strrc = md.getSchemaTerm();
        LOG.debug("Schemas are called " + strrc);

        rs = md.getSchemas();
        while (rs.next()) {
            strrc = rs.getString(1);
            LOG.debug("Found schema " + strrc);
        }

        strrc = md.getSearchStringEscape();
        LOG.debug("Search string escape is " + strrc);

        strrc = md.getStringFunctions();
        LOG.debug("String functions are " + strrc);

        strrc = md.getSystemFunctions();
        LOG.debug("System functions are " + strrc);

        rs = md.getTableTypes();
        while (rs.next()) {
            strrc = rs.getString(1);
            LOG.debug("Found table type " + strrc);
        }

        strrc = md.getTimeDateFunctions();
        LOG.debug("Time/date functions are " + strrc);

        rs = md.getTypeInfo();
        while (rs.next()) {
            strrc = rs.getString(1);
            LOG.debug("Found type " + strrc);
        }

        // In Hive 1.2 this always returns an empty RS
        rs = md.getUDTs("a", "b", "d", null);

        boolrc = md.supportsAlterTableWithAddColumn();
        LOG.debug("Supports alter table with add column? " + boolrc);

        boolrc = md.supportsAlterTableWithDropColumn();
        LOG.debug("Supports alter table with drop column? " + boolrc);

        boolrc = md.supportsBatchUpdates();
        LOG.debug("Supports batch updates? " + boolrc);

        boolrc = md.supportsCatalogsInDataManipulation();
        LOG.debug("Supports catalogs in data manipulation? " + boolrc);

        boolrc = md.supportsCatalogsInIndexDefinitions();
        LOG.debug("Supports catalogs in index definition? " + boolrc);

        boolrc = md.supportsCatalogsInPrivilegeDefinitions();
        LOG.debug("Supports catalogs in privilege definition? " + boolrc);

        boolrc = md.supportsCatalogsInProcedureCalls();
        LOG.debug("Supports catalogs in procedure calls? " + boolrc);

        boolrc = md.supportsCatalogsInTableDefinitions();
        LOG.debug("Supports catalogs in table definition? " + boolrc);

        boolrc = md.supportsColumnAliasing();
        LOG.debug("Supports column aliasing? " + boolrc);

        boolrc = md.supportsFullOuterJoins();
        LOG.debug("Supports full outer joins? " + boolrc);

        boolrc = md.supportsGroupBy();
        LOG.debug("Supports group by? " + boolrc);

        boolrc = md.supportsLimitedOuterJoins();
        LOG.debug("Supports limited outer joins? " + boolrc);

        boolrc = md.supportsMultipleResultSets();
        LOG.debug("Supports limited outer joins? " + boolrc);

        boolrc = md.supportsNonNullableColumns();
        LOG.debug("Supports non-nullable columns? " + boolrc);

        boolrc = md.supportsOuterJoins();
        LOG.debug("Supports outer joins? " + boolrc);

        boolrc = md.supportsPositionedDelete();
        LOG.debug("Supports positioned delete? " + boolrc);

        boolrc = md.supportsPositionedUpdate();
        LOG.debug("Supports positioned update? " + boolrc);

        boolrc = md.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        LOG.debug("Supports result set holdability? " + boolrc);

        boolrc = md.supportsResultSetType(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        LOG.debug("Supports result set type? " + boolrc);

        boolrc = md.supportsSavepoints();
        LOG.debug("Supports savepoints? " + boolrc);

        boolrc = md.supportsSchemasInDataManipulation();
        LOG.debug("Supports schemas in data manipulation? " + boolrc);

        boolrc = md.supportsSchemasInIndexDefinitions();
        LOG.debug("Supports schemas in index definitions? " + boolrc);

        boolrc = md.supportsSchemasInPrivilegeDefinitions();
        LOG.debug("Supports schemas in privilege definitions? " + boolrc);

        boolrc = md.supportsSchemasInProcedureCalls();
        LOG.debug("Supports schemas in procedure calls? " + boolrc);

        boolrc = md.supportsSchemasInTableDefinitions();
        LOG.debug("Supports schemas in table definitions? " + boolrc);

        boolrc = md.supportsSelectForUpdate();
        LOG.debug("Supports select for update? " + boolrc);

        boolrc = md.supportsStoredProcedures();
        LOG.debug("Supports stored procedures? " + boolrc);

        boolrc = md.supportsTransactions();
        LOG.debug("Supports transactions? " + boolrc);

        boolrc = md.supportsUnion();
        LOG.debug("Supports union? " + boolrc);

        boolrc = md.supportsUnionAll();
        LOG.debug("Supports union all? " + boolrc);

    }

    @Test
    public void setSchema() throws SQLException {
        try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)) {

            final String dbName = "bigtop_jdbc_test_db";

            final String tableName = "bigtop_jdbc_test_table";
            stmt.execute("drop table if exists " + tableName);

            stmt.execute("drop database if exists " + dbName + " cascade");
            stmt.execute("create database " + dbName);

            conn.setSchema(dbName);

            DatabaseMetaData md = conn.getMetaData();

            ResultSet rs = md.getSchemas(null, dbName);

            while (rs.next()) {
                String schemaName = rs.getString(2);
                LOG.debug("Schema name is " + schemaName);
            }

            stmt.execute("create table " + tableName + " (i int, s varchar(32))");

            rs = md.getTables(null, dbName, tableName, null);
            while (rs.next()) {
                String tName = rs.getString(3);
                LOG.debug("Schema name is " + tName);
            }

            rs = md.getColumns(null, dbName, tableName, "i");
            while (rs.next()) {
                String colName = rs.getString(4);
                LOG.debug("Schema name is " + colName);
            }

            rs = md.getFunctions(null, dbName, "foo");
            while (rs.next()) {
                String funcName = rs.getString(3);
                LOG.debug("Schema name is " + funcName);
            }
        }
    }

    @Test
    public void statement() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.cancel();
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.clearWarnings();

            final String tableName = "bigtop_jdbc_statement_test_table";

            stmt.execute("drop table if exists " + tableName);
            stmt.execute("create table " + tableName + " (a int, b varchar(32))");

            stmt.executeUpdate("insert into " + tableName + " values (1, 'abc'), (2, 'def')");

            int intrc = stmt.getUpdateCount();
            LOG.debug("Update count is " + intrc);

            ResultSet rs = stmt.executeQuery("select * from " + tableName);
            while (rs.next()) {
                LOG.debug("Fetched " + rs.getInt(1) + "," + rs.getString(2));
            }

            Connection localConn = stmt.getConnection();

            intrc = stmt.getFetchDirection();
            LOG.debug("Fetch direction is " + intrc);

            intrc = stmt.getFetchSize();
            LOG.debug("Fetch size is " + intrc);

            intrc = stmt.getMaxRows();
            LOG.debug("max rows is " + intrc);

            boolean boolrc = stmt.getMoreResults();
            LOG.debug("more results is " + boolrc);

            intrc = stmt.getQueryTimeout();
            LOG.debug("query timeout is " + intrc);

            stmt.execute("select * from " + tableName);
            rs = stmt.getResultSet();
            while (rs.next()) {
                LOG.debug("Fetched " + rs.getInt(1) + "," + rs.getString(2));
            }

            intrc = stmt.getResultSetType();
            LOG.debug("result set type is " + intrc);

            SQLWarning warning = stmt.getWarnings();
            while (warning != null) {
                LOG.debug("Found a warning: " + warning.getMessage());
                warning = warning.getNextWarning();
            }

            boolrc = stmt.isClosed();
            LOG.debug("is closed " + boolrc);

            boolrc = stmt.isCloseOnCompletion();
            LOG.debug("is close on completion " + boolrc);

            boolrc = stmt.isPoolable();
            LOG.debug("is poolable " + boolrc);

            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
            stmt.setFetchSize(500);
            stmt.setMaxRows(500);
        }
    }

    @Test
    public void preparedStmtAndResultSet() throws SQLException {
        final String tableName = "bigtop_jdbc_psars_test_table";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("drop table if exists " + tableName);
            stmt.execute("create table " + tableName + " (bo boolean, ti tinyint, db double, fl float, " +
                    "i int, lo bigint, sh smallint, st varchar(32))");
        }

        // NOTE Hive 1.2 theoretically support binary, Date & Timestamp in JDBC, but I get errors when I
        // try to put them in the query.
        try (PreparedStatement ps = conn.prepareStatement("insert into " + tableName +
                " values (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setBoolean(1, true);
            ps.setByte(2, (byte) 1);
            ps.setDouble(3, 3.141592654);
            ps.setFloat(4, 3.14f);
            ps.setInt(5, 3);
            ps.setLong(6, 10L);
            ps.setShort(7, (short) 20);
            ps.setString(8, "abc");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("insert into " + tableName + " (i, st) " +
                "values(?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            ps.setNull(1, Types.INTEGER);
            ps.setObject(2, "mary had a little lamb");
            ps.executeUpdate();
            ps.setNull(1, Types.INTEGER, null);
            ps.setString(2, "its fleece was white as snow");
            ps.clearParameters();
            ps.setNull(1, Types.INTEGER, null);
            ps.setString(2, "its fleece was white as snow");
            ps.execute();

        }

        try (Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("select * from " + tableName);

            ResultSetMetaData md = rs.getMetaData();

            int colCnt = md.getColumnCount();
            LOG.debug("Column count is " + colCnt);

            for (int i = 1; i <= colCnt; i++) {
                LOG.debug("Looking at column " + i);
                String strrc = md.getColumnClassName(i);
                LOG.debug("Column class name is " + strrc);

                int intrc = md.getColumnDisplaySize(i);
                LOG.debug("Column display size is " + intrc);

                strrc = md.getColumnLabel(i);
                LOG.debug("Column label is " + strrc);

                strrc = md.getColumnName(i);
                LOG.debug("Column name is " + strrc);

                intrc = md.getColumnType(i);
                LOG.debug("Column type is " + intrc);

                strrc = md.getColumnTypeName(i);
                LOG.debug("Column type name is " + strrc);

                intrc = md.getPrecision(i);
                LOG.debug("Precision is " + intrc);

                intrc = md.getScale(i);
                LOG.debug("Scale is " + intrc);

                boolean boolrc = md.isAutoIncrement(i);
                LOG.debug("Is auto increment? " + boolrc);

                boolrc = md.isCaseSensitive(i);
                LOG.debug("Is case sensitive? " + boolrc);

                boolrc = md.isCurrency(i);
                LOG.debug("Is currency? " + boolrc);

                intrc = md.getScale(i);
                LOG.debug("Scale is " + intrc);

                intrc = md.isNullable(i);
                LOG.debug("Is nullable? " + intrc);

                boolrc = md.isReadOnly(i);
                LOG.debug("Is read only? " + boolrc);

            }

            while (rs.next()) {
                LOG.debug("bo = " + rs.getBoolean(1));
                LOG.debug("bo = " + rs.getBoolean("bo"));
                LOG.debug("ti = " + rs.getByte(2));
                LOG.debug("ti = " + rs.getByte("ti"));
                LOG.debug("db = " + rs.getDouble(3));
                LOG.debug("db = " + rs.getDouble("db"));
                LOG.debug("fl = " + rs.getFloat(4));
                LOG.debug("fl = " + rs.getFloat("fl"));
                LOG.debug("i = " + rs.getInt(5));
                LOG.debug("i = " + rs.getInt("i"));
                LOG.debug("lo = " + rs.getLong(6));
                LOG.debug("lo = " + rs.getLong("lo"));
                LOG.debug("sh = " + rs.getShort(7));
                LOG.debug("sh = " + rs.getShort("sh"));
                LOG.debug("st = " + rs.getString(8));
                LOG.debug("st = " + rs.getString("st"));
                LOG.debug("tm = " + rs.getObject(8));
                LOG.debug("tm = " + rs.getObject("st"));
                LOG.debug("tm was null " + rs.wasNull());
            }
            LOG.debug("bo is column " + rs.findColumn("bo"));

            int intrc = rs.getConcurrency();
            LOG.debug("concurrency " + intrc);

            intrc = rs.getFetchDirection();
            LOG.debug("fetch direction " + intrc);

            intrc = rs.getType();
            LOG.debug("type " + intrc);

            Statement copy = rs.getStatement();

            SQLWarning warning = rs.getWarnings();
            while (warning != null) {
                LOG.debug("Found a warning: " + warning.getMessage());
                warning = warning.getNextWarning();
            }
            rs.clearWarnings();
        }
    }
}
