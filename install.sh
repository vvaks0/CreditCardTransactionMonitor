#!/bin/bash

if [ ! -d "/usr/jdk64" ]; then
	echo "*********************************Install and Enable Oracle JDK 8"
	wget http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-8u77-linux-x64.tar.gz
	tar -vxzf jdk-8u77-linux-x64.tar.gz -C /usr
	mv /usr/jdk1.8.0_77 /usr/jdk64
	alternatives --install /usr/bin/java java /usr/jdk64/bin/java 3
	alternatives --install /usr/bin/javac javac /usr/jdk64/bin/javac 3
	alternatives --install /usr/bin/jar jar /usr/jdk64/bin/jar 3
	export JAVA_HOME=/usr/jdk64
	echo "export JAVA_HOME=/usr/jdk64" >> /etc/bashrc
fi

if [[ -d "/usr/hdp/current/atlas-server"  && ! -d "/usr/hdp/current/atlas-client" ]]; then 
echo "*********************************Only Atlas Server installed, setting symbolic link"
	ln -s /usr/hdp/current/atlas-server /usr/hdp/current/atlas-client
	ln -s /usr/hdp/current/atlas-server/conf/application.properties /usr/hdp/current/atlas-client/conf/atlas-application.properties
fi

export AMBARI_HOST=$(hostname -f)
echo "*********************************AMABRI HOST IS: $AMBARI_HOST"

export CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "(.+)'|grep -Po '[a-zA-Z0-9\-_!?.]+')

if [[ -z $CLUSTER_NAME ]]; then
        echo "Could not connect to Ambari Server. Please run the install script on the same host where Ambari Server is installed."
        exit 1
else
       	echo "*********************************CLUSTER NAME IS: $CLUSTER_NAME"
fi

export ROOT_PATH=$(pwd)
echo "*********************************ROOT PATH IS: $ROOT_PATH"

export VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
export INTVERSION=$(echo $VERSION*10 | bc | grep -Po '([0-9][0-9])')
echo "*********************************HDP VERSION IS: $VERSION"

export HADOOP_USER_NAME=hdfs
echo "*********************************HADOOP_USER_NAME set to HDFS"

serviceExists () {
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"status" : ' | grep -Po '([0-9]+)')

       	if [ "$SERVICE_STATUS" == 404 ]; then
       		echo 0
       	else
       		echo 1
       	fi
}

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

stopService () {
       	SERVICE=$1
       	SERVICE_STATUS=$(getServiceStatus $SERVICE)
       	echo "*********************************Stopping Service $SERVICE ..."
       	if [ "$SERVICE_STATUS" == STARTED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Stop $SERVICE"}, "ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

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
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context": "Start $SERVICE" }, "ServiceInfo": {"maintenance_state" : "OFF", "state": "STARTED"}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

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

getLatestNifiBits () {
       	if [ "$INTVERSION" -gt 24 ]; then
       		echo "*********************************Removing Current Version of NIFI..."
       		rm -rf /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI

       	echo "*********************************Downloading Newest Version of NIFI..."
       	git clone https://github.com/abajwa-hw/ambari-nifi-service.git  /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI
       	fi
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

installNifiService () {
       	echo "*********************************Creating NIFI service..."
       	# Create NIFI service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI

       	sleep 2
       	echo "*********************************Adding NIFI MASTER component..."
       	# Add NIFI Master component to service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI/components/NIFI_MASTER

       	sleep 2
       	echo "*********************************Creating NIFI configuration..."

       	# Create and apply configuration
       	#sleep 2
		if [ "$INTVERSION" -gt 24 ]; then
			/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-env $ROOT_PATH/Nifi/config/nifi-env.json
		else	
			/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-logback-env $ROOT_PATH/Nifi/config/nifi-logback-env.json
		fi
		sleep 2
       	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-ambari-config $ROOT_PATH/Nifi/config/nifi-ambari-config.json
       	sleep 2
       	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-bootstrap-env $ROOT_PATH/Nifi/config/nifi-bootstrap-env.json
       	sleep 2
       	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-flow-env $ROOT_PATH/Nifi/config/nifi-flow-env.json
       	sleep 2
       	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME nifi-properties-env $ROOT_PATH/Nifi/config/nifi-properties-env.json
       	sleep 2
       	echo "*********************************Adding NIFI MASTER role to Host..."
       	# Add NIFI Master role to Sandbox host
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/hosts/$AMBARI_HOST/host_components/NIFI_MASTER

       	sleep 30
       	echo "*********************************Installing NIFI Service"
       	# Install NIFI Service
       	TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Nifi"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI | grep "id" | grep -Po '([0-9]+)')
		
		sleep 2       	
       	if [ -z $TASKID ]; then
       		until ! [ -z $TASKID ]; do
       			TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Nifi"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI | grep "id" | grep -Po '([0-9]+)')
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

waitForNifiServlet () {
       	LOOPESCAPE="false"
       	until [ "$LOOPESCAPE" == true ]; do
       		TASKSTATUS=$(curl -u admin:admin -i -X GET http://$AMBARI_HOST:9090/nifi-api/controller | grep -Po 'OK')
       		if [ "$TASKSTATUS" == OK ]; then
               		LOOPESCAPE="true"
       		else
               		TASKSTATUS="PENDING"
       		fi
       		echo "*********************************Waiting for NIFI Servlet..."
       		echo "*********************************NIFI Servlet Status... " $TASKSTATUS
       		sleep 2
       	done
}


deployTemplateToNifi () {
	echo "*********************************Importing NIFI Template..."		
	if [ "$INTVERSION" -gt 24 ]; then
       	# Import NIFI Template HDF 2.x
       	TEMPLATEID=$(curl -v -F template=@"$ROOT_PATH/Nifi/template/CreditFraudDetectionFlow.xml" -X POST http://$AMBARI_HOST:9090/nifi-api/process-groups/root/templates/upload | grep -Po '<id>([a-z0-9-]+)' | grep -Po '>([a-z0-9-]+)' | grep -Po '([a-z0-9-]+)')
		sleep 1
		
		# Instantiate NIFI Template 2.x
		echo "*********************************Instantiating NIFI Flow..."
       	curl -u admin:admin -i -H "Content-Type:application/json" -d "{\"templateId\":\"$TEMPLATEID\",\"originX\":100,\"originY\":100}" -X POST http://$AMBARI_HOST:9090/nifi-api/process-groups/root/template-instance
       	sleep 1
       	
       	# Rename NIFI Root Group HDF 2.x
		echo "*********************************Renaming Nifi Root Group..."
		ROOT_GROUP_REVISION=$(curl -X GET http://$AMBARI_HOST:9090/nifi-api/process-groups/root |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')
		
		sleep 1
		ROOT_GROUP_ID=$(curl -X GET http://$AMBARI_HOST:9090/nifi-api/process-groups/root|grep -Po '("component":{"id":")([0-9a-zA-z\-]+)'| grep -Po '(:"[0-9a-zA-z\-]+)'| grep -Po '([0-9a-zA-z\-]+)')

		PAYLOAD=$(echo "{\"id\":\"$ROOT_GROUP_ID\",\"revision\":{\"version\":$ROOT_GROUP_REVISION},\"component\":{\"id\":\"$ROOT_GROUP_ID\",\"name\":\"Credit-Fraud-Demo\"}}")
		
		sleep 1
		curl -d $PAYLOAD  -H "Content-Type: application/json" -X PUT http://$AMBARI_HOST:9090/nifi-api/process-groups/$ROOT_GROUP_ID
	else
       	 # Import NIFI Template HDF 1.x
       	TEMPLATEID=$(curl -v -F template=@"$ROOT_PATH/Nifi/template/CreditFraudDetectionFlow.xml" -X POST http://$AMBARI_HOST:9090/nifi-api/controller/templates | grep -Po '<id>([a-z0-9-]+)' | grep -Po '>([a-z0-9-]+)' | grep -Po '([a-z0-9-]+)')
       	sleep 1		
		
		# Instantiate NIFI Template HDF 1.x
		echo "*********************************Instantiating NIFI Flow..."
		REVISION=$(curl -u admin:admin  -i -X GET http://$AMBARI_HOST:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')
		sleep 1
		
		curl -u admin:admin -i -H "Content-Type:application/x-www-form-urlencoded" -d "templateId=$TEMPLATEID&originX=100&originY=100&version=$REVISION" -X POST http://$AMBARI_HOST:9090/nifi-api/controller/process-groups/root/template-instance
	fi
	sleep 1
}

# Start NIFI Flow
startNifiFlow () {
    echo "*********************************Starting NIFI Flow..."
	if [ "$INTVERSION" -gt 24 ]; then	     
    	# Start NIFI Flow HDF 2.x
    	TARGETS=($(curl -u admin:admin -i -X GET http://$AMBARI_HOST:9090/nifi-api/process-groups/root/processors | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
       	length=${#TARGETS[@]}
       	echo $length
       	echo ${TARGETS[0]}
       	
       	for ((i = 0; i < $length; i++))
       	do
       		ID=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"id":"([a-zA-z0-9\-]+)'|grep -Po ':"([a-zA-z0-9\-]+)'|grep -Po '([a-zA-z0-9\-]+)'|head -1)
       		REVISION=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')
       		TYPE=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"type":"([a-zA-Z0-9\-.]+)' |grep -Po ':"([a-zA-Z0-9\-.]+)' |grep -Po '([a-zA-Z0-9\-.]+)' |head -1)
       		echo "Current Processor Path: ${TARGETS[i]}"
       		echo "Current Processor Revision: $REVISION"
       		echo "Current Processor ID: $ID"
       		echo "Current Processor TYPE: $TYPE"

       		if ! [ -z $(echo $TYPE|grep "PutKafka") ] || ! [ -z $(echo $TYPE|grep "PublishKafka") ]; then
       			echo "***************************This is a PutKafka Processor"
       			echo "***************************Updating Kafka Broker Porperty and Activating Processor..."
       			if ! [ -z $(echo $TYPE|grep "PutKafka") ]; then
                    PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"config\":{\"properties\":{\"Known Brokers\":\"$AMBARI_HOST:6667\"}},\"state\":\"RUNNING\"}}")
                else
                    PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"config\":{\"properties\":{\"bootstrap.servers\":\"$AMBARI_HOST:6667\"}},\"state\":\"RUNNING\"}}")
                fi
       		else
       			echo "***************************Activating Processor..."
       				PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"state\":\"RUNNING\"}}")
			fi
       		echo "$PAYLOAD"
       		
       		curl -u admin:admin -i -H "Content-Type:application/json" -d "${PAYLOAD}" -X PUT ${TARGETS[i]}
       	done
	else       	
       	# Start NIFI Flow HDF 1.x
		echo "*********************************Starting NIFI Flow..."
		REVISION=$(curl -u admin:admin  -i -X GET http://$AMBARI_HOST:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')

		TARGETS=($(curl -u admin:admin -i -X GET http://$AMBARI_HOST:9090/nifi-api/controller/process-groups/root/processors | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
length=${#TARGETS[@]}

		for ((i = 0; i != length; i++)); do
   			echo curl -u admin:admin -i -X GET ${TARGETS[i]}
   			echo "Current Revision: " $REVISION
   			curl -u admin:admin -i -H "Content-Type:application/x-www-form-urlencoded" -d "state=RUNNING&version=$REVISION" -X PUT ${TARGETS[i]}
		   REVISION=$(curl -u admin:admin  -i -X GET http://$AMBARI_HOST:9090/nifi-api/controller/revision |grep -Po '\"version\":([0-9]+)' | grep -Po '([0-9]+)')
		done
	fi
}

installDemoControl () {
		echo "*********************************Creating Demo Control service..."
       	# Create Demo Control service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/CREDIT_FRAUD_DEMO_CONTROL

       	sleep 2
       	echo "*********************************Adding Demo Control component..."
       	# Add Demo Control component to service
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/CREDIT_FRAUD_DEMO_CONTROL/components/CREDIT_FRAUD_DEMO_CONTROL

       	sleep 2
       	echo "*********************************Creating Demo Control configuration..."
		
		tee control-config <<-'EOF'
			"properties" : {
"democontrol.download_url" : "https://github.com/vakshorton/CreditCardTransactionMonitor.git",
"democontrol.install_dir" : "/root",
"democontrol.nifi_host_ip" : " "
			}
		EOF
		
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME control-config control-config
		
       	# Create and apply configuration
       	#/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME control-config "democontrol.install_dir" "/root"
       	#sleep 2
       	#/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME control-config "democontrol.download_url" "https://github.com/vakshorton/DataSimulators.git"
       	sleep 2
       	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME control-config "democontrol.nifi_host_ip" $(getent hosts $NIFI_HOST | awk '{ print $1 }')

       	sleep 2
       	echo "*********************************Adding Creating role to Host..."
       	# Add Demo Control role to CreditCardTransactionMonitorhost
       	curl -u admin:admin -H "X-Requested-By:ambari" -i -X POST http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/hosts/$AMBARI_HOST/host_components/CREDIT_FRAUD_DEMO_CONTROL

       	sleep 15
       	echo "*********************************Installing Demo Control Service"
       	# Install Demo Control Service
       	TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Demo Control"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/CREDIT_FRAUD_DEMO_CONTROL | grep "id" | grep -Po '([0-9]+)')
       	
       	if [ -z $TASKID ]; then
       		until ! [ -z $TASKID ]; do
       			TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Install Demo Control"}, "Body": {"ServiceInfo": {"maintenance_state" : "OFF", "state": "INSTALLED"}}}' http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/CREDIT_FRAUD_DEMO_CONTROL grep "id" | grep -Po '([0-9]+)')
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

enablePhoenix () {
	echo "*********************************Installing Phoenix Binaries..."
	yum install -y phoenix
	echo "*********************************Enabling Phoenix..."
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site phoenix.functions.allowUserDefinedFunctions true
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.defaults.for.version.skip true
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.regionserver.wal.codec org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.region.server.rpc.scheduler.factory.class org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.rpc.controllerfactory.class org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory
}

configureYarnMemory () {
	YARN_MEM_MAX=$(/var/lib/ambari-server/resources/scripts/configs.sh get $AMBARI_HOST $CLUSTER_NAME yarn-site | grep '"yarn.scheduler.maximum-allocation-mb"'|grep -Po ': "([0-9]+)'|grep -Po '([0-9]+)')
	echo "*********************************yarn.scheduler.maximum-allocation-mb is set to $YARN_MEM_MAX MB"
	if [[ $YARN_MEM_MAX -lt 6000 ]]; then
		echo "*********************************Changing YARN Container Memory Size..."
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME yarn-site "yarn.scheduler.maximum-allocation-mb" "6144"
		sleep 1	
		/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME yarn-site "yarn.nodemanager.resource.memory-mb" "6144"
	fi	
}

configureHiveACID () {
	echo "*********************************Configuring Hive ACID..."
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.support.concurrency" "true"
	sleep 1	
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.txn.manager" "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager"
	sleep 1	
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.exec.dynamic.partition.mode" "nonstrict"
	sleep 1	
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.enforce.bucketing" "true"
	sleep 1	
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.compactor.worker.threads" "1"
	sleep 1	
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hive-site "hive.compactor.initiator.on" "true"
}

createTransactionHistoryTable () {
	HIVESERVER_HOST=$(getHiveServerHost)
	HQL="CREATE TABLE IF NOT EXISTS transaction_history_$CLUSTER_NAME ( accountNumber String,
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
	PARTITIONED BY (accountType String)
	CLUSTERED BY (merchantType) INTO 30 BUCKETS
	STORED AS ORC
	TBLPROPERTIES (\"transactional\"=\"true\");"
	
	# CREATE Customer Transaction History Table
	beeline -u jdbc:hive2://$HIVESERVER_HOST:10000/default -d org.apache.hive.jdbc.HiveDriver -e "$HQL" -n hive
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

getNifiHost () {
       	NIFI_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI/components/NIFI_MASTER|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

       	echo $NIFI_HOST
}

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

echo "export NAMENODE_HOST=$NAMENODE_HOST" >> /etc/bashrc
echo "export ZK_HOST=$ZK_HOST" >> /etc/bashrc
echo "export KAFKA_BROKER=$KAFKA_BROKER" >> /etc/bashrc
echo "export ATLAS_HOST=$ATLAS_HOST" >> /etc/bashrc
echo "export HIVE_METASTORE_HOST=$HIVE_METASTORE_HOST" >> /etc/bashrc
echo "export HIVE_METASTORE_URI=$HIVE_METASTORE_URI" >> /etc/bashrc
echo "export COMETD_HOST=$COMETD_HOST" >> /etc/bashrc

echo "export NAMENODE_HOST=$NAMENODE_HOST" >> ~/.bash_profile
echo "export ZK_HOST=$ZK_HOST" >> ~/.bash_profile
echo "export KAFKA_BROKER=$KAFKA_BROKER" >> ~/.bash_profile
echo "export ATLAS_HOST=$ATLAS_HOST" >> ~/.bash_profile
echo "export HIVE_METASTORE_HOST=$HIVE_METASTORE_HOST" >> ~/.bash_profile
echo "export HIVE_METASTORE_URI=$HIVE_METASTORE_URI" >> ~/.bash_profile
echo "export COMETD_HOST=$COMETD_HOST" >> ~/.bash_profile

. ~/.bash_profile

# Install Maven
echo "*********************************Installing Maven..."
wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
yum install -y apache-maven

#Install, Configure, and Start Docker
echo "*********************************Installing Docker..."
echo " 				  *****************Adding Docker Yum Repo..."
tee /etc/yum.repos.d/docker.repo <<-'EOF'
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/$releasever/
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF

echo " 				  *****************Installing Docker via Yum..."
rpm -iUvh http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
yum -y install docker-io
echo " 				  *****************Configuring Docker Permissions..."
groupadd docker
gpasswd -a yarn docker
echo " 				  *****************Registering Docker to Start on Boot..."
service docker start
chkconfig --add docker
chkconfig docker on
echo " 				  *****************Create /root HDFS folder for Slider..."
hadoop fs -mkdir /user/root/
hadoop fs -chown root:hdfs /user/root/
#Create Docker working folder
echo " 				  *****************Creating Docker Home Folder..."
mkdir /home/docker/
mkdir /home/docker/dockerbuild/
mkdir /home/docker/dockerbuild/transactionmonitorui

echo "*********************************Staging Slider Configurations..."
cd $ROOT_PATH/SliderConfig
cp -vf appConfig.json /home/docker/dockerbuild/transactionmonitorui
cp -vf metainfo.json /home/docker/dockerbuild/transactionmonitorui
cp -vf resources.json /home/docker/dockerbuild/transactionmonitorui

echo "*********************************Copy redeployApplication.sh to /root"
cd $ROOT_PATH
cp -Rvf $ROOT_PATH/redeployApplication.sh /root

echo "*********************************Load Demo Control Service into Ambari"
cd $ROOT_PATH
cp -Rvf $ROOT_PATH/CREDIT_FRAUD_DEMO_CONTROL /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/

echo "*********************************Load Data Plane Client Service into Ambari"
git clone https://github.com/vakshorton/Utils
cp -Rvf $ROOT_PATH/Utils/DATA_PLANE_CLIENT /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/

# Build from source
echo "*********************************Building Credit Card Transaction Monitor Storm Topology"
cd $ROOT_PATH/CreditCardTransactionMonitor
mvn clean package
cp -vf target/CreditCardTransactionMonitor-0.0.1-SNAPSHOT.jar /home/storm

# Build from source
echo "*********************************Building Nifi Atlas Reporter"
cd $ROOT_PATH
git clone https://github.com/vakshorton/NifiAtlasBridge.git
cd $ROOT_PATH/NifiAtlasBridge/NifiAtlasFlowReportingTask
mvn clean install
cd $ROOT_PATH/NifiAtlasBridge/NifiAtlasLineageReportingTask
mvn clean install
cd $ROOT_PATH

NIFI_SERVICE_PRESENT=$(serviceExists NIFI)
if [[ "$NIFI_SERVICE_PRESENT" == 0 ]]; then
       	echo "*********************************NIFI Service Not Present, Installing..."
       	getLatestNifiBits
       	ambari-server restart
       	waitForAmbari
       	installNifiService
       	
       	mkdir /var/run/nifi
		chown nifi:nifi /var/run/nifi
		
		echo "*********************************Install Nifi Atlas Reporter..."
		NIFI_HOME=$(ls /opt/|grep nifi)
		if [ -z "$NIFI_HOME" ]; then
        	NIFI_HOME=$(ls /opt/|grep HDF)
		fi
		export NIFI_HOME
		
		mv -vf  $ROOT_PATH/NifiAtlasBridge/NifiAtlasFlowReportingTask/target/NifiAtlasFlowReportingTask-0.0.1-SNAPSHOT.nar /opt/$NIFI_HOME/lib
		
		mv -vf  $ROOT_PATH/NifiAtlasBridge/NifiAtlasLineageReportingTask/target/NifiAtlasLineageReporter-0.0.1-SNAPSHOT.nar /opt/$NIFI_HOME/lib
       	
       	startService NIFI
else
       	echo "*********************************NIFI Service Already Installed"
		echo "*********************************Install Nifi Atlas Reporter..."
		stopService NIFI
		NIFI_HOME=$(ls /opt/|grep nifi)
		if [ -z "$NIFI_HOME" ]; then
        	NIFI_HOME=$(ls /opt/|grep HDF)
		fi
		export NIFI_HOME
		cp -vf  $ROOT_PATH/NifiAtlasLineageReporter/target/NifiAtlasLineageReporter-0.0.1-SNAPSHOT.nar /opt/$NIFI_HOME/lib
		sleep 2
		startService NIFI
fi

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

echo "*********************************Install GeoLite City DB for Nifi..."
wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz -P /home/centos
cd /home/centos
gunzip -vf GeoLite2-City.mmdb.gz
chmod 755 -R /home/centos

waitForNifiServlet
echo "*********************************Deploying NIFI Template..."
deployTemplateToNifi

echo "*********************************Starting NIFI Flow ..."
startNifiFlow
cd $ROOT_PATH

echo "*********************************Installing Demo Control Service ..."
NIFI_HOST=$(getNifiHost)
export NIFI_HOST=$NIFI_HOST
echo "export NIFI_HOST=$NIFI_HOST" >> /etc/bashrc
echo "export NIFI_HOST=$NIFI_HOST" >> ~/.bash_profile
. ~/.bash_profile
installDemoControl

sleep 2
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

#Configure Kafka
echo "*********************************Creating Kafka Topics..."
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $AMBARI_HOST:2181 --replication-factor 1 --partitions 1 --topic IncomingTransactions
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $AMBARI_HOST:2181 --replication-factor 1 --partitions 1 --topic CustomerTransactionValidation

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

stopService STORM
startService STORM

# Download Docker Images
echo "*********************************Downloading Docker Images for UI..."
service docker start
docker pull vvaks/transactionmonitorui
docker pull vvaks/cometd

echo "*********************************Checking Yarn and Phoenix Configurations..."
configureYarnMemory
enablePhoenix
stopService HBASE
startService HBASE
echo "*********************************Checking Hive Configurations..."
echo " 				  *****************Set Hive Scratch Folder..."
mkdir /tmp/hive
chmod 777 /tmp
chmod 777 /tmp/hive
hadoop fs -mkdir /tmp/hive/
hadoop fs -chmod 777 /tmp/
hadoop fs -chmod 777 /tmp/hive/
hadoop fs -chown hive:hdfs /tmp/hive/
configureHiveACID
stopService HIVE
startService HIVE
#Create Transaction History Hive Table for Storm topology
echo "*********************************Creating TransactionHistory Hive Table..."
createTransactionHistoryTable

echo "*********************************Setting Ambari-Server to Start on Boot..."
chkconfig --add ambari-server
chkconfig ambari-server on
echo "*********************************Installation Complete... "

exit 0
# Reboot to refresh configuration
#reboot now