package il.technion.cs236369.proxy;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public abstract class AbstractHttpProxy implements IHttpProxy {
	protected final SocketFactory sockFactory;
	protected final ServerSocketFactory srvSockFactory;
	protected final int port;
	protected final String dbURL;
	protected final String dbName;
	protected final String tblName;
	protected final String dbUsername;
	protected final String dbPassword;
	protected final String dbDriver;

	@Inject
	AbstractHttpProxy(SocketFactory sockFactory,
			ServerSocketFactory srvSockFactory,
			@Named("httproxy.net.port") int port,
			@Named("httproxy.db.url") String dbURL,
			@Named("httproxy.db.name") String dbName,
			@Named("httproxy.db.table") String tblName,
			@Named("httproxy.db.username") String dbUsername,
			@Named("httproxy.db.password") String dbPassword,
			@Named("httproxy.db.driver") String dbDriver)
			throws ClassNotFoundException {

		this.sockFactory = sockFactory;
		this.srvSockFactory = srvSockFactory;
		this.port = port;
		this.dbURL = dbURL;
		this.dbName = dbName;
		this.tblName = tblName;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		this.dbDriver = dbDriver;
	}
}
