<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Case Test Login</title>
</head>

<%@ page language="java" contentType="text/html" %>
<%@ page import="java.util.*,java.net.*,edu.pitt.dbmi.casetest.*" %>

<body>

<%! String [] tests; %>

<%
		// init communicator
		URL servlet = null;
		try{
			String url = "http://"+request.getServerName()+":"+request.getServerPort();
			servlet = new URL(url+"/"+request.getContextPath()+"/servlet/CaseTestServlet" );
		}catch(MalformedURLException ex){
			ex.printStackTrace();
		}
		// query servlet for available tests
		List domains = (List) CaseTestHelper.queryServlet(servlet,"get_domain_list");
		if( domains == null)
			domains = new ArrayList();
		//String [] conditions = new String [] {"expert","novice"};
		
		//{"Immediate Feedback","Fading Feedback","Delayed Feedback",
		//"Control Fading", "Control Delayed", "No Feedback"};
		
%>
<form action="<%= servlet %>" method="POST">
<input type=hidden name="session" value="<%=session.getId()%>">
<br><br>
<center>
<!--#C0C0C0 -->
<table width="50%" bgcolor="#C8C8FF">
<tr>
	<td colspan="2"><center><b>Please login</b><hr></ceter></td>
</tr>
<tr>
	<td>Username</td>
	<td><input type="text" name="username"></td>
</tr>
<tr>
	<td>Password</td>	
	<td><input type="password" name="password"></td>
</tr>
<tr>
	<td>Domain</td>
	<td><select name="domain" style="width: 160;">
	  <% for(int i=0;i<domains.size();i++){ %>
			<option><%=domains.get(i)%></option>
	  <% } %>	
	</select></td>	
</tr>

<tr>
	<td colspan="2">
		<center><input type="submit" name="action" value="login"></center>
	</td>
</tr>	
</center>
</table>
</form>
</body>
</html>
