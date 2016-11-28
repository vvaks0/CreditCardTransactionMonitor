#!/bin/bash

export AMBARI_HOST=$(hostname -f)
echo "*********************************AMABRI HOST IS: $AMBARI_HOST"

export CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "(.+)'|grep -Po '[a-zA-Z0-9!$\-]+')

if [[ -z $CLUSTER_NAME ]]; then
        echo "Could not connect to Ambari Server. Please run the install script on the same host where Ambari Server is installed."
        exit 1
else
       	echo "*********************************CLUSTER NAME IS: $CLUSTER_NAME"
fi

git clone https://github.com/vakshorton/Utils
cd Utils
export ROOT_PATH=$(pwd)
echo "*********************************ROOT PATH IS: $ROOT_PATH"

getServiceStatus () {
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')

       	echo $SERVICE_STATUS
}

stopService () {
       	SERVICE=$1
       	SERVICE_STATUS=$(getServiceStatus $SERVICE)
       	echo "*********************************Stopping Service $SERVICE ..."
       	if [ "$SERVICE_STATUS" == STARTED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Stop $SERVICE"}, "ServiceInfo": {"state": "INSTALLED"}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

        echo "*********************************Stop $SERVICE TaskID $TASKID"
        sleep 2
        LOOPESCAPE="false"
        until [ "$LOOPESCAPE" == true ]; do
            TASKSTATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
            if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************Stop $SERVICE Task Status $TASKSTATUS"
            sleep 2
        done
        echo "*********************************$SERVICE Service Stopped..."
       	elif [ "$SERVICE_STATUS" == INSTALLED ]; then
       	echo "*********************************$SERVICE Service Stopped..."
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
       	elif [ "$SERVICE_STATUS" == STARTED ]; then
       	echo "*********************************$SERVICE Service Started..."
       	fi
}

recreateTransactionHistoryTable () {
	HIVESERVER_HOST=$(getHiveServerHost)
	HQL="DROP TABLE TransactionHistory;"
	# CREATE Customer Transaction History Table
	beeline -u jdbc:hive2://$HIVESERVER_HOST:10000/default -d org.apache.hive.jdbc.HiveDriver -e "$HQL"
	
	HQL="CREATE TABLE IF NOT EXISTS TransactionHistory ( accountNumber String,
                                                    fraudulent String,
                                                    merchantId String,
                                                    merchantType String,
                                                    amount Int,
                                                    currency String,
                                                    isCardPresent String,
                                                    latitude Double,
                                                    longitude Double,
                                                    transactionId String,
                                                    transactionTimeStamp String,
                                                    distanceFromHome Double,                                                                          
                                                    distanceFromPrev Double)
	COMMENT 'Customer Credit Card Transaction History'
	PARTITIONED BY (accountType String)
	CLUSTERED BY (merchantType) INTO 30 BUCKETS
	STORED AS ORC;"
	
	# CREATE Customer Transaction History Table
	beeline -u jdbc:hive2://$HIVESERVER_HOST:10000/default -d org.apache.hive.jdbc.HiveDriver -e "$HQL"
}

retargetNifiFlowReporter() {
	sleep 1
	echo "*********************************Getting Nifi Reporting Task Id..."
	REPORTING_TASK_ID=$(curl -H "Content-Type: application/json" -X GET http://$AMBARI_HOST:9090/nifi-api/flow/reporting-tasks| grep -Po '("component":{"id":"[0-9a-zA-z\-]+","name":"AtlasFlowReportingTask)'| grep -Po 'id":"([0-9a-zA-z\-]+)'| grep -Po ':"([0-9a-zA-z\-]+)'| grep -Po '([0-9a-zA-z\-]+)')

	echo "*********************************Getting Nifi Reporting Task Revision..."
	REPORTING_TASK_REVISION=$(curl -X GET http://$AMBARI_HOST:9090/nifi-api/reporting-tasks/$REPORTING_TASK_ID |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')

	echo "*********************************Stopping Nifi Reporting Task..."
	PAYLOAD=$(echo "{\"id\":\"$REPORTING_TASK_ID\",\"revision\":{\"version\":$REPORTING_TASK_REVISION},\"component\":{\"id\":\"$REPORTING_TASK_ID\",\"state\":\"STOPPED\"}}")

	curl -d "$PAYLOAD" -H "Content-Type: application/json" -X PUT http://$AMBARI_HOST:9090/nifi-api/reporting-tasks/$REPORTING_TASK_ID

	echo "*********************************Getting Nifi Reporting Task Revision..."
	REPORTING_TASK_REVISION=$(curl -X GET http://$AMBARI_HOST:9090/nifi-api/reporting-tasks/$REPORTING_TASK_ID |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')

	echo "*********************************Removing Nifi Reporting Task..."
	curl -X DELETE http://$AMBARI_HOST:9090/nifi-api/reporting-tasks/$REPORTING_TASK_ID?version=$REPORTING_TASK_REVISION

	echo "*********************************Instantiating Reporting Task..."
	PAYLOAD=$(echo "{\"revision\":{\"version\":0},\"component\":{\"name\":\"AtlasFlowReportingTask\",\"type\":\"org.apache.nifi.atlas.reporting.AtlasFlowReportingTask\",\"properties\":{\"Atlas URL\":\"http://$DATAPLANE_ATLAS_HOST:$ATLAS_PORT\",\"Nifi URL\":\"http://$AMBARI_HOST:9090\"}}}")

	REPORTING_TASK_ID=$(curl -d "$PAYLOAD" -H "Content-Type: application/json" -X POST http://$AMBARI_HOST:9090/nifi-api/controller/reporting-tasks|grep -Po '("component":{"id":")([0-9a-zA-z\-]+)'| grep -Po '(:"[0-9a-zA-z\-]+)'| grep -Po '([0-9a-zA-z\-]+)')

	echo "*********************************Starting Reporting Task..."
PAYLOAD=$(echo "{\"id\":\"$REPORTING_TASK_ID\",\"revision\":{\"version\":1},\"component\":{\"id\":\"$REPORTING_TASK_ID\",\"state\":\"RUNNING\"}}")

	curl -d "$PAYLOAD" -H "Content-Type: application/json" -X PUT http://$AMBARI_HOST:9090/nifi-api/reporting-tasks/$REPORTING_TASK_ID
	sleep 1
}

waitForAmbari () {
       	# Wait for Ambari
       	LOOPESCAPE="false"
       	until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -I -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME | grep -Po 'OK')
        if [ "$TASKSTATUS" == OK ]; then
                LOOPESCAPE="true"
                TASKSTATUS="READY"
        else
               	AUTHSTATUS=$(curl -u admin:admin -I -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME | grep HTTP | grep -Po '( [0-9]+)'| grep -Po '([0-9]+)')
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
}

installRangerHivePlugin () {
       	echo "*********************************Creating Ranger Hive Plugin service..."
       	# Create Ranger Hive Plugin service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/RANGER_HIVE_PLUGIN

       	sleep 2
       	echo "*********************************Adding Ranger Hive Plugin component..."
       	# Add Ranger Hive Plugin component to service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/RANGER_HIVE_PLUGIN/components/RANGER_HIVE_PLUGIN

       	sleep 2
       	echo "*********************************Adding Ranger Hive Plugin role to Host..."
       	# Add NIFI Master role to Sandbox host
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/hosts/$HIVESERVER_HOST/host_components/RANGER_HIVE_PLUGIN

       	sleep 30
       	echo "*********************************Installing Ranger Hive Plugin Service"
       	# Install NIFI Service
       	TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Ranger Hive Plugin"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/RANGER_HIVE_PLUGIN | grep "id" | grep -Po '([0-9]+)')
       	
       	if [ -z $TASKID ]; then
       		until ! [ -z $TASKID ]; do
       			TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Ranger Hive Plugin"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}')
       		 	echo "*********************************AMBARI TaskID " $TASKID
       		done
       	fi
       	
       	echo "*********************************AMBARI TaskID " $TASKID
       	sleep 2
       	LOOPESCAPE="false"
       	until [ "$LOOPESCAPE" == true ]; do
               	TASKSTATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
               	if [ "$TASKSTATUS" == COMPLETED ]; then
                       	LOOPESCAPE="true"
               	fi
               	echo "*********************************Task Status" $TASKSTATUS
               	sleep 2
       	done
}

getNameNodeHost () {
       	NAMENODE_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/HDFS/components/NAMENODE|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $NAMENODE_HOST
}

getHiveServerHost () {
        HIVESERVER_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/HIVE/components/HIVE_SERVER|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

        echo $HIVESERVER_HOST
}

getHiveMetaStoreHost () {
        HIVE_METASTORE_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/HIVE/components/HIVE_METASTORE|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

        echo $HIVE_METASTORE_HOST
}

getKafkaBroker () {
       	KAFKA_BROKER=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/KAFKA/components/KAFKA_BROKER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $KAFKA_BROKER
}

getAtlasHost () {
       	ATLAS_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/ATLAS/components/ATLAS_SERVER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $ATLAS_HOST
}

getRangerHost () {
       	RANGER_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/RANGER/components/RANGER_ADMIN |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $RANGER_HOST
}

#Need to recreate the Environment Variables since shell may have changed and BashRC script may not have loaded
export JAVA_HOME=/usr/jdk64
NAMENODE_HOST=$(getNameNodeHost)
export NAMENODE_HOST=$NAMENODE_HOST
HIVESERVER_HOST=$(getHiveServerHost)
export HIVESERVER_HOST=$HIVESERVER_HOST
HIVE_METASTORE_HOST=$(getHiveMetaStoreHost)
export HIVE_METASTORE_HOST=$HIVE_METASTORE_HOST
HIVE_METASTORE_URI=thrift://$HIVE_METASTORE_HOST:9083
export HIVE_METASTORE_URI=$HIVE_METASTORE_URI
ZK_HOST=$AMBARI_HOST
export ZK_HOST=$ZK_HOST
KAFKA_BROKER=$(getKafkaBroker)
export KAFKA_BROKER=$KAFKA_BROKER
ATLAS_HOST=$(getAtlasHost)
export ATLAS_HOST=$ATLAS_HOST
COMETD_HOST=$AMBARI_HOST
export COMETD_HOST=$COMETD_HOST
env

echo "HOSTNAME of the Data Plane AMBARI SERVER: "
read DATAPLANE_AMBARI_HOST

export AMBARI_HOST=$DATAPLANE_AMBARI_HOST

echo "*********************************DATAPLANE AMABRI HOST IS: $AMBARI_HOST"
export CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "(.+)'|grep -Po '[a-zA-Z0-9!$\-]+')

export DATAPLANE_ZK_HOST=$AMBARI_HOST
DATAPLANE_ATLAS_HOST=$(getAtlasHost)
DATAPLANE_KAFKA_BROKER=$(getKafkaBroker)
DATAPLANE_HIVE_METASTORE_HOST=$(getHiveMetaStoreHost)
DATAPLANE_HIVESERVER_HOST=$(getHiveServerHost)
DATAPLANE_RANGER_HOST=$(getRangerHost)
DATAPLANE_NAMENODE_HOST=$(getNameNodeHost)
env

export ZK_PORT=2181
export ATLAS_PORT=21000
export KAFKA_PORT=6667
export HIVE_METASTORE_PORT=9083
export RANGER_ADMIN_PORT=6080
export RANGER_HIVE_REPO=data-plane_hive

export DATAPLANE_HIVE_METASTORE_URI=thrift://$DATAPLANE_HIVE_METASTORE_HOST:$HIVE_METASTORE_PORT

echo "export ATLAS_HOST=$DATAPLANE_ATLAS_HOST" >> /etc/bashrc
echo "export ATLAS_HOST=$DATAPLANE_ATLAS_HOST" >> ~/.bash_profile
echo "export RANGER_HOST=$DATAPLANE_RANGER_HOST" >> ~/.bash_profile
echo "export DATAPLANE_NAMENODE_HOST=$DATAPLANE_NAMENODE_HOST" >> ~/.bash_profile
echo "export HIVE_METASTORE_HOST=$DATAPLANE_HIVE_METASTORE_HOST" >> /etc/bashrc
echo "export HIVE_METASTORE_HOST=$DATAPLANE_HIVE_METASTORE_HOST" >> ~/.bash_profile
echo "export HIVE_METASTORE_JDO_HOST=$DATAPLANE_HIVESERVER_HOST" >> /etc/bashrc
echo "export HIVE_METASTORE_JDO_HOST=$DATAPLANE_HIVESERVER_HOST" >> ~/.bash_profile
echo "export HIVE_METASTORE_URI=$DATAPLANE_HIVE_METASTORE_URI" >> ~/.bash_profile

. ~/.bash_profile

echo "********************************DATAPLANE ATLAS ENDPOINT: $DATAPLANE_ATLAS_HOST:$ATLAS_PORT"
echo "********************************DATAPLANE KAFKA ENDPOINT: $DATAPLANE_KAFKA_BROKER:$KAFKA_PORT"
echo "********************************DATAPLANE ZOOKEEPER ENDPOINT: $DATAPLANE_ZK_HOST:$ZK_PORT"
echo "********************************DATAPLANE RANGER ENDPOINT: $DATAPLANE_RANGER_HOST:$RANGER_PORT"
echo "********************************DATAPLANE NAMENODE: $DATAPLANE_NAMENODE_HOST"

export AMBARI_HOST=$(hostname -f)
echo "*********************************AMABRI HOST IS: $AMBARI_HOST"
export CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "(.+)'|grep -Po '[a-zA-Z0-9!$\-]+')

echo "*********************************Setting Hive Ranger Plugin Configuration..."
sed -r -i "s;\{\{ZK_HOST\}\};$DATAPLANE_ZK_HOST;" $ROOT_PATH/hive-ranger-config/ranger-hive-audit
sed -r -i "s;\{\{NAMENODE_HOST\}\};$DATAPLANE_NAMENODE_HOST;" $ROOT_PATH/hive-ranger-config/ranger-hive-audit

sed -r -i "s;\{\{ZK_HOST\}\};$DATAPLANE_ZK_HOST;" $ROOT_PATH/RANGER_HIVE_PLUGIN/package/configuration/ranger-hive-audit.xml
sed -r -i "s;\{\{NAMENODE_HOST\}\};$DATAPLANE_NAMENODE_HOST;" $ROOT_PATH/RANGER_HIVE_PLUGIN/package/configuration/ranger-hive-audit.xml

sed -r -i "s;\{\{RANGER_URL\}\};http://$DATAPLANE_RANGER_HOST:$RANGER_ADMIN_PORT;" $ROOT_PATH/hive-ranger-config/ranger-hive-security
sed -r -i "s;\{\{REPO_NAME\}\};$RANGER_HIVE_REPO;" $ROOT_PATH/hive-ranger-config/ranger-hive-security

sed -r -i "s;\{\{RANGER_URL\}\};http://$DATAPLANE_RANGER_HOST:$RANGER_ADMIN_PORT;" $ROOT_PATH/RANGER_HIVE_PLUGIN/package/configuration/ranger-hive-security.xml
sed -r -i "s;\{\{REPO_NAME\}\};$RANGER_HIVE_REPO;" $ROOT_PATH/RANGER_HIVE_PLUGIN/package/configuration/ranger-hive-security.xml
		
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site hive.security.authorization.enabled true
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site hive.conf.restricted.list 	hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hiveserver2-site hive.security.authorization.enabled true
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hiveserver2-site hive.security.authorization.manager org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hiveserver2-site hive.security.authenticator.manager org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator 
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME ranger-hive-audit $ROOT_PATH/hive-ranger-config/ranger-hive-audit
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME ranger-hive-plugin-properties $ROOT_PATH/hive-ranger-config/ranger-hive-plugin-properties
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME  ranger-hive-policymgr-ssl $ROOT_PATH/hive-ranger-config/ranger-hive-policymgr-ssl
		sleep 2
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME ranger-hive-security $ROOT_PATH/hive-ranger-config/ranger-hive-security

echo "*********************************Loading and Installing Ranger Hive Plugin..."
cp -Rvf $ROOT_PATH/RANGER_HIVE_PLUGIN /var/lib/ambari-server/resources/stacks/HDP/2.5/services/

ambari-server restart
waitForAmbari
installRangerHivePlugin

echo "*********************************Setting Hive Atlas Client Configuration..."
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "atlas.rest.address" "$DATAPLANE_ATLAS_HOST:$ATLAS_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-atlas-application.properties "atlas.kafka.bootstrap.servers" "$DATAPLANE_KAFKA_BROKER:$KAFKA_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-atlas-application.properties "atlas.kafka.zookeeper.connect" "$DATAPLANE_ZK_HOST:$ZK_PORT"

echo "*********************************Setting Storm Atlas Client Configuration..."
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME storm-atlas-application.properties "atlas.rest.address" "$DATAPLANE_ATLAS_HOST:$ATLAS_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME storm-atlas-application.properties "atlas.kafka.zookeeper.connect" "$DATAPLANE_ZK_HOST:$ZK_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME storm-atlas-application.properties "atlas.kafka.bootstrap.servers" "$DATAPLANE_KAFKA_BROKER:$KAFKA_PORT"

echo "*********************************Setting Sqoop Atlas Client Configuration..."
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME sqoop-atlas-application.properties "atlas.rest.address" "$DATAPLANE_ATLAS_HOST:$ATLAS_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME sqoop-atlas-application.properties "atlas.kafka.zookeeper.connect" "$DATAPLANE_ZK_HOST:$ZK_PORT"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME sqoop-atlas-application.properties "atlas.kafka.bootstrap.servers" "$DATAPLANE_KAFKA_BROKER:$KAFKA_PORT"

echo "*********************************Setting Hive Meta Store Configuration..."
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "javax.jdo.option.ConnectionURL" "jdbc:mysql://$DATAPLANE_HIVESERVER_HOST/hive?createDatabaseIfNotExist=true"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "javax.jdo.option.ConnectionPassword" "hive"
/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.metastore.uris" "thrift://$DATAPLANE_HIVE_METASTORE_HOST:$HIVE_METASTORE_PORT"

echo "*********************************Restarting Services to refresh configurations..."
stopService HIVE
sleep 1
stopService STORM
sleep 1
stopService SQOOP
sleep 1
 
startService HIVE
sleep 1
startService STORM
sleep 1
startService SQOOP

cd $ROOT_PATH/DataPlaneUtils
mvn clean package
java -jar target/DataPlaneUtils-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# Recreate TransactionHistory table to reset Atlas qualified name to this cluster
echo "*********************************Recreating TransactionHistory Table..."
recreateTransactionHistoryTable

# Redeploy Storm Topology to send topology meta data to Atlas
echo "*********************************Redeploying Storm Topology..."
storm kill CreditCardTransactionMonitor
storm jar /home/storm/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.financial.topology.CreditCardTransactionMonitorTopology

# Start Nifi Flow Reporter to send flow meta data to Atlas
echo "*********************************Retargeting Nifi Flow Reporting Task..."
sleep 5
retargetNifiFlowReporter

exit 0