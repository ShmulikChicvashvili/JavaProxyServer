package il.technion.cs236369.proxy;

import java.io.IOException;

public interface IHttpProxy {
	void bind() throws IOException;

	void start();
}
