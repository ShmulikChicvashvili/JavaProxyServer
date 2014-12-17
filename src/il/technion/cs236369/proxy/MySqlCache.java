/**
 *
 */

package il.technion.cs236369.proxy;


import java.sql.Connection;
import java.sql.SQLException;

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
		url,
		/**
		 * Header column
		 */
		header,
		/**
		 * Body column
		 */
		body,
		/**
		 * Last modified column
		 */
		lastmodified
	}
	
	
	
	/**
	 * @param connectionEstablisher
	 *            The class which handles the connection establishment
	 */
	public MySqlCache(MySqlConnectionEstablisher connectionEstablisher)
	{
		this.connectionEstablisher = connectionEstablisher;
	} /* (non-Javadoc) @see il.technion.cs236369.proxy.ICache#buildTable() */
	
	
	@Override
	public void buildTable() throws SQLException
	{
		try (
			Connection con = connectionEstablisher.getConnection();
			Statement statement = (Statement) con.createStatement())
		{
			@SuppressWarnings("nls")
			final String statementTable =
				String.format(
					"CREATE TABLE IF NOT EXISTS `%s`.`%s` "
						+ "(%s VARCHAR(255) PRIMARY KEY, "
						+ "%s TEXT, %s BLOB, %s VARCHAR(255)",
					connectionEstablisher.getDatabaseName(),
					connectionEstablisher.getTable(),
					Columns.url.toString().toLowerCase(),
					Columns.header.toString().toLowerCase(),
					Columns.body.toString().toLowerCase(),
					Columns.lastmodified.toString().toLowerCase());
			statement.execute(statementTable);
		}
	}
	
	
	/* (non-Javadoc) @see il.technion.cs236369.proxy.ICache#destroyTable() */
	@Override
	public void destroyTable() throws SQLException
	{
		// TODO Auto-generated method stub
		
	}
	
	
	
	/**
	 * The connection handler
	 */
	private final MySqlConnectionEstablisher connectionEstablisher;
	
}
