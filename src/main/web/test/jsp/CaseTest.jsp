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
Map info = (Map) request.getAttribute("info");
if(info == null){
	info = (Map) session.getAttribute("info");
}
// if null forward request
if(info == null){
%>
	<meta http-equiv="refresh" content="0;url=CaseTestLogin"> </head> 
<% 
} else {
	session.setAttribute("info",info);
	final String [] status = new String [] {"","TEST","BUG","OK","FIXED"};
	
	final String shadeColor = "style=\"background-color: rgb(204, 204, 204);\"";
	URL servlet = null;
	try{
		String url = "http://"+request.getServerName()+":"+request.getServerPort();
		servlet = new URL(url+request.getContextPath()+"/servlet/CaseTestServlet" );
	}catch(MalformedURLException ex){
		ex.printStackTrace();
	}
	// get list of cases
	String user = CaseTestHelper.filter((String) info.get("username"));
	String pass = CaseTestHelper.encodePassword(CaseTestHelper.filter((String) info.get("password")));
	String domain =  CaseTestHelper.filter((String) info.get("domain"));
	String condition =  CaseTestHelper.filter((String) info.get("condition"));
	String sort =   CaseTestHelper.filter((String) info.get("sort"));
	String offset =  CaseTestHelper.filter((String) info.get("page"));
	Map caseMap =  (Map) info.get("cases");
	if(caseMap == null){
		caseMap = (Map) CaseTestHelper.queryServlet(servlet,"get_case_list&domain="+domain+"&sort="+sort+"&page="+offset);
	}
	List pages = new ArrayList(caseMap.keySet());
	Collections.sort(pages);
	if(offset == null && pages.size() > 0)
		offset = ""+pages.get(0);
		
	int testcount = 0, okcount = 0;
	
%>
	<title>CaseTest [page <%=offset%>]</title>
</head>
<body>
<% if(caseMap != null){ 
	// count all test cases
	for(Iterator i=caseMap.values().iterator();i.hasNext();){
		for(Iterator j=((List)i.next()).iterator();j.hasNext();){
			CaseBean bean = (CaseBean) j.next();
						
			// count number of cases to test
			if(!"AUTH".equals(bean.getStatus()))
				testcount++;
			
			// count number of tested cases
			if("OK".equals(bean.getStatus()))
				okcount++;	
		}
	}
 %>
<hr>
<!-- <center><h3>Tutor Case Testing Plan</h3></center><hr> -->
<table style="text-align: left; width: 100%;" border="0" cellpadding="2" cellspacing="0">
    <tbody>
      <tr style="background-color: rgb(200, 200, 255);">
          <td></td>
	      <td  align="left" valign="top"><b>
	      <a href="<%=servlet+"?user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&page="+offset %>&action=sort&sort=name">Name</a></b></td>
	      <td  align="left" valign="top"><b>
	      <a href="<%=servlet+"?user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&page="+offset %>&action=sort&sort=status">Status</a></b></td>
	      <td  align="center" valign="top"><b>
	      <a href="<%=servlet+"?user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&page="+offset %>&action=sort&sort=diagnosis">Diagnosis</a></b></td>
	      <td  align="center" valign="top"><b>
	      <a href="<%=servlet+"?user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&page="+offset %>&action=sort&sort=difficulty">Difficulty</b></td>
	

	      <td></td>
	      <td  align="center" valign="top"><b>Tested Status</b></td>
	      <td></td>
	      <td  align="center" valign="top"><b>Users that already tested this case</b></td>   
 		   <td></td>
      </tr>	
    	<% 
		boolean shade = false;
		List cases = (List) caseMap.get(offset);
		for(int i=0;i<cases.size();i++,shade=shade^true){
			CaseBean bean = (CaseBean) cases.get(i);
						
			StringBuffer tested = new StringBuffer();
			Map map = bean.getTestUserMap();
						
			// create user map
			if(map != null){
				String br="";
				for(Iterator k=map.keySet().iterator();k.hasNext();){
					String key = k.next().toString();
					List value = (List) map.get(key);
					CaseTestHelper.sortComponentsByTime(value);
					tested.append(br+"<b>"+key+"</b> <select>");
					for(Iterator l=value.iterator();l.hasNext();){
						Object val = l.next();
						List lst = new ArrayList((List)val);
						if(lst.size() > 2)
							lst.remove(2);
						tested.append("<option>"+lst+"</option>");
					}
					tested.append("</select>");
					br = "<br>";
				}
			}		
		%>	
	  <form method="post" action="<%=servlet%>">		
	  <tr <%= (shade)?shadeColor:"" %>>
	    <td style="background-color: rgb(200, 200, 255);">
      	</td>
        <td align="left" valign="top" >
        	<!-- 
        	<a href="<%= ""+servlet+"?action=launch_jnlp&jnlp=SimTutor&problem="+bean.getName()+
					"&domain="+domain+"&user="+user+"&pass="+pass+"&condition="+condition %>">
					 -->
			<b><%= bean.getName() %></b>
        </td>
		<td align="left" valign="top" >
			<%= bean.getStatus() %>
		</td>
		
		<td align="left" valign="top" >
			<%= CaseTestHelper.chopString(""+bean.getDiagnoses(),100) %> 
		</td>
		
		<td align="left" valign="top" >
			<%= CaseTestHelper.chopString(bean.getDifficulty(),18) %> 
		</td>
      	
      	<td style="background-color: rgb(200, 200, 255);">
      	</td>
      	<td align="center"  valign="top" style="background-color: rgb(255, 255, 255);">
      		<input type="hidden" name="action" value="tested">
      		<input type="hidden" name="user" value="<%=user%>">
      		<input type="hidden" name="pass" value="<%=pass%>">
      		<input type="hidden" name="domain" value="<%=domain%>">
      		<input type="hidden" name="condition" value="<%=condition%>">
      		<input type="hidden" name="case" value="<%=bean.getName()%>">
      		<input type="hidden" name="page" value="<%=offset%>">
      		
      		<select name="status" id="status<%=i%>">
      		<% for(int j=0;j<status.length;j++){ %>	
      			<option><%=status[j]%></option>
      		<% } %>
      		</select>
      		<input value="OK" type="submit" onclick="return (document.getElementById('status<%=i%>').selectedIndex > 0)">
      	</td>
      	<td style="background-color: rgb(200, 200, 255);">
      	</td>
      	<td align="left"  valign="top">
			<%= ""+tested %>
		</td>
		<td style="background-color: rgb(200, 200, 255);">
      	</td>  
		</form>	
      </tr>
	   <%
	      }
	    %>
	    <tr style="background-color: rgb(200, 200, 255);">
          <td colspan="11"></td>
      </tr>	 
    </tbody>
</table>
<hr>
Total of <b><%= testcount %></b> completly authored cases in <b><%= domain %></b> domain.
<b><%= okcount %></b> of those are well tested cases.
[<a href="<%= servlet+"?action=refresh&user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&page="+offset%>">refresh list</a>]
[<a href="<%=servlet+"?domain="+domain+"&action=summary"%>">summary</a>]
pages [<% 
	for(int i=0;i<pages.size();i++){
		String p = ""+pages.get(i);
		if(offset.equals(p)){
			%>
			<b><%=p%></b>
			<%
		}else {
			%>
			<b><a href="<%=servlet+"?user="+user+"&pass="+pass+"&domain="+domain+"&condition="+condition+"&action=sort&sort="+sort+"&page="+p%>">
			<%=p%></a></b>
			<%
		}
	}	
%>]
	<% } %>
	</body>
<% } %>
</html>

