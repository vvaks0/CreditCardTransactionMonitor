# CreditCardTransactionMonitor
Credit Card Transaction Monitor is an example of a Modern Data Application running on the Hortonworks Connected Platform (HDP/HDF). The application shows how a financial institution can use Hortonworks Data Flow and Hortonworks Data Platform to protect credit card customers from credit card fraud.

# Install CreditCardTransactionMonitor
Download and Import Hortonworks Sandbox 2.4 for Virtual Box. Should work with VMWare but has not been tested. Modify local hosts file so that sandbox.hortonworks.com resolves to 127.0.0.1 (This is important and may break the simulator and UI) 

Start Sandbox, SSH to Sandbox (ssh root@sandbox.hortonworks.com -p 2222)

Change sandbox root password (passwd)

Change Ambari password to "admin" (ambari-admin-password-reset) (Ambari passwords needs to be "admin" as scripts depend on it)

cd /root (use the /root directory to begin the install)

git clone https://github.com/vakshorton/CreditCardTransactionMonitor.git (make sure that git cloned to /root/CreditCardTransactionMonitor)

cd /root/CreditCardTransactionMonitor

./install.sh

Reboot Sandbox (reboot now)

Configure Virtual Box Port Forward

8082 – HDF_HTTP_Ingest

8090 - TransactionMonitorUI

8091 - Cometd

9090 – HDF_Studio

# Start Demo
The script to start demo services should be located in the CreditCardTransactionMonitor directory
./startDemoServices.sh

Slider will download the servlet (UI) docker containers from the docker hub so it may take a few minutes for the application server to start

From the browser: http://sandbox.hortonworks.com:8090/TransactionMonitorUI/CustomerOverview

Copy the resulting jar to the host machine

Start Simulation on Host (Not inside VM):

The Simulator should be located in the CreditCardTransactionMonitor directory

java -jar CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar Customer 1000 Simulation

USAGE:

java -jar simulator.jar arg1=Simulator-Type{Customer} arg2=EntityId{1000} arg3={Simulation|Training}

Example:
java -jar CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar Customer 1000 Simulation
