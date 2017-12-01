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
package org.edu_sharing.repository.screenreader;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

/**
 * A tool class with static methods for more comfort within JSP pages.
 * Method are not for reuse - its special for screenreader search
 * @author Christian
 *
 */
public class JspTools {
	
	/**
	 * Used to set the state and data of an input field according to request parameter
	 * @param req
	 * @param id
	 * @param defaultValue
	 * @return
	 */
	public static String text_nameValue(javax.servlet.ServletRequest req, String id, String defaultValue) {
		String ret = "name=\""+id+"\" id=\""+id+"\" value=\"";
		if (req.getParameter(id)!=null) {
			ret += req.getParameter(id);		
		} else {
			ret +=defaultValue;		
		}
		return ret+"\"";
	}
	
	/**
	 * Used to set the state and data of an checkbox according to request parameter
	 * @param req
	 * @param id
	 * @param defaultValue
	 * @return
	 */
	public static String checkbox_nameValue(javax.servlet.ServletRequest req, String id, String defaultValue) {
		String ret = "name=\""+id+"\" id=\""+id+"\" value=\""+defaultValue+"\"";
		
		// set checked if parameter tells so
		if ((req.getParameter(id)!=null) && (req.getParameter(id).equals(defaultValue))) {
			ret += " checked";		
			//System.out.println("Set '"+id+"' checked because of parameter");
		}
		
		// or set checked if default tells so 
		if ((req.getParameter(id)==null) && (defaultValue.equals(Const.VAL_TRUE))) {
			ret += " checked";			
			//System.out.println("Set '"+id+"' checked because of default");			
		}
		
		return ret;
	}	
	
	/**
	 * Rebuilds a URL encoded parameter string from all parameters on request 
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String reCreateParameterString(javax.servlet.ServletRequest request) {
		
		String paraString = "";
		Iterator i = request.getParameterMap().keySet().iterator();
		
		try {
			while (i.hasNext()) {
				
				// org parameter value
				String paraName = (String) i.next();
				String[] paraVal 	= request.getParameterValues(paraName);
				
				for (int j=0; j<paraVal.length; j++) {
					// check if maybe to ignore
					boolean ignore = false;
					if (paraName.equals("msg")) ignore = true; 
					if (paraName.equals(Const.PARA_STARTITEM)) ignore = true; 	
				
					// add to parameter string
					if (!ignore) paraString += "&" + paraName + "=" + URLEncoder.encode(paraVal[j],"UTF-8");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		// Add unchecked checkbox values 
		
		// - the reposity selection
		Vector<String> repoIds = getAllReposFromParameters(request);
		Iterator<String> j = repoIds.iterator();
		while (j.hasNext()) {
			String id = j.next();
			if (request.getParameter(Const.PARA_REPO_PREFIX+id)==null) paraString += "&"+ Const.PARA_REPO_PREFIX+id +"="+Const.VAL_FALSE;
		}
		
		if (paraString.length()>1) return paraString.substring(1);
		return "";
		
	}
	
	/**
	 * Returns all actived/checked reposities IDs from request
	 * @param request
	 * @return
	 */
	public static Vector<String> getActiveReposFromParameters(javax.servlet.ServletRequest request) {	
		Vector<String> results = new Vector<String>();
		
		Vector<String> repoIDs = getAllReposFromParameters(request);
		
		Iterator<String> i = repoIDs.iterator();
		while (i.hasNext()) {
			String repoID = i.next();
			String val = request.getParameter(Const.PARA_REPO_PREFIX+repoID);
			if ((val!=null) && (val.equals(Const.VAL_TRUE))) {
				results.add(repoID);
			}
		}
		
		return results;
	}
	
	/**
	 * Returns all reposities IDs that are listed in request parameter
	 * @param request
	 * @return
	 */
	public static Vector<String> getAllReposFromParameters(javax.servlet.ServletRequest request) {	
		Vector<String> results = new Vector<String>();
		String repList = request.getParameter(Const.PARA_REPO_LIST);
		if (repList!=null) {
			String[] parts = repList.split(",");
			for (int i=0; i<parts.length; i++) {
				results.add(parts[i]);
			}
		}
		return results;
	}	
	
	/**
	 * Converts special chars into HTML entities
	 * @param string
	 * @return
	 */
	public static String toHTML(String string) {
	    StringBuffer sb = new StringBuffer(string.length());
	    // true if last char was blank
	    boolean lastWasBlankChar = false;
	    int len = string.length();
	    char c;

	    for (int i = 0; i < len; i++)
	        {
	        c = string.charAt(i);
	        if (c == ' ') {
	            // blank gets extra work,
	            // this solves the problem you get if you replace all
	            // blanks with &nbsp;, if you do that you loss 
	            // word breaking
	            if (lastWasBlankChar) {
	                lastWasBlankChar = false;
	                sb.append("&nbsp;");
	                }
	            else {
	                lastWasBlankChar = true;
	                sb.append(' ');
	                }
	            }
	        else {
	            lastWasBlankChar = false;
	            //
	            // HTML Special Chars
	            if (c == '"')
	                sb.append("&quot;");
	            else if (c == '&')
	                sb.append("&amp;");
	            else if (c == '<')
	                sb.append("&lt;");
	            else if (c == '>')
	                sb.append("&gt;");
	            else if (c == '\n')
	                // Handle Newline
	                sb.append("&lt;br/&gt;");
	            else {
	                int ci = 0xffff & c;
	                if (ci < 160 )
	                    // nothing special only 7 Bit
	                    sb.append(c);
	                else {
	                    // Not 7 Bit use the unicode system
	                    sb.append("&#");
	                    sb.append(new Integer(ci).toString());
	                    sb.append(';');
	                    }
	                }
	            }
	        }
	    return sb.toString();
	}	
	
	/*
	 * get just the char values from a string
	 */
	public static String justChars(String src) {
		String result = "";
		if (src!=null) {
			for (int i=0; i<src.length(); i++) {
				if ((src.charAt(i)>=65) && (src.charAt(i)<=90)) result += src.charAt(i);
				if ((src.charAt(i)>=97) && (src.charAt(i)<=122)) result += src.charAt(i);				
			}
		}
		return result;		
	}
	
	/*
	 * returns a value from http request or given default value if not available
	 */
	public static String getParameterOrDefault(javax.servlet.ServletRequest req, String id, String defaultValue) {
		if (req.getParameter(id)!=null) return req.getParameter(id);
		return defaultValue;
	}
	
	/*
	 * Gets the locale (language) settig from request parameter or sets default if not available
	 */
	public static Locale getLocaleFromRequest(javax.servlet.ServletRequest req) {
		Locale locale = null;
		if (req.getParameter("lang")!=null) {
			String langStr = req.getParameter("lang");
			if (langStr.indexOf("_")>1){
				String country = langStr.substring((langStr.indexOf("_")+1));
				langStr = langStr.substring(0,langStr.indexOf("_"));
				locale = new Locale(langStr, country);
			} else {
				locale = new Locale(req.getParameter("lang"));	
			}
		}
		if (locale==null) locale = Locale.getDefault();
		return locale;
	}
	
	
}
