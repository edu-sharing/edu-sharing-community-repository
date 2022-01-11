/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.alfrescocontext.loader;


public class AlfrescoContextClassLoaderUtil {
	
	private static ClassLoader classLoader = null;

	/**
	 * @return the classLoader
	 */
	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * @param classLoader the classLoader to set
	 */
	public static void setClassLoader(ClassLoader classLoader) {
		System.out.println("AlfrescoContextClassLoaderUtil.setClassLoader:"+classLoader);
		AlfrescoContextClassLoaderUtil.classLoader = classLoader;
	}
	
	
}
