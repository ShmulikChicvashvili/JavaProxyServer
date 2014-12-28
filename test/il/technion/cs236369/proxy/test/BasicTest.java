package il.technion.cs236369.proxy.test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import il.technion.cs236369.proxy.HttpProxy;
import il.technion.cs236369.proxy.IHttpProxy;
import il.technion.cs236369.proxy.test.BlockingSocket;
import il.technion.cs236369.proxy.test.RequestExpectingSocket;
import il.technion.cs236369.proxy.test.ResponseExpectingSocket;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;
import org.mockito.exceptions.base.MockitoAssertionError;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Scenario: - client sends request to proxy - proxy should forward request to
 * response - proxy should send the response back to the client
 */
public class BasicTest {

	private final static String requestedURL = "http://jquery.com/index.html";
	private final static HttpHost target = new HttpHost("jquery.com",80,"http");

	private HttpProxyTestModule module;
	private ResponseExpectingSocket clientToProxySocket1;
	private RequestExpectingSocket proxyToServerSocket1;

	@Test
	public void sendOneRequestAndRecvACachableResponse() throws Exception {

		module = new HttpProxyTestModule();

		BasicHttpRequest httpRequest = new BasicHttpRequest("GET", requestedURL ,HttpVersion.HTTP_1_1);
		httpRequest.addHeader("host", target.getHostName());
		httpRequest.addHeader("Accept-Encoding", "gzip,deflate");
		
		clientToProxySocket1 = spy(new ResponseExpectingSocket(httpRequest));


		when(module.getMockedServerSocket().accept()).thenReturn(
				clientToProxySocket1).thenReturn(new BlockingSocket());

		BasicHttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK,
				null);
		httpResponse.addHeader("Content-Encoding", "deflate");
		
		proxyToServerSocket1 = spy(new RequestExpectingSocket(httpResponse));

		when(module.getMockedSockFactory().createSocket(anyString(), anyInt()))
				.thenReturn(proxyToServerSocket1);

		Injector injector = Guice.createInjector(module);

		final IHttpProxy proxy = injector.getInstance(HttpProxy.class);

		proxy.bind();
		new Thread(new Runnable() {
			@Override
			public void run() {
				proxy.start();
			}
		}).start();

		Thread.sleep(1000);
		try {
			// check the proxy listens on the correct port
			verify(module.getMockedServerSocketFactory(), times(1))
					.createServerSocket(8080);
			// check only a single connection was opened with the server
		} catch (MockitoAssertionError e) {
			System.err
					.println("Please call one time only to createServerSocket, by ServerSocketFactory.");
			throw e;
		}
		try {
			verify(module.getMockedSockFactory(), times(1)).createSocket(
					"jquery.com", 80);
		} catch (MockitoAssertionError e) {
			System.err
					.println("Please call one time only to createSocket, by SocketFactory.");
			throw e;
		}

		// check all sockets were closed
		try {
			verify(clientToProxySocket1, times(1)).close();
		} catch (MockitoAssertionError e) {
			System.err
					.println("Please close the socket to the client when done responding.");
			throw e;
		}
		try {
			verify(proxyToServerSocket1, times(1)).close();
		} catch (MockitoAssertionError e) {
			System.err
					.println("Please close the socket to the server when response recieved");
			throw e;
		}
	}
}
