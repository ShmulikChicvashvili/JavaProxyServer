
package il.technion.cs236369.proxy;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Properties;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
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
import org.apache.http.protocol.ResponseContent;
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
@SuppressWarnings("nls")
public class HttpProxy extends AbstractHttpProxy
{
	@SuppressWarnings("javadoc")
	private class ProxyHandler implements HttpRequestHandler
	{

		/* (non-Javadoc) @see
		 * org.apache.http.protocol.HttpRequestHandler#handle(
		 * org.apache.http.HttpRequest, org.apache.http.HttpResponse,
		 * org.apache.http.protocol.HttpContext) */
		@Override
		public void handle(
			HttpRequest request,
			HttpResponse response,
			HttpContext context) throws HttpException, IOException
		{
			final HttpHost host =
				new HttpHost(request.getHeaders("host")[0].getValue(), 80);
			System.out.println("Host:" + host.getHostName());

			if (cache.isExist(request.getRequestLine().getUri()))
			{
				getFromCache(request, response);
				responseSetContentHeaders(response, context);
				System.err.println("Getting from Cache");
			}

			else
			{
				try (
					final Socket sock =
						sockFactory.createSocket(
							host.getHostName(),
							host.getPort());
					HttpClientConnection conn =
						DefaultBHttpClientConnectionFactory.INSTANCE
							.createConnection(sock))
				{
					getResponseFromServer(request, response, context, conn);
					if (Utils.isAcceptingGzip(request))
					{
						
						responseGZipEntity(
							response,
							(ByteArrayEntity) response.getEntity());
						
					}
					responseSetContentHeaders(response, context);
					
					insertIntoCache(request, response);

				}
			}
			System.out.println("Request:");
			System.out.println(request.toString());
			System.out.println("Response:");
			System.out.println(response.toString());
		}


		/**
		 * @param request
		 * @param response
		 * @throws IOException
		 */
		private void getFromCache(HttpRequest request, HttpResponse response)
			throws IOException
		{
			System.out.println("Getting resource from cache");
			final DBRecord recordResponse =
				cache.get(request.getRequestLine().getUri());
			
			System.out.println("Printing headers");
			System.out.println(recordResponse.getHeader());

			Utils.stringToResponse(recordResponse.getHeader(), response);
			response
				.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
			try
			{
				EntityUtils.updateEntity(
					response,
					new ByteArrayEntity(recordResponse.getBody().getBytes(
						1,
						(int) recordResponse.getBody().length())));
			} catch (final SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		/**
		 * @param request
		 * @param response
		 * @param context
		 * @param conn
		 * @throws IOException
		 * @throws HttpException
		 */
		@SuppressWarnings("boxing")
		private void getResponseFromServer(
			HttpRequest request,
			HttpResponse response,
			HttpContext context,
			HttpClientConnection conn) throws IOException, HttpException
		{
			final HttpRequestExecutor httpExecutor = new HttpRequestExecutor();
			final HttpResponse serverResponse =
				httpExecutor.execute(request, conn, context);
			System.out.println("Server response status: "
				+ serverResponse.getStatusLine().toString());
			response.setStatusLine(serverResponse.getStatusLine());
			response.setHeaders(serverResponse.getAllHeaders());

			if (serverResponse.getEntity() != null)
			{

				System.out.println("Original length");
				System.out.println(serverResponse
					.getEntity()
					.getContentLength());

				final ByteArrayEntity entity =
					new ByteArrayEntity(EntityUtils.toByteArray(serverResponse
						.getEntity()));
				entity.setChunked(serverResponse.getEntity().isChunked());
				EntityUtils.updateEntity(response, entity);
				EntityUtils.consume(serverResponse.getEntity());
			}
		}


		/**
		 * @param request
		 * @param response
		 * @throws SerialException
		 * @throws SQLException
		 * @throws IOException
		 * @throws RuntimeException
		 */
		private
			void
			insertIntoCache(HttpRequest request, HttpResponse response)
		{
			if (response.getStatusLine().getStatusCode() >= 200
				&& response.getStatusLine().getStatusCode() <= 399)
			{
				try
				{
					final Blob body =
						new SerialBlob(EntityUtils.toByteArray(response
							.getEntity()));
					System.out.println("entity response");
					System.out.println(response.getEntity().getContentLength());
					if (body.length() < 65535)
					{

						System.out.println("Inserting body of size");
						System.out.println(body.length());
						cache.insert(new DBRecord(
							request.getRequestLine().getUri(),
							Utils.responseToString(response),
							body,
							""));

					}

				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}


		/**
		 * @param response
		 * @param entity
		 * @throws IOException
		 * @throws ParseException
		 */
		private void responseGZipEntity(
			HttpResponse response,
			ByteArrayEntity entity) throws IOException, ParseException
		{
			final byte[] ent =
				GZipHandler.compress(EntityUtils.toString(entity));
			entity = new ByteArrayEntity(ent);
			responseSetHeaders(response, entity);
			assert response
				.getFirstHeader("Content-Length")
				.getValue()
				.equals(entity.getContentLength());
			EntityUtils.updateEntity(response, entity);
		}


		/**
		 * @param response
		 * @param context
		 * @throws HttpException
		 * @throws IOException
		 */
		private void responseSetContentHeaders(
			HttpResponse response,
			HttpContext context) throws HttpException, IOException
		{
			response.removeHeaders("Content-Length");
			response.removeHeaders("Transfer-encoding");
			final HttpProcessor proc =
				HttpProcessorBuilder
					.create()
					.add(new ResponseContent())
					.build();
			proc.process(response, context);
		}


		/**
		 * @param response
		 * @param ent
		 */
		private void responseSetHeaders(HttpResponse response, HttpEntity ent)
		{
			if (response.containsHeader("Content-Encoding"))
			{
				response.removeHeaders("Content-Encoding");
			}
			response.addHeader("Content-Encoding", "gzip");
			// if (response.containsHeader("Content-Length"))
			// {
			// response.removeHeaders("Content-Length");
			// }
			// response.addHeader(
			// "Content-Length",
			// new Long(ent.getContentLength()).toString());
			// if (response.containsHeader("Transfer-Encoding"))
			// {
			//
			// response.removeHeaders("Transfer-Encoding");
			// }
		}
		
	}



	@SuppressWarnings({ "javadoc", "resource" })
	public static void main(String[] args) throws Exception
	{
		final PrintStream out =
			new PrintStream(new FileOutputStream("output.txt"));
		System.setOut(out);
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
		try
		{
			cache =
				new MySqlCache(new MySqlConnectionEstablisher(
					dbDriver,
					dbURL,
					dbName,
					tblName,
					dbUsername,
					dbPassword));
			cache.buildTable();
		} catch (final SQLException e)
		{
			// TODO Auto-generated catch block
			System.err.println("Failed to connect to the DB");
			e.printStackTrace();
		}
	}


	@Override
	public void bind() throws IOException
	{
		// Add your code here
	}


	@SuppressWarnings({ "synthetic-access" })
	@Override
	public void start()
	{
		final HttpProcessor proc = HttpProcessorBuilder.create()
		// .add(new ResponseContent(true))
			.add(new HttpRequestInterceptor()
			{

				@Override
				public void process(HttpRequest arg0, HttpContext arg1)
					throws HttpException,
					IOException
				{
					if (arg0.containsHeader("Connection"))
					{
						arg0.removeHeaders("Connection");
					}
					arg0.addHeader("Connection", "close");
				}
			})
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


	@SuppressWarnings({ "static-method", "javadoc" })
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



	ICache cache;
}
