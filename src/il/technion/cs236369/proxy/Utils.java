/**
 *
 */

package il.technion.cs236369.proxy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * @author Shmulik
 *
 */
public class Utils {
	/**
	 * @param request
	 */
	public static boolean isAcceptingGzip(HttpRequest request) {
		if (request.containsHeader(HttpHeaders.ACCEPT_ENCODING)
				&& request.getHeaders(HttpHeaders.ACCEPT_ENCODING)[0]
						.getValue().contains("gzip")) {
			return true;
		}
		return false;
	}

	public static String join(Collection<?> s, String delimiter) {
		final StringBuilder builder = new StringBuilder();
		final Iterator<?> iter = s.iterator();
		while (iter.hasNext()) {
			builder.append(iter.next());
			if (!iter.hasNext()) {
				break;
			}
			builder.append(delimiter);
		}
		return builder.toString();
	}

	public static String responseToString(HttpResponse response) {
		String $ = "";
		for (final Header h : response.getAllHeaders()) {
			$ += h.toString() + "\r\n";
		}
		return $;
	}

	public static void stringToResponse(String headersString,
			HttpResponse response) {
		try (final Scanner scanner = new Scanner(headersString)) {
			response.setHeaders(null);
			while (scanner.hasNextLine()) {
				final String line = scanner.nextLine();
				if (line.contains(":")) {
					final String[] values = line.split(":");
					for (final String value : values) {
						value.replace("\r", "").replace("\n", "");
					}
					if (values.length == 2) {
						response.addHeader(values[0], values[1]);
					} else {
						final String[] headerValues = Arrays.copyOfRange(
								values, 1, values.length);
						response.addHeader(values[0],
								join(Arrays.asList(headerValues), ":"));
					}
				}
			}
		}
	}

}
