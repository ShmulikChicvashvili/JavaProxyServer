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
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
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
 * Merabi Shmulik Chicvashvili 317655157 shmulikjkech@gmail.com <br>
 * Eyal Tolchinsky 311505564 eyal.tolchinsky@gmail.com
 * @author Shmulik and Eyal
 * 
 *         Created at : 11:37 AM 3.12.14
 *
 */
@SuppressWarnings("nls")
public class HttpProxy extends AbstractHttpProxy {
	private static void log(String s){
//		System.out.println(s);
	}
	@SuppressWarnings("javadoc")
	private class ProxyHandler implements HttpRequestHandler {
		/*
		 * (non-Javadoc) @see
		 * org.apache.http.protocol.HttpRequestHandler#handle(
		 * org.apache.http.HttpRequest, org.apache.http.HttpResponse,
		 * org.apache.http.protocol.HttpContext)
		 */
		@Override
		public void handle(HttpRequest request, HttpResponse response,
				HttpContext context) throws HttpException, IOException {
			if (request.containsHeader(HttpHeaders.CACHE_CONTROL)
					&& request.getFirstHeader(HttpHeaders.CACHE_CONTROL)
							.getValue().equals("no-cache")) {
				log("Generating response from server");
				genResponseFromServer(request, response, context);
			} else {
				log("Generating response from cache");
				genResponseFromCache(request, response, context);
			}

			log("Request:");
			log(request.toString());
			log("Response:");
			log(response.toString());
			log("");
		}

		private void genResponseFromCache(HttpRequest request,
				HttpResponse response, HttpContext context) throws IOException,
				HttpException {
			final String url = request.getRequestLine().getUri();
			if (!cache.isExist(url)) {
				log("URL " + url + " not found in cache.");
				log("Generating response from server instead");
				genResponseFromServer(request, response, context);
				return;
			}

			final HttpHost host = getRequestHost(request);
			final DBRecord rec = cache.get(url);

			final HttpRequest validationRequest = new BasicHttpRequest("GET",
					url, HttpVersion.HTTP_1_1);
			validationRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE,
					rec.getLastModified());
			validationRequest.addHeader(HttpHeaders.CONNECTION, "close");
			validationRequest.addHeader(HttpHeaders.HOST, host.getHostName());
			try (HttpClientConnection conn = DefaultBHttpClientConnectionFactory.INSTANCE
					.createConnection(sockFactory.createSocket(
							host.getHostName(), host.getPort()))) {
				sendRequestToServer(request, response, context, conn);
			}

			if (response.getStatusLine().getStatusCode() == 304) {
				dbRecordToResponse(rec, response);
			} else if (response.getStatusLine().getStatusCode() == 200) {
				handleResponseToCache(request, response);
			} else {
				cache.delete(url);
			}
		}

		private void genResponseFromServer(HttpRequest request,
				HttpResponse response, HttpContext context) throws IOException,
				HttpException {
			final HttpHost host = getRequestHost(request);
			log("Host:" + host.getHostName());

			try (HttpClientConnection conn = DefaultBHttpClientConnectionFactory.INSTANCE
					.createConnection(sockFactory.createSocket(
							host.getHostName(), host.getPort()))) {
				sendRequestToServer(request, response, context, conn);
			}

			handleResponseToCache(request, response);
		}

		private void handleResponseToCache(HttpRequest request,
				HttpResponse response) throws IOException {
			// we always return a compressed response
			final boolean isZippedSucceeded = zipEntityToResponse(request,
					response);

			// if 'no-store' try to delete
			String url = request.getRequestLine().getUri();
			if (response.containsHeader(HttpHeaders.CACHE_CONTROL)
					&& response.getFirstHeader(HttpHeaders.CACHE_CONTROL)
							.getValue().equals("no-store")) {
				if (cache.isExist(url)) {
					cache.delete(url);
				}
				return;
			}

			// if it's okay to store, and zip succeded update cache
			if (isZippedSucceeded) {
				updateCache(request, response);
			}
		}

		/**
		 * @param request
		 * @return
		 */
		private HttpHost getRequestHost(HttpRequest request) {
			HttpHost host = null;
			if (!request.containsHeader("Host")) {
				try {
					final URI uri = new URI(request.getRequestLine().getUri());
					host = new HttpHost(uri.getHost(), 80);
				} catch (final URISyntaxException e) {
					System.err
							.println("Couldn't convert request uri to URI object");
					e.printStackTrace();
				}
			} else {

				host = new HttpHost(request.getHeaders("Host")[0].getValue(),
						80);
			}
			return host;
		}

		/**
		 * @param request
		 * @param response
		 * @throws IOException
		 */
		private void dbRecordToResponse(DBRecord rec, HttpResponse response)
				throws IOException {
			log("Sending resource from cache");

			log("Printing headers:");
			log(rec.getHeader());

			response.setStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
			Utils.stringToResponse(rec.getHeader(), response);
			try {
				EntityUtils.updateEntity(response, new ByteArrayEntity(rec
						.getBody().getBytes(1, (int) rec.getBody().length())));
			} catch (final SQLException e) {
				System.err
						.println("Couldn't update response with the record in cache");
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
		private void sendRequestToServer(HttpRequest request,
				HttpResponse response, HttpContext context,
				HttpClientConnection conn) throws IOException, HttpException {

			assert (request.getRequestLine().getProtocolVersion()
					.equals(HttpVersion.HTTP_1_1));
			final HttpRequestExecutor httpExecutor = new HttpRequestExecutor();
			final HttpResponse serverResponse = httpExecutor.execute(request,
					conn, context);

			log("Server response status: "
					+ serverResponse.getStatusLine().toString());

			response.setStatusLine(serverResponse.getStatusLine());
			response.setHeaders(serverResponse.getAllHeaders());

			if (serverResponse.getEntity() != null) {
				log("Original length: "
						+ serverResponse.getEntity().getContentLength());

				final ByteArrayEntity entity = new ByteArrayEntity(
						EntityUtils.toByteArray(serverResponse.getEntity()));
				entity.setChunked(serverResponse.getEntity().isChunked());
				EntityUtils.updateEntity(response, entity);
				EntityUtils.consume(serverResponse.getEntity());
			}
		}

		/**
		 * @param response
		 */
		private void setGzipHeader(HttpResponse response) {
			if (response.containsHeader(HttpHeaders.CONTENT_ENCODING)) {
				response.removeHeaders(HttpHeaders.CONTENT_ENCODING);
			}
			response.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
		}

		/**
		 * @param request
		 * @param response
		 * @throws SerialException
		 * @throws SQLException
		 * @throws IOException
		 * @throws RuntimeException
		 */
		private void updateCache(HttpRequest request, HttpResponse response) {
			// assert there is no 'no-store'
			assert (!(response.containsHeader(HttpHeaders.CACHE_CONTROL) && response
					.getFirstHeader(HttpHeaders.CACHE_CONTROL).getValue()
					.equals("no-store")));

			if (!request.getRequestLine().getMethod().equals("GET")) {
				return;
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				return;
			}
			if (response.containsHeader(HttpHeaders.CACHE_CONTROL)
					&& response.getFirstHeader(HttpHeaders.CACHE_CONTROL)
							.getValue().contains("no-store")) {
				return;
			}
			if (!response.containsHeader(HttpHeaders.LAST_MODIFIED)) {
				return;
			}
			if (response.containsHeader(HttpHeaders.CONTENT_ENCODING)
					&& !response.getFirstHeader(HttpHeaders.CONTENT_ENCODING)
							.getValue().contains("gzip")) {
				return;
			}
			if (response.containsHeader(HttpHeaders.TRANSFER_ENCODING)) {
				return;
			}

			// check no-store
			if (response.containsHeader(HttpHeaders.CACHE_CONTROL)
					&& response.getFirstHeader(HttpHeaders.CACHE_CONTROL)
							.getValue().equals("no-store")) {
				return;
			}

			try {
				final String url = request.getRequestLine().getUri();
				final String headers = Utils.responseToString(response);

				final byte[] initBody = "".getBytes();
				Blob body = new SerialBlob(initBody);

				if (response.getEntity() != null) {
					body = new SerialBlob(EntityUtils.toByteArray(response
							.getEntity()));

					log("entity response length: "
							+ response.getEntity().getContentLength());
				}

				final String lastModified = response.getFirstHeader(
						HttpHeaders.LAST_MODIFIED).getValue();

				if (body.length() >= 65535 || url.length() >= 256) {
					return;
				}

				log("Inserting body of size: " + body.length());

				cache.insert(new DBRecord(url, headers, body, lastModified));

			} catch (final Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * @param response
		 * @param entity
		 * @throws IOException
		 * @throws ParseException
		 */
		private boolean zipEntityToResponse(HttpRequest request,
				HttpResponse response) throws IOException, ParseException {

			// if already zipped, then WIN for us
			if (response.containsHeader(HttpHeaders.CONTENT_ENCODING)
					&& response.getFirstHeader(HttpHeaders.CONTENT_ENCODING)
							.getValue().equals("gzip")) {
				return true;
			}

			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return false;
				// FIXME why not true for null entity? consider storing null
				// body in cache
			}

			if (!Utils.isAcceptingGzip(request)
					|| (response.containsHeader(HttpHeaders.CONTENT_ENCODING) && !response
							.getFirstHeader(HttpHeaders.CONTENT_ENCODING)
							.getValue().equals("gzip"))
					|| response.containsHeader(HttpHeaders.TRANSFER_ENCODING)) {
				return false;
			}

			final byte[] ent = GZipHandler.compress(EntityUtils
					.toString(entity));

			if (ent == null) {
				return false;
			}

			entity = new ByteArrayEntity(ent);
			EntityUtils.updateEntity(response, entity);

			setGzipHeader(response);

			return true;
		}

	}

	@SuppressWarnings({ "javadoc" })
	public static void main(String[] args) throws Exception {
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
	HttpProxy(SocketFactory sockFactory, ServerSocketFactory srvSockFactory,

	@Named("httproxy.net.port") int port,
			@Named("httproxy.db.url") String dbURL,
			@Named("httproxy.db.name") String dbName,
			@Named("httproxy.db.table") String tblName,
			@Named("httproxy.db.username") String dbUsername,
			@Named("httproxy.db.password") String dbPassword,
			@Named("httproxy.db.driver") String dbDriver)
			throws ClassNotFoundException {
		super(sockFactory, srvSockFactory, port, dbURL, dbName, tblName,
				dbUsername, dbPassword, dbDriver);
		// Add your code here
		try {
			cache = new MySqlCache(new MySqlConnectionEstablisher(dbDriver,
					dbURL, dbName, tblName, dbUsername, dbPassword));
			cache.buildTable();
		} catch (final SQLException e) {
			System.err.println("Failed to connect to the DB");
			e.printStackTrace();
		}
	}

	@Override
	public void bind() throws IOException {
		// Add your code here
	}

	@SuppressWarnings({ "synthetic-access" })
	@Override
	public void start() {
		final HttpProcessor proc = HttpProcessorBuilder
				.create()
				.add(new HttpRequestInterceptor() {

					@Override
					public void process(HttpRequest req, HttpContext context)
							throws HttpException, IOException {
						if (req.containsHeader(HttpHeaders.CONNECTION)) {
							req.removeHeaders(HttpHeaders.CONNECTION);
						}
						req.addHeader(HttpHeaders.CONNECTION, "close");
					}
				})
				.add(new HttpResponseInterceptor() {

					@Override
					public void process(HttpResponse response,
							HttpContext context) throws HttpException,
							IOException {
						if (response.containsHeader(HttpHeaders.CONTENT_LENGTH)) {
							response.removeHeaders(HttpHeaders.CONTENT_LENGTH);
						}
//						if (response
//								.containsHeader(HttpHeaders.TRANSFER_ENCODING)) {
//							response.removeHeaders(HttpHeaders.TRANSFER_ENCODING);
//						}

					}
				}).add(new ResponseContent())
				.add(new HttpResponseInterceptor() {

					@Override
					public void process(HttpResponse response,
							HttpContext context) throws HttpException,
							IOException {
						if (response.containsHeader(HttpHeaders.CONNECTION)) {
							response.removeHeaders(HttpHeaders.CONNECTION);
						}
						response.addHeader(HttpHeaders.CONNECTION, "close");

					}
				}).build();
		final UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
		registry.register("*", new ProxyHandler());

		final HttpService httpService = new HttpService(proc, registry);

		try (ServerSocket serverSocket = srvSockFactory
				.createServerSocket(port)) {
			while (!Thread.interrupted()) {
				final Socket sock = serverSocket.accept();
				final HttpServerConnection conn = DefaultBHttpServerConnectionFactory.INSTANCE
						.createConnection(sock);

				handleSingleRequest(httpService, conn);
			}
		} catch (final IOException e) {
			System.err.println("Proxy encountered a problem. exiting. ");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

	}

	@SuppressWarnings({ "static-method", "javadoc" })
	private void handleSingleRequest(final HttpService httpService,
			final HttpServerConnection conn) {
		System.err.flush();
		log("@@@ Handling new request @@@");
		final HttpContext context = new BasicHttpContext(null);
		try {
			while (!Thread.interrupted() && conn.isOpen()) {
				System.err.flush();
				log("### handling same connection ###");
				httpService.handleRequest(conn, context);
			}
		} catch (final ConnectionClosedException ex) {
			System.err.println("Client closed connection...");
		} catch (final IOException ex) {
			System.err.println("I/O error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (final HttpException ex) {
			System.err.println("Unrecoverable HTTP protocol violation: "
					+ ex.getMessage());
		} finally {
			log("================================");
			try {
				conn.shutdown();
				// throw new IOException();
			} catch (final IOException ignore) {
				System.err.println("HttoServerConnection shutdown failed");
			}
		}
	}

	ICache cache;
}
