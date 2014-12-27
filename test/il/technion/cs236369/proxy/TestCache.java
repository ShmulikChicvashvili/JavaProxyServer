/**
 *
 */

package il.technion.cs236369.proxy;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.http.ParseException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;




/**
 * @author Shmulik
 *
 */
public class TestCache
{

	// @After
	// public void After() throws SQLException
	// {
	// cache.destroyTable();
	// }

	@After
	public void After() throws SQLException
	{
		cache.destroyTable();
	}


	@Before
	public void Before() throws SQLException
	{
		@SuppressWarnings("nls")
		final MySqlConnectionEstablisher connectionEstablisher =
			new MySqlConnectionEstablisher(
				"com.mysql.jdbc.Driver",
				"jdbc:mysql://127.0.0.1:3306/",
				"proxy",
				"test_cache",
				"root",
				"root");
		cache = new MySqlCache(connectionEstablisher);
		cache.buildTable();
	}
	
	
	//
	// @SuppressWarnings("nls")
	// @Test
	// public void GetTest() throws SQLException
	// {
	// final DBRecord dbTestInsertion =
	// new DBRecord(
	// "www.facebook.com",
	// "Welcome To Facebook",
	// "You have 10000 Likes",
	// "Yesterday");
	// cache.insert(dbTestInsertion);
	// dbTestInsertion.setUrl("www.Google.com");
	// dbTestInsertion.setHeader("Search for some shit");
	// dbTestInsertion.setBody("Here, take some pictures of cats");
	// dbTestInsertion.setLastModified("1 hour ago");
	// cache.insert(dbTestInsertion);
	//
	// assertEquals(null, cache.get(null));
	//
	// final DBRecord dbTest =
	// new DBRecord(
	// "www.facebook.com",
	// "Welcome To Facebook",
	// "You have 10000 Likes",
	// "Yesterday");
	// assertEquals(dbTest, cache.get("WwW.FacEbooK.com"));
	//
	// dbTest.setUrl("www.Google.com");
	// dbTest.setHeader("Search for some shit");
	// dbTest.setBody("Here, take some pictures of cats");
	// dbTest.setLastModified("1 hour ago");
	// assertEquals(dbTest, cache.get("www.google.com"));
	//
	// assertEquals(null, cache.get("www.linkedin.com"));
	// }
	//
	//
	// @SuppressWarnings("nls")
	// @Test
	// public void InsertTest() throws SQLException
	// {
	// assertEquals(SqlError.INVALID_PARAMS, cache.insert(null));
	//
	// final DBRecord dbTest =
	// new DBRecord(
	// "www.facebook.com",
	// "Welcome To Facebook",
	// "You have 10000 Likes",
	// "Yesterday");
	// assertEquals(SqlError.SUCCESS, cache.insert(dbTest));
	// assertEquals(SqlError.ALREADY_EXIST, cache.insert(dbTest));
	//
	// dbTest.setUrl("WwW.FaceBook.CoM");
	// assertEquals(SqlError.ALREADY_EXIST, cache.insert(dbTest));
	//
	// dbTest.setUrl("www.Google.com");
	// dbTest.setHeader("Search for some shit");
	// dbTest.setBody("Here, take some pictures of cats");
	// dbTest.setLastModified("1 hour ago");
	// assertEquals(SqlError.SUCCESS, cache.insert(dbTest));
	// assertEquals(SqlError.ALREADY_EXIST, cache.insert(dbTest));
	// }
	//
	//
	// @SuppressWarnings({ "nls", "boxing" })
	// @Test
	// public void IsExistTest() throws SQLException
	// {
	// assertEquals(false, cache.isExist(null));
	//
	// final DBRecord dbTestInsertion =
	// new DBRecord(
	// "www.facebook.com",
	// "Welcome To Facebook",
	// "You have 10000 Likes",
	// "Yesterday");
	// cache.insert(dbTestInsertion);
	// dbTestInsertion.setUrl("www.Google.com");
	// dbTestInsertion.setHeader("Search for some shit");
	// dbTestInsertion.setBody("Here, take some pictures of cats");
	// dbTestInsertion.setLastModified("1 hour ago");
	// cache.insert(dbTestInsertion);
	//
	// assertEquals(true, cache.isExist("WwW.google.com"));
	// assertEquals(true, cache.isExist("wWw.FacEbook.com"));
	// assertEquals(false, cache.isExist("www.notexist.com"));
	// }
	
	@SuppressWarnings("nls")
	@Test
	public void testGZipInsertGet()
		throws ParseException,
		IOException,
		SQLException
	{
		final String test =
			"I'm Sssssshmulik and im ggggggziping this stringgggggggg";
		final byte[] testZiped = GZipHandler.compress(test);
		final int length = testZiped.length;

		final ByteArrayEntity entity = new ByteArrayEntity(testZiped);

		assertEquals(length, EntityUtils.toString(entity).length());

		final Blob body = new SerialBlob(EntityUtils.toByteArray(entity));

		final DBRecord dbTest = new DBRecord("www.fuckme.com", "bla", body, "");
		cache.insert(dbTest);

		final DBRecord testGet = cache.get("www.fuckme.com");

		assertEquals(testGet.getBody().length(), length);
	}
	
	
	
	//
	// @SuppressWarnings("nls")
	// @Test
	// public void UpdateTest() throws SQLException
	// {
	// assertEquals(SqlError.INVALID_PARAMS, cache.update(null));
	//
	// final DBRecord dbTestInsertion =
	// new DBRecord(
	// "www.facebook.com",
	// "Welcome To Facebook",
	// "You have 10000 Likes",
	// "Yesterday");
	// cache.insert(dbTestInsertion);
	// dbTestInsertion.setUrl("www.Google.com");
	// dbTestInsertion.setHeader("Search for some shit");
	// dbTestInsertion.setBody("Here, take some pictures of cats");
	// dbTestInsertion.setLastModified("1 hour ago");
	// cache.insert(dbTestInsertion);
	//
	// final DBRecord dbTest =
	// new DBRecord(
	// "www.google.com",
	// "Welcome To Google",
	// dbTestInsertion.getBody(),
	// "1 minute ago");
	// assertEquals(SqlError.SUCCESS, cache.update(dbTest));
	// assertEquals(dbTest, cache.get("Www.GoogLe.com"));
	//
	// dbTest.setUrl("www.notexists.com");
	// assertEquals(SqlError.DOES_NOT_EXIST, cache.update(dbTest));
	// }
	
	/**
	 * Cache
	 */
	MySqlCache cache;
}
