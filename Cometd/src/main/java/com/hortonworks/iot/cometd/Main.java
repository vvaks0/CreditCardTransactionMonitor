package com.hortonworks.iot.cometd;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.server.CometDServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

	public static void main(String[] args) throws Exception {
		// Configure Jetty
		Server server = new Server(8091);
		
		ServletContextHandler webAppContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        webAppContext.setContextPath("/");
       
        System.out.println("Loading Cometd PubSub.....");
		// Configure Cometd
		CometDServlet cometdServlet = new CometDServlet();
		webAppContext.addServlet(new ServletHolder(cometdServlet), "/cometd/*");
		server.setHandler(webAppContext);
		
		System.out.println("Starting Jetty Server....."); 
		// Start Jetty
		server.start();
		
		System.out.println("Creating PubSub Topics.....");
		// Create channels to publish on
		BayeuxServer bayeux = cometdServlet.getBayeux();
		createChannel(bayeux, "/devicestatus");
		createChannel(bayeux, "/technicianstatus");
		createChannel(bayeux, "/alert");
		createChannel(bayeux, "/prediction");
		createChannel(bayeux, "/fraudAlert");
		createChannel(bayeux, "/incomingTransactions");
		createChannel(bayeux, "/accountStatusUpdate");
		createChannel(bayeux, "/bioReactorStatus");
		createChannel(bayeux, "/filtrationStatus");
		//final ServerChannel channel = bayeux.getChannel("");
		
		server.join();
	}
	
	public static void createChannel(BayeuxServer bayeux, String channelName){
		bayeux.createChannelIfAbsent(channelName, new ServerChannel.Initializer()
		{
			public void configureChannel(ConfigurableServerChannel channel)
			{
				channel.setPersistent(true);
				System.out.println(channel.getChannelId());
			}
		});
	}
}
