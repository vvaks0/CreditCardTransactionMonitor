# CreditCardTransactionMonitor
Credit Card Transaction Monitor is an example of a Modern Data Application running on the Hortonworks Connected Platform (HDP/HDF). The application shows how a financial institution can use Hortonworks Data Flow and Hortonworks Data Platform to protect credit card customers from credit card fraud.

Download and Import Hortonworks Sandbox 2.4 for Virtual Box. Should work with VMWare but has not been tested. Modify local hosts file so that sandbox.hortonworks.com resolves to 127.0.0.1 (This is important and may break the simulator and UI) 

Start Sandbox, SSH to Sandbox (ssh root@sandbox.hortonworks.com -p 2222)

Change sandbox root password (passwd)

Change Ambari password to "admin" (ambari-admin-password-reset) (Ambari passwords needs to be "admin" as scripts depend on it)

cd /root (use the /root directory to begin the install)

git clone https://github.com/vakshorton/CreditCardTransactionMonitor.git (make sure that git cloned to /root/CreditCardTransactionMonitor)

cd /root/CreditCardTransactionMonitor

chmod 755 install.sh 

./install.sh

From Ambari:

- Increase Yarn memory per container to 6GB (This is important as the default setting of 2GB is not enough to support the application servers on Yarn) 

- Install Nifi using Add Service button

Reboot Sandbox

Configure Virtual Box Port Forward

8082 – HDF_HTTP_Ingest

8090 - MapUI

8091 - Cometd

9090 – HDF_Studio

From Ambari Admin 

 - start Nifi, Hbase, Kafka, Storm

From the NiFi Studio interface (http://sandbox.hortonworks.com:9090/nifi), Import CreditFraudDetectionFlow.xml as a template into Nifi (The template is in the NifiFlow floder. Nifi allows you to browse the local machine so it may be easier to download a copy locally directly from git)

Make sure to start all of the processors, should just need to hit the green start button as all of the processors will be selected after import

Make sure that docker is running: service docker status. If not, start it: 

service docker start

Start Application Servers on Slider:

slider create transactionmonitorui --template /home/docker/dockerbuild/transactionmonitorui/appConfig.json --metainfo /home/docker/dockerbuild/transactionmonitorui/metainfo.json --resources /home/docker/dockerbuild/transactionmonitorui/resources.json 

(Slider will download the docker containers from the docker hub so it may take a few minutes for the application server to start)

Add TransactionHistory aggregate Phoenix View:

usr/hdp/2.3.2.0-2950/phoenix/bin/sqlline.py localhost:2181:/hbase-unsecure
create view "TransactionHistory" (pk VARCHAR PRIMARY KEY, "Transactions"."merchantType" VARCHAR, "Transactions"."frauduent" VARCHAR);

Start Simulation on Host (Not inside VM):
USAGE:

java -jar simulator.jar arg1=Simulator-Type{Customer} arg2=EntityId{1000} arg3={Simulation|Training}

Example:
java -jar CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar Customer 1000 Simulation
