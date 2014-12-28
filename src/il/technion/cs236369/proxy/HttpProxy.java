
package il.technion.cs236369.proxy;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Properties;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.message.BasicHttpRequest;
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
			if (request.containsHeader("Cache-Control")
				&& request
					.getFirstHeader("Cache-Control")
					.getValue()
					.equals("no-cache"))
			{
				genResponseFromServer(request, response, context);
			} else
			{
				genResponseFromCache(request, response, context);
			}

			System.out.println("Request:");
			System.out.println(request.toString());
			System.out.println("Response:");
			System.out.println(response.toString());
		}


		private void genResponseFromCache(
			HttpRequest request,
			HttpResponse response,
			HttpContext context) throws IOException, HttpException
		{
			final String url = request.getRequestLine().getUri();
			if (!cache.isExist(url))
			{
				genResponseFromServer(request, response, context);
				return;
			}

			final HttpHost host = getRequestHost(request);
			final DBRecord rec = cache.get(url);
			final HttpRequest validationRequest =
				new BasicHttpRequest("GET", url);

			validationRequest.addHeader(
				"If-Modified-Since",
				rec.getLastModified());
			validationRequest.addHeader("Connection", "close");
			validationRequest.addHeader("Host", host.getHostName());

			try (
				final Socket sock =
					sockFactory
						.createSocket(host.getHostName(), host.getPort());
				HttpClientConnection conn =
					DefaultBHttpClientConnectionFactory.INSTANCE
						.createConnection(sock))
			{
				sendRequestToServer(request, response, context, conn);
			}

			if (response.getStatusLine().getStatusCode() == 304)
			{
				sendCached(rec, response);
			} else if (response.getStatusLine().getStatusCode() == 200)
			{
				updateCache(request, response);
			} else
			{
				cache.delete(url);
			}
		}


		private void genResponseFromServer(
			HttpRequest request,
			HttpResponse response,
			HttpContext context) throws IOException, HttpException
		{
			final HttpHost host = getRequestHost(request);
			System.out.println("Host:" + host.getHostName());

			try (
				final Socket sock =
					sockFactory
						.createSocket(host.getHostName(), host.getPort());
				HttpClientConnection conn =
					DefaultBHttpClientConnectionFactory.INSTANCE
						.createConnection(sock))
			{
				sendRequestToServer(request, response, context, conn);
			}

			final boolean isZippedSucceeded =
				zipEntityToResponse(request, response);

			if (isZippedSucceeded)
			{
				updateCache(request, response);
			}
		}


		/**
		 * @param request
		 * @return
		 */
		private HttpHost getRequestHost(HttpRequest request)
		{
			System.out.println("request:");
			System.out.println(request.toString());
			System.out.println("Host header length: "
				+ request.getHeaders("Host").length);
			System.out.println("Host header exists: "
				+ request.containsHeader("Host"));
			assert request.containsHeader("Host");

			HttpHost host = null;
			if (!request.containsHeader("Host"))
			{
				try
				{
					final URI uri = new URI(request.getRequestLine().getUri());
					host = new HttpHost(uri.getHost(), 80);
				} catch (final URISyntaxException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
			{

				host =
					new HttpHost(request.getHeaders("Host")[0].getValue(), 80);
			}
			return host;
		}


		/**
		 * @param request
		 * @param response
		 * @throws IOException
		 */
		private void sendCached(DBRecord rec, HttpResponse response)
			throws IOException
		{
			System.out.println("Sending resource from cache");

			System.out.println("Printing headers");
			System.out.println(rec.getHeader());

			Utils.stringToResponse(rec.getHeader(), response);
			response
				.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
			try
			{
				EntityUtils.updateEntity(response, new ByteArrayEntity(rec
					.getBody()
					.getBytes(1, (int) rec.getBody().length())));
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
		private void sendRequestToServer(
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
		 * @param response
		 */
		private void setGzipHeader(HttpResponse response)
		{
			if (response.containsHeader("Content-Encoding"))
			{
				response.removeHeaders("Content-Encoding");
			}
			response.addHeader("Content-Encoding", "gzip");
		}


		/**
		 * @param request
		 * @param response
		 * @throws SerialException
		 * @throws SQLException
		 * @throws IOException
		 * @throws RuntimeException
		 */
		private void updateCache(HttpRequest request, HttpResponse response)
		{
			if (!request.getRequestLine().getMethod().equals("GET")) { return; }
			if (response.getStatusLine().getStatusCode() != 200) { return; }
			if (response.containsHeader("Cache-Control")
				&& response
					.getFirstHeader("Cache-Control")
					.getValue()
					.contains("no-store")) { return; }
			if (!response.containsHeader("Last-Modified")) { return; }
			if (response.containsHeader("Content-Encoding")
				&& !response
					.getFirstHeader("Content-Encoding")
					.getValue()
					.contains("gzip")) { return; }
			if (response.containsHeader("Transfer-Encoding")) { return; }

			try
			{
				final String url = request.getRequestLine().getUri();
				final String headers = Utils.responseToString(response);

				final byte[] initBody = "".getBytes();
				Blob body = new SerialBlob(initBody);

				if (response.getEntity() != null)
				{
					body =
						new SerialBlob(EntityUtils.toByteArray(response
							.getEntity()));

					System.out.println("entity response");
					System.out.println(response.getEntity().getContentLength());
				}

				final String lastModified =
					response.getFirstHeader("Last-Modified").getValue();

				if (body.length() >= 65535 || url.length() >= 256) { return; }

				System.out.println("Inserting body of size");
				System.out.println(body.length());

				cache.insert(new DBRecord(url, headers, body, lastModified));

			} catch (final Exception e)
			{
				e.printStackTrace();
			}

		}


		/**
		 * @param response
		 * @param entity
		 * @throws IOException
		 * @throws ParseException
		 */
		private boolean zipEntityToResponse(
			HttpRequest request,
			HttpResponse response) throws IOException, ParseException
		{
			ByteArrayEntity entity = (ByteArrayEntity) response.getEntity();

			if (entity == null) { return false; }

			if (!(Utils.isAcceptingGzip(request) && response != null && (!response
				.containsHeader("Content-Encoding") || !response
				.getFirstHeader("Content-Encoding")
				.getValue()
				.contains("gzip")))) { return false; }

			final byte[] ent =
				GZipHandler.compress(EntityUtils.toString(entity));

			if (ent == null) { return false; }

			entity = new ByteArrayEntity(ent);
			EntityUtils.updateEntity(response, entity);

			setGzipHeader(response);

			return true;
		}

	}



	@SuppressWarnings({ "javadoc", "resource" })
	public static void main(String[] args) throws Exception
	{
		// final PrintStream out =
		// new PrintStream(new FileOutputStream("output.txt"));
		// System.setOut(out);
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
				public void process(HttpRequest req, HttpContext context)
					throws HttpException,
					IOException
				{
					if (req.containsHeader("Connection"))
					{
						req.removeHeaders("Connection");
					}
					req.addHeader("Connection", "close");
				}
			})
			.add(new HttpResponseInterceptor()
			{

				@Override
				public void process(HttpResponse response, HttpContext context)
					throws HttpException,
					IOException
				{
					if (response.containsHeader("Content-Length"))
					{
						response.removeHeaders("Content-Length");
					}
					if (response.containsHeader("Transfer-encoding"))
					{
						response.removeHeaders("Transfer-encoding");
					}

				}
			})
			.add(new ResponseContent())
			.build();
		final UriHttpRequestHandlerMapper registry =
			new UriHttpRequestHandlerMapper();
		registry.register("*", new ProxyHandler());

		final HttpService httpService = new HttpService(proc, registry);

		try (
			ServerSocket serverSocket = srvSockFactory.createServerSocket(port))
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
				// throw new IOException();
			} catch (final IOException ignore)
			{
				System.err.println("HttoServerConnection shutdown failed");
			}
		}
	}



	ICache cache;
}
