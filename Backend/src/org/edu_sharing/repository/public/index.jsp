
<%
// route WebDAV requests
if (request.getMethod().equalsIgnoreCase("PROPFIND") ||
	request.getMethod().equalsIgnoreCase("OPTIONS"))
{
	response.sendRedirect(request.getContextPath() + "/webdav/");
} 
%>

<%@ include file="index.html"%>