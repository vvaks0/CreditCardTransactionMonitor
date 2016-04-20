#!/bin/bash

cd CreditCardTransactionMonitor
mvn clean package
cp -vf target/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar /home/storm

#Start Kafka
#!/bin/bash

KAFKASTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/KAFKA | grep '"state" :' | grep -Po '([A-Z]+)')

if [ "$KAFKASTATUS" == INSTALLED ]; then
	echo "Starting Kafka Broker..."
	TASKID=$(curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/KAFKA | grep "id" | grep -Po '([0-9]+)')
	echo "AMBARI TaskID " $TASKID
	sleep 2

	LOOPESCAPE="false"

	until [ "$LOOPESCAPE" == true ]; do

		TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
		if [ "$TASKSTATUS" == COMPLETED ]; then
			LOOPESCAPE="true"
 		fi
		
		echo "Task Status" $TASKSTATUS
		sleep 2

	done
	echo "Kafka Broker Started..."

elif [ "$KAFKASTATUS" == STARTED ]; then
	echo "Kafka Broker Started..."
else
	echo "Kafka Broker in a transition state. Wait for process to complete and then run the install script again."
	exit 1
fi

#Configure Kafka
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper sandbox.hortonworks.com:2181 --replication-factor 1 --partitions 1 --topic IncomingTransactions
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper sandbox.hortonworks.com:2181 --replication-factor 1 --partitions 1 --topic CustomerTransactionValidation

#Install and start Docker
tee /etc/yum.repos.d/docker.repo <<-'EOF'
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/$releasever/
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF

rpm -iUvh http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
yum -y install docker-io
groupadd docker
gpasswd -a yarn docker
service docker start
chkconfig --add docker
chkconfig docker on
sudo -u hdfs hadoop fs -mkdir /user/root/
sudo -u hdfs hadoop fs -chown root:hdfs /user/root/

#Create Docker working folder
mkdir /usr/hdp/docker/
mkdir /usr/hdp/docker/dockerbuild/
mkdir /usr/hdp/docker/dockerbuild/transactionmonitorui
cd ../SliderConfig
cp -vf appConfig.json /usr/hdp/docker/dockerbuild/transactionmonitorui
cp -vf metainfo.json /usr/hdp/docker/dockerbuild/transactionmonitorui
cp -vf resources.json /usr/hdp/docker/dockerbuild/transactionmonitorui

slider create transactionmonitorui --template /usr/hdp/docker/dockerbuild/transactionmonitorui/appConfig.json --metainfo /usr/hdp/docker/dockerbuild/transactionmonitorui/metainfo.json --resources /usr/hdp/docker/dockerbuild/transactionmonitorui/resources.json
storm jar /home/storm/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.financial.topology.CreditCardTransactionMonitorTopology

#Install NiFi Service in Ambari. Still need to log into Ambari and install the service from the console
#VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
#sudo git clone https://github.com/abajwa-hw/ambari-nifi-service.git   /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI
#service ambari restart

#/usr/hdp/current/phoenix-client/bin/sqlline.py localhost:2181:/hbase-unsecure
#create view "TransactionHistory" (pk VARCHAR PRIMARY KEY, "Transactions"."merchantType" VARCHAR, "Transactions"."frauduent" VARCHAR);