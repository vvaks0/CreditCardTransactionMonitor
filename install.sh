#!/bin/bash
VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
INTVERSION=$(echo $VERSION*10 | bc | grep -Po '([0-9][0-9])')
echo "*********************************SANDBOX VERSION IS $VERSION" 
if [ "$INTVERSION" -lt 24 ]; then	
	echo "*********************************Downloading NIFI..." 
	sudo git clone https://github.com/abajwa-hw/ambari-nifi-service.git   /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI
	service ambari restart
else
	echo "*********************************Install Zeppelin Notebook"
	cp -rvf Zeppelin/notebook/* /usr/hdp/current/zeppelin-server/lib/notebook/  
fi

# Wait for Ambari
LOOPESCAPE="false"
until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -I -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox | grep -Po 'OK')
        if [ "$TASKSTATUS" == OK ]; then
                LOOPESCAPE="true"
                TASKSTATUS="READY"
        else
        		AUTHSTATUS=$(curl -u admin:admin -I -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox | grep HTTP | grep -Po '( [0-9]+)'| grep -Po '([0-9]+)')
                if [ "$AUTHSTATUS" == 403 ]; then
                	echo "THE AMBARI PASSWORD IS NOT SET TO: admin"
                	echo "RUN COMMAND: ambari-admin-password-reset, SET PASSWORD: admin"
                	exit 403
                else
                	TASKSTATUS="PENDING"
                fi
        fi
		echo "Waiting for Ambari..."
        echo "Ambari Status... " $TASKSTATUS
        sleep 2
done

echo "*********************************Changing YARN Container Memory Size..."
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox yarn-site "yarn.scheduler.maximum-allocation-mb" "6144"
sleep 2
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox yarn-site "yarn.nodemanager.resource.memory-mb" "6144"

# Ensure that Yarn is not in a transitional state
YARNSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/YARN | grep '"state" :' | grep -Po '([A-Z]+)')
sleep 2
echo "YARN STATUS: $YARNSTATUS"
LOOPESCAPE="false"
if ! [[ "$YARNSTATUS" == STARTED || "$YARNSTATUS" == INSTALLED ]]; then
        until [ "$LOOPESCAPE" == true ]; do
                TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/YARN | grep '"state" :' | grep -Po '([A-Z]+)')
                if [[ "$TASKSTATUS" == STARTED || "$TASKSTATUS" == INSTALLED ]]; then
                        LOOPESCAPE="true"
                fi
                echo "*********************************Task Status" $TASKSTATUS
                sleep 2
        done
fi

sleep 2
echo "*********************************Restarting YARN..."
if [ "$YARNSTATUS" == STARTED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Stop YARN"}, "ServiceInfo": {"state": "INSTALLED"}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/YARN | grep "id" | grep -Po '([0-9]+)')
        echo "*********************************AMBARI TaskID " $TASKID
        sleep 2
        LOOPESCAPE="false"
        until [ "$LOOPESCAPE" == true ]; do
                TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
                if [ "$TASKSTATUS" == COMPLETED ]; then
                        LOOPESCAPE="true"
                fi
                echo "*********************************Task Status" $TASKSTATUS
                sleep 2
        done
elif [ "$YARNSTATUS" == INSTALLED ]; then
        echo "YARN Service Stopped..."
fi

TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Start YARN"}, "ServiceInfo": {"state": "STARTED"}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/YARN | grep "id" | grep -Po '([0-9]+)')
echo "*********************************AMBARI TaskID " $TASKID
sleep 2
LOOPESCAPE="false"
until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
        if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
        fi
        echo "*********************************Task Status" $TASKSTATUS
        sleep 2
done

echo "*********************************Creating NIFI service..."
# Create NIFI service
curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/NIFI

sleep 2
echo "*********************************Adding NIFI MASTER component..."
# Add NIFI Master component to service
curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/NIFI/components/NIFI_MASTER

sleep 2
echo "*********************************Creating NIFI configuration..."

# Create and apply configuration
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox nifi-ambari-config /root/CreditCardTransactionMonitor/Nifi/config/nifi-ambari-config.json
sleep 2
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox nifi-bootstrap-env /root/CreditCardTransactionMonitor/Nifi/config/nifi-bootstrap-env.json
sleep 2
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox nifi-flow-env /root/CreditCardTransactionMonitor/Nifi/config/nifi-flow-env.json
sleep 2
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox nifi-logback-env /root/CreditCardTransactionMonitor/Nifi/config/nifi-logback-env.json
sleep 2
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox.hortonworks.com Sandbox nifi-properties-env /root/CreditCardTransactionMonitor/Nifi/config/nifi-properties-env.json

sleep 2
echo "*********************************Adding NIFI MASTER role to Host..."
# Add NIFI Master role to Sandbox host
curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/hosts/sandbox.hortonworks.com/host_components/NIFI_MASTER

sleep 2
echo "*********************************Installing NIFI Service"
# Install NIFI Service
TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Nifi"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/NIFI | grep "id" | grep -Po '([0-9]+)')
echo "*********************************AMBARI TaskID " $TASKID
sleep 2
LOOPESCAPE="false"
until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
        if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
        fi
        echo "*********************************Task Status" $TASKSTATUS
        sleep 2
done
echo "*********************************NIFI Service Installed..."

sleep 2
echo "*********************************Starting NIFI Service..."
# Start NIFI service
TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start NIFI"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/NIFI | grep "id" | grep -Po '([0-9]+)')
echo "*********************************AMBARI TaskID " $TASKID
sleep 2
LOOPESCAPE="false"
until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
        if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
        fi
        echo "*********************************Task Status" $TASKSTATUS
        sleep 2
done
echo "*********************************NIFI Service Started..."
LOOPESCAPE="false"
until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -i -X GET http://sandbox.hortonworks.com:9090/nifi-api/controller | grep -Po 'OK')
        if [ "$TASKSTATUS" == OK ]; then
                LOOPESCAPE="true"
        else
                TASKSTATUS="PENDING"
        fi
		echo "*********************************Waiting for NIFI Servlet..."
        echo "*********************************NIFI Servlet Status... " $TASKSTATUS
        sleep 2
done

echo "*********************************Importing NIFI Template..."
# Import NIFI Template
#TEMPLATEID=$(curl -v -F template=@"/root/CreditCardTransactionMonitor/Nifi/template/CreditFraudDetectionFlow.xml" -X POST http://sandbox.hortonworks.com:9090/nifi-api/controller/templates | grep -Po '<id>([a-z0-9-]+)' | grep -Po '>([a-z0-9-]+)' | grep -Po '([a-z0-9-]+)')
TEMPLATEID=$(curl -v -F template=@"Nifi/template/CreditFraudDetectionFlow.xml" -X POST http://sandbox.hortonworks.com:9090/nifi-api/controller/templates | grep -Po '<id>([a-z0-9-]+)' | grep -Po '>([a-z0-9-]+)' | grep -Po '([a-z0-9-]+)')
sleep 2
echo "*********************************Instantiating NIFI Flow..."
# Instantiate NIFI Template
REVISION=$(curl -u admin:admin  -i -X GET http://sandbox.hortonworks.com:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')
curl -u admin:admin -i -H "Content-Type:application/x-www-form-urlencoded" -d "templateId=$TEMPLATEID&originX=100&originY=100&version=$REVISION" -X POST http://sandbox.hortonworks.com:9090/nifi-api/controller/process-groups/root/template-instance

echo "*********************************Installing Maven"
# Install Maven
wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
yum install -y apache-maven

echo "*********************************Building Credit Card Transaction Monitor Storm Topology"
# Build from source
cd CreditCardTransactionMonitor
mvn clean package
cp -vf target/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar /home/storm

echo "*********************************Building Credit Card Transaction Simulator"
# Build from source
cd ../CreditCardTransactionSimulator
mvn clean package
cp -vf target/CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../

#Start Kafka
KAFKASTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/KAFKA | grep '"state" :' | grep -Po '([A-Z]+)')

if [ "$KAFKASTATUS" == INSTALLED ]; then
	echo "*********************************Starting Kafka Broker..."
	TASKID=$(curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/KAFKA | grep "id" | grep -Po '([0-9]+)')
	echo "*********************************AMBARI TaskID " $TASKID
	sleep 2
	LOOPESCAPE="false"
	until [ "$LOOPESCAPE" == true ]; do
		TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
		if [ "$TASKSTATUS" == COMPLETED ]; then
			LOOPESCAPE="true"
 		fi
		echo "*********************************Task Status" $TASKSTATUS
		sleep 2
	done
	echo "*********************************Kafka Broker Started..."
elif [ "$KAFKASTATUS" == STARTED ]; then
	echo "*********************************Kafka Broker Started..."
else
	echo "*********************************Kafka Broker in a transition state. Wait for process to complete and then run the install script again."
	exit 1
fi

#Configure Kafka
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper sandbox.hortonworks.com:2181 --replication-factor 1 --partitions 1 --topic IncomingTransactions
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper sandbox.hortonworks.com:2181 --replication-factor 1 --partitions 1 --topic CustomerTransactionValidation

#Start HBASE
HBASESTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/HBASE | grep '"state" :' | grep -Po '([A-Z]+)')
if [ "$HBASESTATUS" == INSTALLED ]; then
	echo "*********************************Starting  Hbase Service..."
	TASKID=$(curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo": {"context" :"Start Hbase via REST"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/HBASE | grep "id" | grep -Po '([0-9]+)')
	echo "*********************************HBASE TaskId " $TASKID
	sleep 2
	LOOPESCAPE="false"
	until [ "$LOOPESCAPE" == true ]; do
		TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
		if [ "$TASKSTATUS" == COMPLETED ]; then
			LOOPESCAPE="true"
 		fi
		echo "*********************************Task Status" $TASKSTATUS
		sleep 2
	done
	echo "*********************************Hbase Service Started..."
elif [ "$HBASESTATUS" == STARTED ]; then
	echo "*********************************Hbase Service  Started..."
else
	echo "*********************************Hbase Service in a transition state. Wait for process to complete and then run the install script again."
	exit 1
fi

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
mkdir /home/docker/
mkdir /home/docker/dockerbuild/
mkdir /home/docker/dockerbuild/transactionmonitorui
cd ../SliderConfig
cp -vf appConfig.json /home/docker/dockerbuild/transactionmonitorui
cp -vf metainfo.json /home/docker/dockerbuild/transactionmonitorui
cp -vf resources.json /home/docker/dockerbuild/transactionmonitorui
cd ..

STORMSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/STORM | grep '"state" :' | grep -Po '([A-Z]+)')
if [ "$STORMSTATUS" == INSTALLED ]; then
	echo "*********************************Starting Storm Service..."
	TASKID=$(curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}}' http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/services/STORM | grep "id" | grep -Po '([0-9]+)')
	echo "*********************************STORM TaskId " $TASKID
	sleep 2
	LOOPESCAPE="false"
	until [ "$LOOPESCAPE" == true ]; do
		TASKSTATUS=$(curl -u admin:admin -X GET http://sandbox.hortonworks.com:8080/api/v1/clusters/Sandbox/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
		if [ "$TASKSTATUS" == COMPLETED ]; then
			LOOPESCAPE="true"
 		fi
		echo "*********************************Task Status" $TASKSTATUS
		sleep 2
	done
	echo "*********************************Storm Broker Started..."
elif [ "$STORMSTATUS" == STARTED ]; then
	echo "*********************************Storm Service Started..."
else
	echo "*********************************Storm Service in a transition state. Wait for process to complete and then run the install script again."
	exit 1
fi

# Start NIFI Flow
echo "*********************************Starting NIFI Flow..."
REVISION=$(curl -u admin:admin  -i -X GET http://sandbox.hortonworks.com:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')
TARGETS=($(curl -u admin:admin -i -X GET http://sandbox.hortonworks.com:9090/nifi-api/controller/process-groups/root/processors | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
length=${#TARGETS[@]}
for ((i = 0; i != length; i++)); do
   echo curl -u admin:admin -i -X GET ${TARGETS[i]}
   echo "Current Revision: " $REVISION
   curl -u admin:admin -i -H "Content-Type:application/x-www-form-urlencoded" -d "state=RUNNING&version=$REVISION" -X PUT ${TARGETS[i]}
   REVISION=$(curl -u admin:admin  -i -X GET http://sandbox.hortonworks.com:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')
done

# Deploy Storm Topology
echo "*********************************Deploying Storm Topology..."
storm jar /home/storm/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.financial.topology.CreditCardTransactionMonitorTopology

echo "*********************************Downloading Docker Images for UI..."
# Download Docker Images
service docker start
docker pull vvaks/transactionmonitorui
docker pull vvaks/cometd

# Create Phoenix TransactionHistory View
#/usr/hdp/current/phoenix-client/bin/sqlline.py localhost:2181:/hbase-unsecure
tee createTransactionHistoryView.sql <<-'EOF'
create view "TransactionHistory" (pk VARCHAR PRIMARY KEY, "Transactions"."merchantType" VARCHAR, "Transactions"."frauduent" VARCHAR);
EOF

echo "Sleeping 30 seconds to allow Storm Topology to create HBase tables"
sleep 30
/usr/hdp/current/phoenix-client/bin/psql.py sandbox.hortonworks.com createTransactionHistoryView.sql

# Reboot to refresh configuration
reboot now