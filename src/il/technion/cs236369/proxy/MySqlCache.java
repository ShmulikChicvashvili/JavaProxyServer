/**
 *
 */

package il.technion.cs236369.proxy;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;




/**
 * @author Shmulik
 *
 */
public class MySqlCache implements ICache
{

	/**
	 * @author Shmulik
	 *
	 *         Handling the columns in the database easily
	 */
	private enum Columns
	{
		/**
		 * URL column
		 */
		URL,
		/**
		 * Header column
		 */
		HEADER,
		/**
		 * Body column
		 */
		BODY,
		/**
		 * Last modified column
		 */
		LASTMODIFIED
	}



	/**
	 * @param connectionEstablisher
	 *            The class which handles the connection establishment
	 */
	public MySqlCache(MySqlConnectionEstablisher connectionEstablisher)
	{
		this.connectionEstablisher = connectionEstablisher;
		table = String.format("`%s`.`%s`", //$NON-NLS-1$
			connectionEstablisher.getDatabaseName(),
			connectionEstablisher.getTable());
	} /* (non-Javadoc) @see il.technion.cs236369.proxy.ICache#buildTable() */


	@Override
	public void buildTable() throws SQLException
	{
		try (
			Connection con = connectionEstablisher.getConnection();
			Statement statement = (Statement) con.createStatement())
		{
			final String createTable =
				String.format("CREATE TABLE IF NOT EXISTS %s " //$NON-NLS-1$
					+ "(%s VARCHAR(255) PRIMARY KEY, " //$NON-NLS-1$
					+ "%s TEXT, %s BLOB, %s VARCHAR(255))", //$NON-NLS-1$
					table,
					Columns.URL.toString().toLowerCase(),
					Columns.HEADER.toString().toLowerCase(),
					Columns.BODY.toString().toLowerCase(),
					Columns.LASTMODIFIED.toString().toLowerCase());
			statement.execute(createTable);
		}
	}


	/* (non-Javadoc) @see
	 * il.technion.cs236369.proxy.ICache#delete(java.lang.String) */
	@Override
	public SqlError delete(String url)
	{
		if (url == null) { return SqlError.INVALID_PARAMS; }

		if (!isExist(url)) { return SqlError.DOES_NOT_EXIST; }

		try (
			Connection con = connectionEstablisher.getConnection();
			PreparedStatement preparedStatement =
				(PreparedStatement) con.prepareStatement(""
					+ "DELETE FROM "
					+ table
					+ " WHERE "
					+ Columns.URL.toString().toLowerCase()
					+ "=?;"))
		{
			preparedStatement.setString(1, url);
			preparedStatement.executeUpdate();
		} catch (final SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return SqlError.SUCCESS;
	}


	/* (non-Javadoc) @see il.technion.cs236369.proxy.ICache#destroyTable() */
	@Override
	public void destroyTable() throws SQLException
	{
		try (
			Connection con = connectionEstablisher.getConnection();
			Statement statement = (Statement) con.createStatement())
		{
			final String destroyTable =
				String.format("DROP TABLE IF EXISTS %s", //$NON-NLS-1$
					table);
			statement.execute(destroyTable);
		}
	}


	/**
	 * @param url
	 *            URL
	 * @return The record with that url
	 */
	@Override
	public DBRecord get(String url)
	{
		if (url == null) { return null; }

		DBRecord $ = null;
		try (
			Connection con = connectionEstablisher.getConnection();
			@SuppressWarnings("nls")
			PreparedStatement preparedStatement =
				(PreparedStatement) con.prepareStatement(String.format(
					"SELECT * FROM %s WHERE %s = ?",
					table,
					Columns.URL.toString().toLowerCase())))
		{
			preparedStatement.setString(1, url.toLowerCase());
			final ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next())
			{
				$ =
					new DBRecord(
						resultSet.getString(Columns.URL
							.toString()
							.toLowerCase()),
						resultSet.getString(Columns.HEADER
							.toString()
							.toLowerCase()),
						resultSet
							.getBlob(Columns.BODY.toString().toLowerCase()),
						resultSet.getString(Columns.LASTMODIFIED
							.toString()
							.toLowerCase()));
			}
			resultSet.close();
		} catch (final SQLException e)
		{
			e.printStackTrace();
		}
		return $;
	}


	/**
	 * @param record
	 *            The record to insert
	 * @return SqlError
	 *
	 */
	@Override
	public SqlError insert(DBRecord record)
	{
		if (record == null
			|| record.getUrl() == null
			|| record.getHeader() == null
			|| record.getBody() == null
			|| record.getLastModified() == null) { return SqlError.INVALID_PARAMS; }

		try (
			Connection con = connectionEstablisher.getConnection();
			@SuppressWarnings("nls")
			PreparedStatement preparedStatement =
				(PreparedStatement) con.prepareStatement(String
					.format(
						"REPLACE INTO %s "
							+ "(%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
						table,
						Columns.URL.toString().toLowerCase(),
						Columns.HEADER.toString().toLowerCase(),
						Columns.BODY.toString().toLowerCase(),
						Columns.LASTMODIFIED.toString().toLowerCase())))
		{
			preparedStatement.setString(1, record.getUrl().toLowerCase());
			preparedStatement.setString(2, record.getHeader());
			preparedStatement.setBlob(3, record.getBody());
			preparedStatement.setString(4, record.getLastModified());
			preparedStatement.executeUpdate();
		} catch (final SQLException e)
		{
			if (e.getErrorCode() == insertAlreadyExist) { return SqlError.ALREADY_EXIST; }

		}
		return SqlError.SUCCESS;
	}
	
	
	/**
	 * @param url
	 *            The url to check if exist
	 * @return Whether url exist or not
	 */
	@Override
	@SuppressWarnings("nls")
	public boolean isExist(String url)
	{
		if (url == null) { return false; }
		final String urlCheck = url.toLowerCase();
		boolean $ = false;
		try (
			Connection con = connectionEstablisher.getConnection();
			PreparedStatement preparedStatement =
				(PreparedStatement) con.prepareStatement("" //$NON-NLS-1$
					+ "SELECT * FROM " //$NON-NLS-1$
					+ table
					+ " WHERE " //$NON-NLS-1$
					+ Columns.URL.toString().toLowerCase()
					+ "=?;"))
		{
			preparedStatement.setString(1, urlCheck);
			final ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.first())
			{
				$ = true;
			}
			resultSet.close();
		} catch (final SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return $;
	}
	
	
	
	/**
	 * The connection handler
	 */
	private final MySqlConnectionEstablisher connectionEstablisher;

	/**
	 * The table's name in the database
	 */
	private final String table;
	
	/**
	 * The error code that returned when entry in database already exist
	 */
	private final int insertAlreadyExist = 1062;
}
