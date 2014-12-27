
package il.technion.cs236369.proxy;


import java.sql.SQLException;




public interface ICache
{
	public SqlError delete(String url);
	
	
	public DBRecord get(String url);


	public SqlError insert(DBRecord record);


	public boolean isExist(String url);
	
	
	void buildTable() throws SQLException;
	
	
	void destroyTable() throws SQLException;
}
