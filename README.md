# CreditCardTransactionMonitor
Credit Card Transaction Monitor is an example of a Modern Data Application running on the Hortonworks Connected Platform (HDP/HDF). The application shows how a financial institution can use Hortonworks Data Flow and Hortonworks Data Platform to protect credit card customers from credit card fraud.

Download and Import Hortonworks Sandbox 2.3.2 (should work with Sandbox 2.4 but has not been tested) for Virtual Box. Should work with VMWare but has not been tested. Modify local hosts file so that sandbox.hortonworks.com resolves to 127.0.0.1 (This is important and may break the simulator and UI) Start Sandbox, SSH to Sandbox, Change sandbox root password From Ambari (http://sandbox.hortonworks.com:8080)

Start Kafka (This is important, if Kafka is not started, the install script will not be able to configure the required Kafka Topics) ssh to sandbox as root into /root directory 

git clone https://github.com/vakshorton/CreditCardTransactionMonitor.git 

(make sure that git cloned to /CreditCardTransactionMonitor)

cd DeviceManagerDemo

chmod 755 install.sh

./install.sh

From Ambari:

Increase Yarn memory per container to 5GB (This is important as the default setting of 2GB is not enough to support the application servers on Yarn)

Install Nifi using Add Service button Reboot Sandbox
Configure Virtual Box Port Forward

8082 – HDF_HTTP_Ingest

8090 - MapUI

8091 - Cometd

9090 – HDF_Studio

From Ambari Admin

start Nifi, Hbase, Kafka, Storm
From the NiFi Studio interface (http://sandbox.hortonworks.com:9090/nifi)

Import CreditFraudDetectionFlow.xml as a template into Nifi 

(The template is in the NifiFlow floder. Nifi allows you to browse the local machine so it may be easier to download a copy locally directly from git)

Take note that there are two processors (getSQS and PostHTTP) at the begining and at the end of the flow that require API keys. These processors are only required to enable the mobile app that comes with the demp to work. The code for that app has not been published just yet so for the time being the application will work without the app or any api keys.

Make sure to start all of the processors, should just need to hit the green start button as all of the processors will be selected after import

Make sure that docker is running: service docker status. If not, start it:

service docker start

Start Application Servers on Slider:

slider create transactionmonitorui --template /usr/hdp/docker/dockerbuild/transactionmonitorui/appConfig.json --metainfo /usr/hdp/docker/dockerbuild/transactionmonitorui/metainfo.json --resources /usr/hdp/docker/dockerbuild/transactionmonitorui/resources.json

(Slider will download the docker containers from the docker hub so it may take a few minutes for the application server to start)

Deploy Storm Topology:

storm jar /home/storm/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.financial.topology.CreditCardTransactionMonitorTopology

Add TransactionHistory aggregate Phoenix View:

usr/hdp/2.3.2.0-2950/phoenix/bin/sqlline.py localhost:2181:/hbase-unsecure create view "TransactionHistory" (pk VARCHAR PRIMARY KEY, "Transactions"."merchantType" VARCHAR, "Transactions"."frauduent" VARCHAR);

Build Simulator inside of Sandbox

cd /root/DeviceManagerDemo/DeviceSimulator

mvn clean package

scp target/DeviceSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar to the local machine

Start Simulation on Host (Not inside VM): USAGE:

java -jar simulator.jar arg1=Simulator-Type{Customer} arg2=EntityId{1000} arg3={Simulation|Training}

Example: java -jar CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar Customer 1000 Simulation
