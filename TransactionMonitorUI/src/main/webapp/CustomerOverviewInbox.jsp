<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
<title>Analyst Inbox</title>
<!-- <link rel="stylesheet" type="text/css" href="css/mapStyle.css"> -->
<style type="text/css">
body {
 height: 85%;
}

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

.envolope {
    width: 90%;
    margin-top: 20px;
    margin-left: 10px;
    border-top: solid 1px #B9B8B8;
    border-bottom: solid 1px #B9B8B8;
    border-left: solid 1px #B9B8B8;
    border-right: solid 1px #B9B8B8;
}

.inbox_header {
    width: 100%;
    height: 7%;
    position: relative;
    float: left;
    border-bottom: solid 1px #B9B8B8;
    /* border-left: solid 1px #B9B8B8; */
    /* border-right: solid 1px #B9B8B8; */
    background-color: #F5F5F5;
}

.body_container {
    /* margin-top: 10px; */
    height: 80%;
    width: 100%;
    position: relative;
    /* float: left; */
    overflow: hidden;
}

.pane_container {
    height: 100%;
    width: 49.88%;
    position: relative;
    float: left;
    border-left: solid 1px #B9B8B8;
    overflow: hidden;
}

.menu_pane_container {
	height: 100%;
	width: 1%;
	position: relative;
	float: left;	
}

.menu_pane {
	height: 100%;
	width: 100%;
	border-top: solid 1px #B9B8B8;
	border-bottom: solid 1px #B9B8B8;
	border-left: solid 1px #B9B8B8;
	border-right: solid 1px #B9B8B8;
	background-color: #F5F5F5;
}

.menu_item {
	height: 10%;
	width: 100%;
}

.message_pane {
    height: 95%;
    width: 100%;
    /* border-bottom: solid 1px #B9B8B8; */
    overflow: scroll;
    /* position: relative; */
    /* float: left; */
}

.message_pane_heading {
    height: 5%;
    width: 100%;
    /* border-top: solid 1px #B9B8B8; */
    border-bottom: solid 1px #B9B8B8;
    background: whitesmoke;
    position: relative;
    float: left;
}

.message {
	height: 70px;
    width: 100%;
    padding-left: 5px;
	font-family: arial;
	font-size: 15px;
	border-bottom: solid 1px #B9B8B8;
}

.preview_pane {
    height: 100%;
    width: 100%;
    /* border-top: solid 1px #B9B8B8; */
    /* border-bottom: solid 1px #B9B8B8; */
    /* border-right: solid 1px #B9B8B8; */
    /* margin-right: 2px; */
    /* margin-left: 2px; */
    /* margin-top: 5px; */
    /* margin-bottom: 2px; */
    /* padding-top: 10px; */
    /* padding-bottom: 10px; */
    font-family: arial;
    font-size: 15px;
    overflow: hidden;
    float: left;
    position: relative;
}

.chart {
	height: 50%;
	width: 100%;
	padding-left: 20px;
    padding-top: 10px;
	border-top: solid 1px #B9B8B8;
	overflow: hidden;
	float: left;
    position: relative;
    display: none;
}

.footer_container {
    height: 5%;
    border-top: solid 1px #B9B8B8;
    /* border-bottom: solid 1px #B9B8B8; */
    /* border-left: solid 1px #B9B8B8; */
    /* border-right: solid 1px #B9B8B8; */
    background-color: #F5F5F5;
    /* margin-top: 3px; */
    width: 100%;
}
					
</style>
<script src="http://code.jquery.com/jquery-1.9.1.js"></script>
<script src="https://code.highcharts.com/highcharts.js"></script>
<script src="https://code.highcharts.com/highcharts-more.js"></script>
<script src="https://code.highcharts.com/modules/exporting.js"></script>
<script type="text/javascript" src="http://maps.googleapis.com/maps/api/js?key=${mapAPIKey}"></script>
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.7.8/dojo/dojo.js"></script>
<script type="text/javascript">
  dojo.require("dojo.io.script");
  dojo.require("dojox.cometd");
  dojo.require("dojox.cometd.longPollTransport");
  
  var cometdHost = "${cometdHost}";
  var cometdPort = "${cometdPort}";
  var pubSubUrl = "http://" + cometdHost + ":" + cometdPort + "/cometd";
  var alertChannel = "/fraudAlert";
  var incomingTransactionsChannel = "/incomingTransactions";
  var preview = {};
  var prevColor;
  var selected = false;
  var reasonFlagged = 'Amount for vendor type, distance from previous, and/or time between transactions is outside expected range.';
  
  dojo.ready(connectFraudAlertTopic)
  
	function connectFraudAlertTopic(){
  		dojox.cometd.init(pubSubUrl);
		
  		dojox.cometd.subscribe("/*", function(message){
  			var accountNumber = message.data.accountnumber
  			var score = message.data.score
  			var transactionId = message.data.transactionid
  			var transactionTimeStamp = message.data.transactiontimestamp
  			var accountType = message.data.accounttype
  			var amount =  message.data.amount
  			var merchantId = message.data.merchantid
  			var merchantType = message.data.merchanttype
  			
  			
  			if(message.channel == alertChannel || message.channel == incomingTransactionsChannel){
  				console.log(message)
  				var iDiv = document.getElementById("inbox");
  				var pDiv = document.getElementById("previewPane");
  				var messageDiv = document.createElement("div");
  				var previewDiv = document.createElement("div");
  				var chartDiv = document.createElement("div");
  				
  				messageDiv.className = "message";
  				messageDiv.id = transactionTimeStamp;
  				messageDiv.onclick = function(){showPreview(this.id)};
  				messageDiv.ondblclick = function(){location.href='CustomerOverview?requestType=customerDetails&accountNumber=' + accountNumber};
  				messageDiv.onmouseout = function(){closePreview(this.id)};
  				if(message.channel == alertChannel){
  					if(score == 50){
  						messageDiv.style.backgroundColor = "rgba(216, 216, 20, 0.25)";	
  					}else if(score == 75){
  						messageDiv.style.backgroundColor = "rgba(216,138,20,0.25)";
  					}else if(score == 100){	
  						messageDiv.style.backgroundColor = "rgba(216,20,20,0.25)";
  					}else{
  						messageDiv.style.backgroundColor = "rgba(216,20,20,0.25)";
  					}
  					messageDiv.innerHTML = 'TransactionId: ' + transactionId + '<br>' +
  					<%--'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=' + message.data.accountNumber +'">' + message.data.accountNumber + '</a><br>' + --%>
					'Account Number: ' + accountNumber; 	
  				}else{
  					messageDiv.style.backgroundColor = "rgba(63,171,42,.25)";
  					messageDiv.innerHTML = 'TransactionId: ' + transactionId + '<br>' +
  					<%--'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=' + message.data.accountNumber +'">' + message.data.accountNumber + '</a><br>' + --%>
					'Account Number: ' + accountNumber;
  				}	
  				previewDiv.className = "chart";
  				previewDiv.id = "preview" + transactionTimeStamp;
  				previewDiv.innerHTML = 'TransactionId: ' + transactionId + '<br>' +
					'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=' + accountNumber +'">' + accountNumber + '</a><br>' +
					'Account Type: ' + accountType + '<br>' +
					'Amount: ' + amount + '<br>' +
					'Merchant Id: ' + merchantId + '<br>' +
					'Merchant Type: ' + merchantType + '<br>' +
					'Time of Transaction: ' + transactionTimeStamp + '<br><br>';
				if(message.channel == alertChannel){	
					previewDiv.innerHTML += 'Reason Flagged: ' + reasonFlagged;
  				}	
				chartDiv.className = "chart"
		  		chartDiv.id = "chart" + transactionTimeStamp;
		  		//chartDiv.onclick = function(){showPreview(this.id)};
		  		//chartDiv.onmouseout = function(){closePreview(this.id)};
										
  				iDiv.appendChild(messageDiv);
  				pDiv.appendChild(previewDiv);
  				pDiv.appendChild(chartDiv);
  				drawSDChart(message);
  				//console(message.data.transactionTimeStamp);
  				//document.getElementById(message.data.transactionTimeStamp).onclick = function(){showPreview($(this).attr('id'))};
  				//console.log(document.getElementById(message.data.transactionTimeStamp).id);
  			}	
  		});
  		
  }
  
  function renderInitialInboxItems(){
	<c:forEach items="${transactionHistory}" var="transaction">
		var iDiv = document.getElementById("inbox");
		var pDiv = document.getElementById("previewPane");
		var messageDiv = document.createElement("div");
		var previewDiv = document.createElement("div");
		var chartDiv = document.createElement("div");
		
		messageDiv.className = "message";
		messageDiv.id = ${transaction.transactionTimeStamp};
		messageDiv.onclick = function(){showPreview(this.id)};
		messageDiv.ondblclick = function(){location.href='CustomerOverview?requestType=customerDetails&accountNumber=' + ${transaction.accountNumber}};
		messageDiv.onmouseout = function(){closePreview(this.id)};
		if(${transaction.fraudulent} == true){	
			messageDiv.style.backgroundColor = "rgba(216,20,20,0.25)";
			messageDiv.innerHTML = 'TransactionId: ' + ${transaction.transactionId} + '<br>' +
			<%--'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=' + message.data.accountNumber +'">' + message.data.accountNumber + '</a><br>' + --%>
			'Account Number: ' + ${transaction.accountNumber}; 	
		}else{
			messageDiv.style.backgroundColor = "rgba(63,171,42,.25)";
			messageDiv.innerHTML = 'TransactionId: ' + ${transaction.transactionId} + '<br>' +
			<%--'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=' + message.data.accountNumber +'">' + message.data.accountNumber + '</a><br>' + --%>
		'Account Number: ' + ${transaction.accountNumber};
		}	
		previewDiv.className = "chart";
		previewDiv.id = "preview${transaction.transactionTimeStamp}";
		previewDiv.innerHTML = 'TransactionId: ${transaction.transactionId} <br>' +
		'Account Number:  <a href="CustomerOverview?requestType=customerDetails&accountNumber=${transaction.accountNumber}"> ${transaction.accountNumber} </a><br>' +
		'Account Type: ${transaction.accountType} <br>' +
		'Amount: ${transaction.amount} <br>' +
		'Merchant Id: ${transaction.merchantId} <br>' +
		'Merchant Type: ${transaction.merchantType} <br>' +
		'Time of Transaction: ${transaction.transactionTimeStamp} <br><br>';
		if(${transaction.fraudulent} == true){
			previewDiv.innerHTML = previewDiv.innerHTML + 'Reason Flagged: ' + reasonFlagged;
		}
		chartDiv.className = "chart"
		chartDiv.id = "chart" + ${transaction.transactionTimeStamp};
		//chartDiv.onclick = function(){showPreview(this.id)};
		//chartDiv.onmouseout = function(){closePreview(this.id)};
							
		iDiv.appendChild(messageDiv);
		pDiv.appendChild(previewDiv);
		pDiv.appendChild(chartDiv);
		drawSDChartInitial(0, 0, ${transaction.amount}, 0, 0, ${transaction.distanceFromPrev}, ${transaction.transactionTimeStamp});
	</c:forEach>
  }
  
  function drawSDChart(message){
      var chartValues = message;
      //var amountMean = chartValues.data.amountMean;
      //var amountDev = chartValues.data.amountDev;
      //var amount = chartValues.data.amount;
      //var distanceMean = chartValues.data.distanceMean; 
	  //var distanceDev = chartValues.data.distanceDev;
	  //var distancePrev = chartValues.data.distancePrev;
	  var amountMean = chartValues.data.amountmean;
      var amountDev = chartValues.data.amountdev;
      var amount = chartValues.data.amount;
      var distanceMean = chartValues.data.distancemean; 
	  var distanceDev = chartValues.data.distancedev;
	  var distancePrev = chartValues.data.distance;
	  var timeMean = 60;
	  var timeDev = 22;
	  var time = 50;
	  
	  var transactionTimeStamp = chartValues.data.transactiontimestamp
	  
      $(function () {

          $('#chart' +transactionTimeStamp).highcharts({
              chart: {
                  type: 'columnrange',
                  inverted: true,
                  width: 450,
                  height: 250
              },

              title: {
                  text: 'Customer Transaction Range'
              },

              subtitle: {
                  text: ''
              },

              xAxis: {
                  categories: ['Amount', 'Distance', 'Time']
              },

              yAxis: {
                  title: {
                      text: 'Range Deviation'
                  }
              },

              tooltip: {
                  valueSuffix: ''
              },

              plotOptions: {
                  columnrange: {
                      dataLabels: {
                          enabled: true,
                          formatter: function () {
                              
                          }
                      }
                  }
              },

              legend: {
                  enabled: false
              },
				
              series: [{
                  name: 'Range',
                  data: [
                         
                      [amountMean-amountDev, amountMean+amountDev],	
                      [distanceMean-distanceDev, distanceMean+distanceDev],
                      [timeMean-timeDev, timeMean+timeDev]
                  	]
              	},
              	{
                  type: 'scatter',
           		  name: 'Actual',
                  data: [amount, distancePrev, time]
               	}
              ]
          });

      });
  }
  
  function drawSDChartInitial(customerAmountMean, customerAmountDev,  transactionAmount, customerDistanaceMean, customerDistanceDev, distanceFromPrev, timeStamp){
      var amountMean = customerAmountMean;
      var amountDev = customerAmountDev;
      var amount = transactionAmount;
      var distanceMean = customerDistanaceMean; 
	  var distanceDev = customerDistanceDev;
	  var distancePrev = distanceFromPrev;
	  var timeMean = 60;
	  var timeDev = 22;
	  var time = 50;
	  
      $(function () {

          $('#chart' + timeStamp).highcharts({
              chart: {
                  type: 'columnrange',
                  inverted: true,
                  width: 450,
                  height: 250
              },

              title: {
                  text: 'Customer Transaction Range'
              },

              subtitle: {
                  text: ''
              },

              xAxis: {
                  categories: ['Amount', 'Distance', 'Time']
              },

              yAxis: {
                  title: {
                      text: 'Range Deviation'
                  }
              },

              tooltip: {
                  valueSuffix: ''
              },

              plotOptions: {
                  columnrange: {
                      dataLabels: {
                          enabled: true,
                          formatter: function () {
                              
                          }
                      }
                  }
              },

              legend: {
                  enabled: false
              },
				
              series: [{
                  name: 'Range',
                  data: [
                         
                      [amountMean-amountDev, amountMean+amountDev],	
                      [distanceMean-distanceDev, distanceMean+distanceDev],
                      [timeMean-timeDev, timeMean+timeDev]
                  	]
              	},
              	{
                  type: 'scatter',
           		  name: 'Actual',
                  data: [amount, distancePrev, time]
               	}
              ]
          });

      });
  }
  
  function showPreview(id){
	  console.log(id);
	  prevColor = document.getElementById(id).style.backgroundColor;
	  selected = true;
	  document.getElementById(id).style.backgroundColor = "#D6E9F8";
	  document.getElementById("preview"+id).style.display = "inline";
	  document.getElementById("chart"+id).style.display = "inline";
  }
  
  function closePreview(id){
	if(selected == true){
		console.log(id);
		document.getElementById(id).style.backgroundColor = prevColor;
		document.getElementById("preview"+id).style.display = "none";
		document.getElementById("chart"+id).style.display = "none";
		selected = false;
	}
  }
  
  function clearSelect(){
	  var messages = document.getElementById("inbox").childNodes;
		for(i=1; i <= messages.length; i++){
			document.getElementById("preview"+messages[i].id).style.display = "none";
			document.getElementById(+messages[i].id).style.backgroundColor = "#FFFFFF";
		}
  }
  
	function generateMessages(){
		var evenColor = "#F5F5F5";
		var oddColor = "#FFFFFF";
		var iDiv = document.getElementById("inbox");
		
		// Now create and append to iDiv
		
		for(i=0; i<=15; i++){
			var innerDiv = document.createElement("div");
			innerDiv.className = "message";
			innerDiv.id = "message" + i;
			iDiv.appendChild(innerDiv);
			if(i%2){
				innerDiv.style.backgroundColor = evenColor; 
			}else{
				innerDiv.style.backgroundColor = oddColor;
			}
		}
	}
	
	function setCellColor(){
		var evenColor = "#F5F5F5";
		var oddColor = "#FFFFFF";
		
		var messages = document.getElementById("inbox").childNodes;
		for(i=1; i <= messages.length; i++){
			if(i%2==0){
				messages[i].style.backgroundColor = evenColor; 
			}else{
				messages[i].style.backgroundColor = oddColor;
			}
		}
	}
</script>
</head>
 
<!-- <body onLoad="renderInitialInboxItems();">  -->
<body onLoad="">
	<div class="header">
		<div id="brandingLayout">
                <a class="brandingContent">
                    <img src="images/hortonworks-logo-new.png" width="200px"/>
                    <span class="brandTitle" data-i18n="BRAND_TITLE"></span>
                </a>
		</div>
	</div>
	
	<div class="envolope">
	<div id="inboxHeader" class="inbox_header"></div>
	<div id="bodyContainer" class="body_container">
		<!-- <div class="menu_pane_container">
			<div class="menu_pane">
				<div class="menu_item"></div>
			</div>
		</div>  -->
		<div class="pane_container">
			<div class="message_pane_heading"></div>
			<div id="inbox" class="message_pane">
				<%--<c:forEach items="${transactionHistory}" var="transaction">
					<div class="message">
						<table>
							<tr><td>TransctionId</td><td></tr>
							<tr><td><a href="CustomerOverview?requestType=customerDetails&accountNumber=${transaction.accountNumber}">${transaction.transactionId} ${transaction.accountNumber}</a></td></tr> 
						</table>
					</div>
          		</c:forEach> --%>
			</div>
		</div>
		<div class="pane_container">
			<div id="previewPane" class="preview_pane"></div>
		</div>
	</div>
	<div class="footer_container"></div>
	</div>
</body>
</html>