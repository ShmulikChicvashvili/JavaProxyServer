
package il.technion.cs236369.proxy.test;


import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import il.technion.cs236369.proxy.HttpProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;




public class HttpProxyTestModule extends AbstractModule
{
	
	public HttpProxyTestModule()
	{
		this(new Properties());
	}
	
	
	public HttpProxyTestModule(Properties properties)
	{
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	
	public ServerSocket getMockedServerSocket()
	{
		return mockedServerSocket;
	}
	
	
	public ServerSocketFactory getMockedServerSocketFactory()
	{
		return mockedServerSocketFactory;
	}
	
	
	public SocketFactory getMockedSockFactory()
	{
		return mockedSockFactory;
	}
	
	
	public HttpProxyTestModule setProperty(String name, String value)
	{
		properties.setProperty(name, value);
		return this;
	}
	
	
	private Properties getDefaultProperties()
	{
		final Properties defaultProps = new Properties();
		
		defaultProps.setProperty("httproxy.db.driver", "com.mysql.jdbc.Driver");
		defaultProps.setProperty(
			"httproxy.db.url",
			"jdbc:mysql://127.0.0.1:3306/");
		defaultProps.setProperty("httproxy.db.name", "proxy");
		defaultProps.setProperty("httproxy.db.table", "cache");
		defaultProps.setProperty("httproxy.db.username", "root");
		defaultProps.setProperty("httproxy.db.password", "root");
		
		// do not change this param
		defaultProps.setProperty("httproxy.net.port", "8080");
		
		return defaultProps;
	}
	
	
	@Override
	protected void configure()
	{
		Names.bindProperties(binder(), properties);
		
		try
		{
			when(mockedServerSocketFactory.createServerSocket(anyInt()))
				.thenReturn(mockedServerSocket);
		} catch (final IOException e)
		{
			throw new AssertionError();
		}
		
		bind(ServerSocketFactory.class).toInstance(mockedServerSocketFactory);
		bind(SocketFactory.class).toInstance(mockedSockFactory);
		
		bind(HttpProxy.class).in(Scopes.SINGLETON);
	}
	
	
	
	private final Properties properties;
	
	private final ServerSocketFactory mockedServerSocketFactory =
		mock(ServerSocketFactory.class);
	
	private final SocketFactory mockedSockFactory = mock(SocketFactory.class);
	
	private final ServerSocket mockedServerSocket = mock(ServerSocket.class);
	
}
