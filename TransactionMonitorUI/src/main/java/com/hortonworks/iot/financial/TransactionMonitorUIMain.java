package com.hortonworks.iot.financial;

import java.util.Map;

import org.apache.catalina.startup.Tomcat;

public class TransactionMonitorUIMain {
	public static void main(String [] args) throws Exception {
    	//String contextPath = "/";
        String appBase = ".";
        Integer tomcatListenPort = 8090;
        Map<String, String> env = System.getenv();
        System.out.println("********************** ENV: " + env);
		if(env.get("TOMCAT_PORT") != null){
        	tomcatListenPort = Integer.valueOf((String)env.get("TOMCAT_PORT"));
        }
		System.out.println("********************** TOMCAT PORT: " + String.valueOf(tomcatListenPort));
        Tomcat tomcat = new Tomcat();     
        tomcat.setPort(tomcatListenPort);
        tomcat.getHost().setAppBase(appBase);
        tomcat.addWebapp("/TransactionMonitorUI", appBase);
        System.out.println("****************** Starting Tomcat Server....");
        tomcat.start();
        System.out.println("****************** Starting Tomcat Server Started");
        System.out.println("****************** Acquiring Tomcat Server....");
        tomcat.getServer().await();
        System.out.println("****************** Acquired Tomcat Server....");
	}

}
