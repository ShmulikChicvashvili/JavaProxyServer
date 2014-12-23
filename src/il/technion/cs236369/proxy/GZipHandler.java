/**
 *
 */

package il.technion.cs236369.proxy;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;




/**
 * @author Shmulik
 *
 */
public class GZipHandler
{
	/**
	 * @param str
	 *            The string to compress
	 * @return The compressed byte array
	 * @throws Exception
	 *             e
	 */
	@SuppressWarnings("nls")
	public static byte[] compress(String str)
	{
		if (str == null || str.length() == 0) { return null; }
		final ByteArrayOutputStream obj = new ByteArrayOutputStream();
		try (final GZIPOutputStream gzip = new GZIPOutputStream(obj))
		{
			gzip.write(str.getBytes("UTF-8"));
		} catch (final IOException e)
		{
			System.err.println("Failed to create GZIPOutputSteam");
			e.printStackTrace();
		}
		return obj.toByteArray();
	}
	
	
	/**
	 * @param bytes
	 *            The compressed gzip
	 * @return The string decompressed
	 * @throws Exception
	 *             e
	 */
	@SuppressWarnings("nls")
	public static String decompress(byte[] bytes) throws Exception
	{
		if (bytes == null || bytes.length == 0) { return null; }
		final GZIPInputStream gis =
			new GZIPInputStream(new ByteArrayInputStream(bytes));
		final BufferedReader bf =
			new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		String outStr = "";
		String line;
		while ((line = bf.readLine()) != null)
		{
			outStr += line;
		}
		return outStr;
	}
}
