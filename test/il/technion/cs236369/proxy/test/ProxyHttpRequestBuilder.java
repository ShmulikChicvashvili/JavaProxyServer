package il.technion.cs236369.proxy.test;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;

	/**
	 *  ProxyHttpRequestBuilder 
	 *  Builds a http request from a given url string
	 */
public class ProxyHttpRequestBuilder {

	private final HttpRequest req;

	
	public ProxyHttpRequestBuilder(String url) throws URISyntaxException {
		req = new BasicHttpRequest("GET", url);
		URI uri = new URI(url);
		req.addHeader("Host", uri.getHost());
	}
	
	public ProxyHttpRequestBuilder addHeader(String name, String value) {
		req.addHeader(name, value);
		return this;
	}
	
	
	public HttpRequest build() {
		return req;
	}
}
