package il.technion.cs236369.proxy;

import java.sql.SQLException;

public interface ICache {
	void destroyTable() throws SQLException;

	void buildTable() throws SQLException;
}
