/**
 *
 */

package il.technion.cs236369.proxy;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;




/**
 * @author Shmulik
 *
 */
public class TestGZip
{

	@SuppressWarnings("nls")
	@Test
	public void GZipTest()
	{
		String test = "";

		try
		{
			assertEquals(null, GZipHandler.compress(test));
			assertEquals(null, GZipHandler.decompress(test.getBytes()));
			
			test += "aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbcccccccccccccccccc";
			assertEquals(
				test,
				GZipHandler.decompress(GZipHandler.compress(test)));
		} catch (final Exception e)
		{
			e.printStackTrace();
			fail();
		}

	}

}
