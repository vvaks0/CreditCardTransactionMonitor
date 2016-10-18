#!/bin/bash

AMBARI_HOST=$(hostname -f)
echo "*********************************AMABRI HOST IS: $AMBARI_HOST"

CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "([a-zA-Z]+)'|grep -Po '[a-zA-Z]+')

if [[ -z $CLUSTER_NAME ]]; then
        echo "Could not connect to Ambari Server. Please run the install script on the same host where Ambari Server is installed."
        exit 1
else
       	echo "*********************************CLUSTER NAME IS: $CLUSTER_NAME"
fi

getServiceStatus () {
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')

       	echo $SERVICE_STATUS
}

waitForService () {
       	# Ensure that Service is not in a transitional state
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
       	sleep 2
       	echo "$SERVICE STATUS: $SERVICE_STATUS"
       	LOOPESCAPE="false"
       	if ! [[ "$SERVICE_STATUS" == STARTED || "$SERVICE_STATUS" == INSTALLED ]]; then
        until [ "$LOOPESCAPE" == true ]; do
                SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
            if [[ "$SERVICE_STATUS" == STARTED || "$SERVICE_STATUS" == INSTALLED ]]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************$SERVICE Status: $SERVICE_STATUS"
            sleep 2
        done
       	fi
}

startService (){
       	SERVICE=$1
       	SERVICE_STATUS=$(getServiceStatus $SERVICE)
       		echo "*********************************Starting Service $SERVICE ..."
       	if [ "$SERVICE_STATUS" == INSTALLED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Start $SERVICE"}, "ServiceInfo": {"state": "STARTED"}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

        echo "*********************************Start $SERVICE TaskID $TASKID"
        sleep 2
        LOOPESCAPE="false"
        until [ "$LOOPESCAPE" == true ]; do
            TASKSTATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
            if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************Start $SERVICE Task Status $TASKSTATUS"
            sleep 2
        done
        echo "*********************************$SERVICE Service Started..."
       	elif [ "$SERVICE_TATUS" == STARTED ]; then
       	echo "*********************************$SERVICE Service Started..."
       	fi
}

#Start HDFS
HDFS_STATUS=$(getServiceStatus HDFS)
echo "*********************************Checking HDFS status..."
if ! [[ $HDFS_STATUS == STARTED || $HDFS_STATUS == INSTALLED ]]; then
       	echo "*********************************HDFS is in a transitional state, waiting..."
       	waitForService HDFS
       	echo "*********************************HDFS has entered a ready state..."
fi

if [[ $HDFS_STATUS == INSTALLED ]]; then
       	startService HDFS
else
       	echo "*********************************HDFS Service Started..."
fi

sleep 1
#Start YARN
YARN_STATUS=$(getServiceStatus YARN)
echo "*********************************Checking YARN status..."
if ! [[ $YARN_STATUS == STARTED || $YARN_STATUS == INSTALLED ]]; then
       	echo "*********************************YARN is in a transitional state, waiting..."
       	waitForService YARN
       	echo "*********************************YARN has entered a ready state..."
fi

if [[ $YARN_STATUS == INSTALLED ]]; then
       	startService YARN
else
       	echo "*********************************YARN Service Started..."
fi

sleep 1
#Start ZooKeeper
ZOOKEEPER_STATUS=$(getServiceStatus ZOOKEEPER)
echo "*********************************Checking KAFKA status..."
if ! [[ $ZOOKEEPER_STATUS == STARTED || $ZOOKEEPER_STATUS == INSTALLED ]]; then
       	echo "*********************************ZOOKEEPER is in a transitional state, waiting..."
       	waitForService ZOOKEEPER
       	echo "*********************************ZOOKEEPER has entered a ready state..."
fi

if [[ $ZOOKEEPER_STATUS == INSTALLED ]]; then
       	startService ZOOKEEPER
else
       	echo "*********************************ZOOKEEPER Service Started..."
fi

sleep 1
#Start MAPREDUCE2
MAPREDUCE2_STATUS=$(getServiceStatus MAPREDUCE2)
echo "*********************************Checking HIVE status..."
if ! [[ $MAPREDUCE2_STATUS == STARTED || $MAPREDUCE2_STATUS == INSTALLED ]]; then
       	echo "*********************************MAPREDUCE2 is in a transitional state, waiting..."
       	waitForService MAPREDUCE2
       	echo "*********************************MAPREDUCE2 has entered a ready state..."
fi

if [[ $MAPREDUCE2_STATUS == INSTALLED ]]; then
       	startService MAPREDUCE2
else
       	echo "*********************************MAPREDUCE2 Service Started..."
fi

sleep 1
#Start Hive
HIVE_STATUS=$(getServiceStatus HIVE)
echo "*********************************Checking HIVE status..."
if ! [[ $HIVE_STATUS == STARTED || $HIVE_STATUS == INSTALLED ]]; then
       	echo "*********************************HIVE is in a transitional state, waiting..."
       	waitForService HIVE
       	echo "*********************************HIVE has entered a ready state..."
fi

if [[ $HIVE_STATUS == INSTALLED ]]; then
       	startService HIVE
else
       	echo "*********************************HIVE Service Started..."
fi

sleep 1
#Start Kafka
KAFKA_STATUS=$(getServiceStatus KAFKA)
echo "*********************************Checking KAFKA status..."
if ! [[ $KAFKA_STATUS == STARTED || $KAFKA_STATUS == INSTALLED ]]; then
       	echo "*********************************KAFKA is in a transitional state, waiting..."
       	waitForService KAFKA
       	echo "*********************************KAFKA has entered a ready state..."
fi

if [[ $KAFKA_STATUS == INSTALLED ]]; then
       	startService KAFKA
else
       	echo "*********************************KAFKA Service Started..."
fi

sleep 1
#Start Nifi
NIFI_STATUS=$(getServiceStatus NIFI)
echo "*********************************Checking NIFI status..."
if ! [[ $NIFI_STATUS == STARTED || $NIFI_STATUS == INSTALLED ]]; then
       	echo "*********************************NIFI is in a transitional state, waiting..."
       	waitForService NIFI
       	echo "*********************************NIFI has entered a ready state..."
fi

if [[ $NIFI_STATUS == INSTALLED ]]; then
       	startService NIFI
else
       	echo "*********************************NIFI Service Started..."
fi

sleep 1
#Start HBase
HBASE_STATUS=$(getServiceStatus HBASE)
echo "*********************************Checking HBASE status..."
if ! [[ $HBASE_STATUS == STARTED || $HBASE_STATUS == INSTALLED ]]; then
       	echo "*********************************HBASE is in a transitional state, waiting..."
       	waitForService HBASE
       	echo "*********************************HBASE has entered a ready state..."
fi

if [[ $HBASE_STATUS == INSTALLED ]]; then
       	startService HBASE
else
       	echo "*********************************HBASE Service Started..."
fi

sleep 1
# Start AMBARI_INFRA
AMBARI_INFRA_STATUS=$(getServiceStatus AMBARI_INFRA)
echo "*********************************Checking AMBARI_INFRA status..."
if ! [[ $AMBARI_INFRA_STATUS == STARTED || $AMBARI_INFRA_STATUS == INSTALLED ]]; then
       	echo "*********************************AMBARI_INFRA is in a transitional state, waiting..."
       	waitForService AMBARI_INFRA
       	echo "*********************************AMBARI_INFRA has entered a ready state..."
fi

if [[ $AMBARI_INFRA_STATUS == INSTALLED ]]; then
       	startService AMBARI_INFRA
else
       	echo "*********************************AMBARI_INFRA Service Started..."
fi

sleep 1
# Start Atlas
ATLAS_STATUS=$(getServiceStatus ATLAS)
echo "*********************************Checking ATLAS status..."
if ! [[ $ATLAS_STATUS == STARTED || $ATLAS_STATUS == INSTALLED ]]; then
       	echo "*********************************ATLAS is in a transitional state, waiting..."
       	waitForService ATLAS
       	echo "*********************************ATLAS has entered a ready state..."
fi

if [[ $ATLAS_STATUS == INSTALLED ]]; then
       	startService ATLAS
else
       	echo "*********************************ATLAS Service Started..."
fi

sleep 1
# Start Storm
STORM_STATUS=$(getServiceStatus STORM)
echo "*********************************Checking STORM status..."
if ! [[ $STORM_STATUS == STARTED || $STORM_STATUS == INSTALLED ]]; then
       	echo "*********************************STORM is in a transitional state, waiting..."
       	waitForService STORM
       	echo "*********************************STORM has entered a ready state..."
fi

if [[ $STORM_STATUS == INSTALLED ]]; then
       	startService STORM
else
       	echo "*********************************STORM Service Started..."
fi

# Deploy Storm Topology
echo "*********************************Deploying Storm Topology..."
storm jar /home/storm/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.financial.topology.CreditCardTransactionMonitorTopology

echo "*********************************Deploying Application Container to YARN..."
# Clear Slider working directory
sudo -u hdfs hadoop fs -rm -R /user/root/.slider/cluster
# Ensure docker service is running
service docker start
# Start UI servlet on Yarn using Slider
slider create transactionmonitorui --template /home/docker/dockerbuild/transactionmonitorui/appConfig.json --metainfo /home/docker/dockerbuild/transactionmonitorui/metainfo.json --resources /home/docker/dockerbuild/transactionmonitorui/resources.json

echo "*********************************Wait 30 seconds for Application to Initialize..."
sleep 30