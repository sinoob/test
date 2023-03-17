<!DOCTYPE HTML>
<%@page import="webcrawler.*"%>
<%@page import="webcrawler.processor.*"%>
<%@page import="java.util.*"%>
<html>
<%
	String baseUrl = request.getParameter("baseUrl");
	String pageFrom = request.getParameter("pageFrom");
	String pageTo = request.getParameter("pageTo");
%>
<head>
<link rel="stylesheet" type="text/css" href="../css/jquery-ui-1.12.0.css">
<link rel="stylesheet" type="text/css" href="../css/jquery.growl.coffee.css">
<link rel="stylesheet" type="text/css" href="../css/common.css">
<link rel="stylesheet" type="text/css" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css">
</head>
<body>
	<form method="POST" action="crawler.jsp">
		<div class="form-group row">
			<div class="col-4">
				<div class="cell">Base URL</div>
				<input type="text" name="baseUrl" class="form-control form-control-sm" />
			</div>
			<div class="col-1">
				Page From :
				<input type="text" name="pageFrom" class="form-control form-control-sm" />
			</div>
			<div class="col-1">
				Page To :
				<input type="text" name="pageTo" class="form-control form-control-sm" />
			</div>
			<div class="col-1">
				<div class="col-1">&nbsp;</div>
				<input type="submit" value="View" class="btn btn-primary btn-block" />
			</div>
		</div>
	</form>

	<%
		if (baseUrl != null ) {
			int i = 0;
			int j = 0;
			if(pageFrom != null && pageFrom != "" && pageTo != null && pageTo != ""){
				i = Integer.parseInt(pageFrom);
				j = Integer.parseInt(pageTo);
			}
			 
			for (; i <= j; i++) {
				out.println("<br>");
				String url = null;
				if(i == 0){
					url = new StringBuilder().append(baseUrl).toString();
				}else{
					url = new StringBuilder().append(baseUrl).append("&page=").append(i).toString();
				}
				ImgCrawler crawler = new ImgCrawler();
				crawler.addProcessor(new PzyProcessor());
				crawler.addProcessor(new ICGProcessor());
				List<String> images = crawler.getImageLinks(url);
				out.print("<div class=\"form-group row\">");
				for(String s : images){
					out.print("<div class=col-10 >");
					//out.print(s);
					out.print("<img class='img-auto' src=");
					out.print(s);
					out.print(" />");
					out.print("</div>");
					//out.print("<br />");
				}
				out.print("</div>");
			}
		}
	%>
</body>
</html>