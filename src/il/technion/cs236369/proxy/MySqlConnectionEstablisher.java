/**
 *
 */

package il.technion.cs236369.proxy;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.mysql.jdbc.Statement;




/**
 * @author Shmulik
 *
 */
public class MySqlConnectionEstablisher
{
	/**
	 * @param driver
	 *            Driver name
	 * @param url
	 *            URL
	 * @param databaseName
	 *            Database name
	 * @param table
	 *            Database table
	 * @param userName
	 *            User name
	 * @param password
	 *            Password of user
	 * @throws SQLException
	 *             Exception
	 */
	public MySqlConnectionEstablisher(
		String driver,
		String url,
		String databaseName,
		String table,
		String userName,
		String password) throws SQLException
	{
		this.driver = driver;
		this.url = url;
		this.databaseName = databaseName;
		this.table = table;
		this.userName = userName;
		this.password = password;
		
		createDatabase();
	}


	@SuppressWarnings("nls")
	public final Connection getConnection() throws SQLException
	{
		try
		{
			Class.forName(driver);
		} catch (final ClassNotFoundException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Can't create mysql.jdbc driver");
		}
		
		final Connection $ =
			DriverManager.getConnection(url
				+ "?user="
				+ userName
				+ "&password="
				+ password);
		
		return $;
		
	}


	/**
	 * @return the databaseName
	 */
	public String getDatabaseName()
	{
		return databaseName;
	}


	/**
	 * @return the table
	 */
	public String getTable()
	{
		return table;
	}


	/**
	 * @throws SQLException
	 *             Exception
	 */
	private void createDatabase() throws SQLException
	{
		try (
			Connection con = getConnection();
			Statement statement = (Statement) con.createStatement())
		{
			statement
				.executeUpdate("CREATE SCHEMA IF NOT EXISTS " + databaseName); //$NON-NLS-1$
		}
	}



	/**
	 * Driver
	 */
	private final String driver;
	
	/**
	 * URL
	 */
	private final String url;
	
	/**
	 * Database Name
	 */
	private final String databaseName;

	/**
	 * Database table
	 */
	private final String table;

	/**
	 * User name
	 */
	private final String userName;

	/**
	 * User's password
	 */
	private final String password;

}
