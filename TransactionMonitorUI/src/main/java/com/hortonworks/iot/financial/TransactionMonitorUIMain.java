package com.hortonworks.iot.financial;

import org.apache.catalina.startup.Tomcat;

public class TransactionMonitorUIMain {
	public static void main(String [] args) throws Exception {
    	//String contextPath = "/";
        String appBase = ".";
        
        Tomcat tomcat = new Tomcat();     
        tomcat.setPort(Integer.parseInt("8090"));
        tomcat.getHost().setAppBase(appBase);
        tomcat.addWebapp("/TransactionMonitorUI", appBase);
        
        tomcat.start();
        tomcat.getServer().await();
	}

}
