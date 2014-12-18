/**
 *
 */

package il.technion.cs236369.proxy;


/**
 * @author Shmulik
 *
 */
public enum SqlError
{
	/**
	 * Function succeed
	 */
	SUCCESS,
	
	/**
	 * Function has wrong parameters
	 */
	INVALID_PARAMS,
	
	/**
	 * Function insert threw exception for user already exist
	 */
	ALREADY_EXIST,

	/**
	 * Record already exist in database
	 */
	DOES_NOT_EXIST
}
