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
package org.edu_sharing.repository.server;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DispatcherServlet
 */
public class DispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	
	String mapTo;
	
    /**
     * Default constructor. 
     */
    public DispatcherServlet() {
        
    	
    	
    }

    @Override
    public void init() throws ServletException {
    	mapTo = this.getInitParameter("mapTo");
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		try{
			RequestDispatcher requestDispatcher = request.getRequestDispatcher(mapTo);
			requestDispatcher.forward(request, response);
		}catch(Throwable e){
			e.printStackTrace();
		}
		
	}


}
