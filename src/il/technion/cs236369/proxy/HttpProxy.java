
package il.technion.cs236369.proxy;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;




/**
 * @author Shmulik and Eyal
 *
 *         Created at : 11:37 AM 3.12.14
 *
 */
public class HttpProxy extends AbstractHttpProxy
{

	@SuppressWarnings("javadoc")
	private class ProxyHandler implements HttpRequestHandler
	{

		/* (non-Javadoc) @see
		 * org.apache.http.protocol.HttpRequestHandler#handle(
		 * org.apache.http.HttpRequest, org.apache.http.HttpResponse,
		 * org.apache.http.protocol.HttpContext) */
		@SuppressWarnings("nls")
		@Override
		public void handle(
			HttpRequest request,
			HttpResponse response,
			HttpContext context) throws HttpException, IOException
		{
			final HttpHost host =
				new HttpHost(request.getHeaders("host")[0].getValue(), 80);
			
			System.out.println("Host:" + host.getHostName());
			try (
				final Socket sock =
					sockFactory
						.createSocket(host.getHostName(), host.getPort());
				HttpClientConnection conn =
					DefaultBHttpClientConnectionFactory.INSTANCE
						.createConnection(sock))
			{
				final HttpRequestExecutor httpExecutor =
					new HttpRequestExecutor();
				final HttpResponse serverResponse =
					httpExecutor.execute(request, conn, context);
				System.out.println("Server response status: "
					+ serverResponse.getStatusLine().toString());
				response.setStatusLine(serverResponse.getStatusLine());
				response.setHeaders(serverResponse.getAllHeaders());

				if (serverResponse.getEntity() != null)
				{
					final ByteArrayEntity entity =
						new ByteArrayEntity(
							EntityUtils.toByteArray(serverResponse.getEntity()));
					entity.setChunked(serverResponse.getEntity().isChunked());
					EntityUtils.updateEntity(response, entity);
				}
			}
			
			System.out.println("Request:");
			System.out.println(request.toString());
			System.out.println("Response:");
			System.out.println(response.toString());
		}
	}



	@SuppressWarnings({ "javadoc", "nls", "resource" })
	public static void main(String[] args) throws Exception
	{
		final Properties p = new Properties();
		p.load(new FileInputStream("config"));
		final Injector inj = Guice.createInjector(new HttpProxyModule(p));
		// Injector inj = Guice.createInjector(new HttpProxyModule());
		final IHttpProxy proxy = inj.getInstance(HttpProxy.class);
		proxy.bind();
		proxy.start();

	}


	@SuppressWarnings("javadoc")
	@Inject
	HttpProxy(
		SocketFactory sockFactory,
		ServerSocketFactory srvSockFactory,

		@Named("httproxy.net.port") int port,
		@Named("httproxy.db.url") String dbURL,
		@Named("httproxy.db.name") String dbName,
		@Named("httproxy.db.table") String tblName,
		@Named("httproxy.db.username") String dbUsername,
		@Named("httproxy.db.password") String dbPassword,
		@Named("httproxy.db.driver") String dbDriver)
		throws ClassNotFoundException
	{
		super(
			sockFactory,
			srvSockFactory,
			port,
			dbURL,
			dbName,
			tblName,
			dbUsername,
			dbPassword,
			dbDriver);
		// Add your code here
	}


	@Override
	public void bind() throws IOException
	{
		// Add your code here
	}


	@SuppressWarnings({ "synthetic-access", "nls" })
	@Override
	public void start()
	{
		final HttpProcessor proc = HttpProcessorBuilder.create()
		// .add(new ResponseContent(true))
			.build();
		final UriHttpRequestHandlerMapper registry =
			new UriHttpRequestHandlerMapper();
		registry.register("*", new ProxyHandler());

		final HttpService httpService = new HttpService(proc, registry);

		try (
			ServerSocket serverSocket =
				srvSockFactory.createServerSocket(port, port, null))
		{
			while (!Thread.interrupted())
			{
				@SuppressWarnings("resource")
				final Socket sock = serverSocket.accept();
				@SuppressWarnings("resource")
				final HttpServerConnection conn =
					DefaultBHttpServerConnectionFactory.INSTANCE
						.createConnection(sock);
				
				handleSingleRequest(httpService, conn);
			}
		} catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	@SuppressWarnings({ "static-method", "javadoc", "nls" })
	private void handleSingleRequest(
		final HttpService httpService,
		final HttpServerConnection conn)
	{
		System.err.flush();
		System.out.println("@@@ Handling new request @@@");
		final HttpContext context = new BasicHttpContext(null);
		try
		{
			while (!Thread.interrupted() && conn.isOpen())
			{
				System.err.flush();
				System.out.println("### handling same connection ###");
				httpService.handleRequest(conn, context);
			}
		} catch (final ConnectionClosedException ex)
		{
			System.err.println("Client closed connection...");
		} catch (final IOException ex)
		{
			System.err.println("I/O error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (final HttpException ex)
		{
			System.err.println("Unrecoverable HTTP protocol violation: "
				+ ex.getMessage());
		} finally
		{
			System.out.println("================================");
			try
			{
				conn.shutdown();
			} catch (final IOException ignore)
			{
				System.err.println("HttoServerConnection shutdown failed");
			}
		}
	}
}
