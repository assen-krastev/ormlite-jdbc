package com.j256.ormlite.jdbc;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Test;

import com.j256.ormlite.BaseJdbcTest;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.GeneratedKeyHolder;

public class JdbcDatabaseConnectionTest extends BaseJdbcTest {

	@Test
	public void testQueryForLong() throws Exception {
		DatabaseConnection databaseConnection = connectionSource.getReadOnlyConnection();
		try {
			Dao<Foo, Object> dao = createDao(Foo.class, true);
			Foo foo = new Foo();
			long id = 21321321L;
			foo.id = id;
			assertEquals(1, dao.create(foo));
			assertEquals(id, databaseConnection.queryForLong("select id from foo"));
		} finally {
			connectionSource.releaseConnection(databaseConnection);
		}
	}

	@Test(expected = SQLException.class)
	public void testQueryForLongNoResult() throws Exception {
		DatabaseConnection databaseConnection = connectionSource.getReadOnlyConnection();
		try {
			createDao(Foo.class, true);
			databaseConnection.queryForLong("select id from foo");
		} finally {
			connectionSource.releaseConnection(databaseConnection);
		}
	}

	@Test(expected = SQLException.class)
	public void testQueryForLongTooManyResults() throws Exception {
		DatabaseConnection databaseConnection = connectionSource.getReadOnlyConnection();
		try {
			Dao<Foo, Object> dao = createDao(Foo.class, true);
			Foo foo = new Foo();
			long id = 21321321L;
			foo.id = id;
			// insert twice
			assertEquals(1, dao.create(foo));
			assertEquals(1, dao.create(foo));
			databaseConnection.queryForLong("select id from foo");
		} finally {
			connectionSource.releaseConnection(databaseConnection);
		}
	}

	@Test
	public void testUpdateReleaseConnection() throws Exception {
		Connection connection = createMock(Connection.class);
		JdbcDatabaseConnection jdc = new JdbcDatabaseConnection(connection);
		String statement = "statement";
		PreparedStatement prepStmt = createMock(PreparedStatement.class);
		expect(connection.prepareStatement(statement)).andReturn(prepStmt);
		expect(prepStmt.executeUpdate()).andReturn(1);
		// should close the statement
		prepStmt.close();
		replay(connection, prepStmt);
		jdc.update(statement, new Object[0], new FieldType[0]);
		verify(connection, prepStmt);
	}

	@Test
	public void testInsertReleaseConnection() throws Exception {
		Connection connection = createMock(Connection.class);
		PreparedStatement prepStmt = createMock(PreparedStatement.class);
		GeneratedKeyHolder keyHolder = createMock(GeneratedKeyHolder.class);
		ResultSet resultSet = createMock(ResultSet.class);
		ResultSetMetaData metaData = createMock(ResultSetMetaData.class);

		JdbcDatabaseConnection jdc = new JdbcDatabaseConnection(connection);
		String statement = "statement";
		expect(connection.prepareStatement(statement, 1)).andReturn(prepStmt);
		expect(prepStmt.executeUpdate()).andReturn(1);
		expect(prepStmt.getGeneratedKeys()).andReturn(resultSet);
		expect(resultSet.getMetaData()).andReturn(metaData);
		expect(resultSet.next()).andReturn(false);
		// should close the statement
		prepStmt.close();
		replay(connection, prepStmt, keyHolder, resultSet);
		jdc.insert(statement, new Object[0], new FieldType[0], keyHolder);
		verify(connection, prepStmt, keyHolder, resultSet);
	}

	@Test
	public void testQueryForOneReleaseConnection() throws Exception {
		Connection connection = createMock(Connection.class);
		PreparedStatement prepStmt = createMock(PreparedStatement.class);
		GeneratedKeyHolder keyHolder = createMock(GeneratedKeyHolder.class);
		ResultSet resultSet = createMock(ResultSet.class);
		@SuppressWarnings("unchecked")
		GenericRowMapper<Foo> rowMapper = createMock(GenericRowMapper.class);

		JdbcDatabaseConnection jdc = new JdbcDatabaseConnection(connection);
		String statement = "statement";
		expect(connection.prepareStatement(statement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).andReturn(
				prepStmt);
		expect(prepStmt.executeQuery()).andReturn(resultSet);
		expect(resultSet.next()).andReturn(false);
		expect(prepStmt.getMoreResults()).andReturn(false);
		// should close the statement
		prepStmt.close();
		replay(connection, prepStmt, keyHolder, resultSet, rowMapper);
		jdc.queryForOne(statement, new Object[0], new FieldType[0], rowMapper);
		verify(connection, prepStmt, keyHolder, resultSet, rowMapper);
	}

	@Test
	public void testQueryKeyHolderNoKeys() throws Exception {
		DatabaseConnection databaseConnection = connectionSource.getReadOnlyConnection();
		try {
			createDao(Foo.class, true);
			GeneratedKeyHolder keyHolder = createMock(GeneratedKeyHolder.class);
			databaseConnection.insert("insert into foo (id) values (1)", new Object[0], new FieldType[0], keyHolder);
		} finally {
			connectionSource.releaseConnection(databaseConnection);
		}
	}

	protected static class Foo {
		@DatabaseField
		public long id;
		Foo() {
		}
	}
}