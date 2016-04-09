<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
<title>Device Monitor Map</title>
<!-- <link rel="stylesheet" type="text/css" href="css/mapStyle.css"> -->
<style type="text/css">
.header1{
	background:-o-linear-gradient(bottom, #415866 5%, #415866 100%);	background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #ff0000), color-stop(1, #415866) );
	background:-moz-linear-gradient( center top, #415866 5%, #415866 100% );
	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr="#ff0000", endColorstr="#415866");	background: -o-linear-gradient(top,#415866,bf5f00);

	background-color:#ff0000;
	border:0px solid #000000;
	text-align:center;
	border-width:0px 0px 1px 1px;
	font-size:14px;
	font-family:Arial;
	font-weight:bold;
	color:#ffffff;
}

.header{
	padding-top: 10px;
    padding-bottom: 10px;
    vertical-align: bottom;
    position: relative;
    height: 70px;
    background-color: #333;
    border-bottom: 5px solid #3FAB2A;
}

#brandingLayout {
    position: relative;
    height: 70px;
    padding: 0px 48px 0px 10px;
}

.brandTitle {
    color: #ffffff;
    font-weight: bolder;
    font-size: 1.25em;
    vertical-align: bottom;
    margin-left: 2px;
    margin-right: 0px;
}

.body_container{
	margin-top: 20px;
}

.section_container {
	width: 100%;
	display: inline-block;
}

.top_section {
	width: 100%;
	height: 200px;
	float: left;
	overflow: hidden;
	
}

.profile_container {
	margin-right: 5px;
	margin-left: 5px;
	margin-bottom: 5px;
	width:90%;
	height: 100%;
	float: left;
	overflow: scroll;
}

div#customer_container{
	margin-right: 2px;
	margin-left: 2px;
	margin-top: 25px;
	margin-bottom: 2px;
	padding-top: 20px;
    padding-bottom: 20px;
	border-top: solid 1px #333;
	border-bottom: solid 1px #333;
	border-left: solid 1px #333;
	border-right: solid 1px #333;
	background-color: #EEE;
	box-shadow: 5px 5px 5px #888888;
}

div#account_container{
	margin-right: 2px;
	margin-left: 2px;
	margin-top: 2px;
	margin-bottom: 2px;
	border-top: solid 1px #333;
	border-bottom: solid 1px #333;
}

.map_container {
	margin-right: 5px;
	margin-left: 5px;
	margin-bottom: 5px;
	width:45%;
	float: left;
}

.charts_section {
	width: 100%;
	height: 210px;
	float: left;
	overflow: hidden;
}

.chart_container {
	margin-top: 5px;
	margin-right: 5px;
	margin-left: 5px;
	width: 45%;
	float: left;
	overflow: hidden;
	border-top: solid 1px #333;
	border-bottom: solid 1px #333;
	border-left: solid 1px #333;
	border-right: solid 1px #333;
}

.table_container{
	margin-top: 5px;
	margin-right: 5px;
	margin-left: 5px;
	width: 90%;
}

.summary_table{
	border-collapse: separate;
  	border-spacing: 50px 0;
	font-family: arial;
	font-size: 15px;
}

</style>
<script type="text/javascript" src="http://maps.googleapis.com/maps/api/js?sensor=false"></script>
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.7.8/dojo/dojo.js"></script>
<script type="text/javascript">
  dojo.require("dojo.io.script");
  dojo.require("dojox.cometd");
  dojo.require("dojox.cometd.longPollTransport");
  
  var pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
  var alertChannel = "/farudalert";
  
  //dojo.ready(connectMedicalDeviceTopic)
  
	function connectMedicalDeviceTopic(){
  		dojox.cometd.init(pubSubUrl);

  		dojox.cometd.subscribe("/*", function(message){
  			if(message.channel == alertChannel){
  				
  			}	
  		});
  	}
</script>
</head>
 
<body>
	<div class="header">
		<div id="brandingLayout">
                <a class="brandingContent">
                    <img src="images/hortonworks-logo-new.png" width="200px"/>
                    <span class="brandTitle" data-i18n="BRAND_TITLE"></span>
                </a>
		</div>
	</div>

	<div id="bodyContainer" class="body_container">
		<c:forEach items="${accountDetailsList}" var="accountDetails">
		<div id="top_div" class="section_container">
			<div class="top_section">
				<div id="details" class="profile_container">
					<!-- <div id="account_container">
						<table class="summary_table">
							
						</table> 
					</div>  -->
					<div id="customer_container">
						<table class="summary_table">
							<tr><td>Account Number: </td><td><a href="CustomerOverview?requestType=customerDetails&accountNumber=${accountDetails.accountNumber}">${accountDetails.accountNumber}</a></td><td>Address: </td><td style="vertical-align: top;" rowspan="3">${accountDetails.streetAddress}<br>${accountDetails.city} ${accountDetails.state} ${accountDetails.zipCode}</td></tr>
							<tr><td>Account Type: </td><td>${accountDetails.accountType}</td></tr>	
							<tr><td>Customer Name: </td><td>${accountDetails.firstName} ${accountDetails.lastName}</td></tr>
							<tr><td>Gender: </td><td>${accountDetails.gender}</td></tr>
							<tr><td>Age: </td><td>${accountDetails.age}</td></tr>
						
						</table>
					</div>
				</div>
			</div>
		</div>
		</c:forEach>
	</div>	 		
</body>
 
</html>