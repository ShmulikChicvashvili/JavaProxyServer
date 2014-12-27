
package il.technion.cs236369.proxy;


import java.sql.SQLException;




public interface ICache
{
	public DBRecord get(String url);


	public SqlError insert(DBRecord record) throws SQLException;


	public boolean isExist(String url);


	public SqlError update(DBRecord record);


	void buildTable() throws SQLException;


	void destroyTable() throws SQLException;
}
