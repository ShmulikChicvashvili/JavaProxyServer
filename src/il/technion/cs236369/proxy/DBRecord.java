/**
 *
 */

package il.technion.cs236369.proxy;


import java.sql.Blob;




/**
 * @author Shmulik
 *
 */
public class DBRecord
{
	/**
	 * @param url
	 *            URL
	 * @param header
	 *            Header
	 * @param body
	 *            Body
	 * @param lastModified
	 *            Last modified
	 */
	public DBRecord(String url, String header, Blob body, String lastModified)
	{
		this.url = url;
		this.header = header;
		this.body = body;
		LastModified = lastModified;
	}
	
	
	/* (non-Javadoc) @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj)
	{
		final DBRecord other = (DBRecord) obj;
		return other.url.toLowerCase().equals(url.toLowerCase())
			&& other.header.toLowerCase().equals(header.toLowerCase())
			&& other.body
				.toString()
				.toLowerCase()
				.equals(body.toString().toLowerCase())
			&& other.LastModified.toLowerCase().equals(
				LastModified.toLowerCase());
	}
	
	
	/**
	 * @return the body
	 */
	public Blob getBody()
	{
		return body;
	}
	
	
	/**
	 * @return the header
	 */
	public String getHeader()
	{
		return header;
	}
	
	
	/**
	 * @return the lastModified
	 */
	public String getLastModified()
	{
		return LastModified;
	}
	
	
	/**
	 * @return the url
	 */
	public String getUrl()
	{
		return url;
	}
	
	
	/**
	 * @param body
	 *            the body to set
	 */
	public void setBody(Blob body)
	{
		this.body = body;
	}
	
	
	/**
	 * @param header
	 *            the header to set
	 */
	public void setHeader(String header)
	{
		this.header = header;
	}
	
	
	/**
	 * @param lastModified
	 *            the lastModified to set
	 */
	public void setLastModified(String lastModified)
	{
		LastModified = lastModified;
	}
	
	
	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}


	/* (non-Javadoc) @see java.lang.Object#toString() */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "URL: "
			+ url
			+ " Header: "
			+ header
			+ " Body: "
			+ body
			+ " Last Modified: "
			+ LastModified;
	}



	/**
	 * URL
	 */
	String url;
	
	/**
	 * Header
	 */
	String header;
	
	/**
	 * Body
	 */
	Blob body;

	/**
	 * Last modified
	 */
	String LastModified;
	
}
