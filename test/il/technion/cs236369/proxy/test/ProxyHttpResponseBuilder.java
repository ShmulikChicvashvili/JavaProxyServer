package il.technion.cs236369.proxy.test;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

public class ProxyHttpResponseBuilder {

	private final HttpResponse res;
	
	
	public ProxyHttpResponseBuilder(int status) {
		res = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, null);
	}
	
	public ProxyHttpResponseBuilder addHeader(String name, String value) {
		res.addHeader(name, value);
		return this;
	}
	
	public ProxyHttpResponseBuilder setBody(String body) throws UnsupportedEncodingException {
		res.setEntity(new StringEntity(body));
		return this;
	}
	
	public HttpResponse build() {
		return res;
	}
}
