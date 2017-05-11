<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<style type="text/css">
	body {font-size: 90%; }
	table {font-size: 90%; }
	select {font-size: 80%; }
</style>

<%@ page language="java" contentType="text/html"%>
<%@ page import="java.util.*,java.net.*,edu.pitt.dbmi.casetest.*" %>


<%
/// retrieve attributes
Map summary = (Map) request.getAttribute("summary");
if(summary == null){
	summary = (Map) session.getAttribute("summary");
}
// if null forward request
if(summary == null){
%>
	<meta http-equiv="refresh" content="0;url=CaseTestLogin"> </head> 
<% 
} else {
	final String shadeColor = "style=\"background-color: rgb(204, 204, 204);\"";
%>
</head>
<body>
<hr>
<!-- <center><h3>Tutor Case Testing Plan</h3></center><hr> -->
<table style="text-align: left;" border="0" cellpadding="2" cellspacing="0">
    <tbody>
    
      <tr style="background-color: rgb(200, 200, 255);">
      	<td colspan="5"></td>
      </tr>	
    	<% 
		boolean shade = false;
		for(Iterator i=summary.keySet().iterator();i.hasNext();shade=shade^true){
			String key = ""+i.next();
			String val = ""+summary.get(key);
		%>	
			<tr <%= (shade)?shadeColor:"" %>>
	    	<td style="background-color: rgb(200, 200, 255);"></td>
	        <td align="left" valign="top" ><%= key %></td>
	        <td style="background-color: rgb(200, 200, 255);"></td>
			<td align="left" valign="top" ><%= val %></td>
		  	<td style="background-color: rgb(200, 200, 255);"></td>  
			</tr>
	   <%
	      }
	    %>
	    <tr style="background-color: rgb(200, 200, 255);">
          <td colspan="5"></td>
      </tr>	 
    </tbody>
</table>
<hr>
</body>
<% } %>
</html>

