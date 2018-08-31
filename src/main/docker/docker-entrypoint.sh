#!/bin/bash

APP_HOME=$(pwd);
RESOURCES_HOME=${APP_HOME}/resources/;

export CHEF_CONFIG_REPO=${CHEF_CONFIG_REPO:-aai-config};
export CHEF_GIT_URL=${CHEF_GIT_URL:-http://gerrit.onap.org/r/aai};
export CHEF_CONFIG_GIT_URL=${CHEF_CONFIG_GIT_URL:-$CHEF_GIT_URL};
export CHEF_DATA_GIT_URL=${CHEF_DATA_GIT_URL:-$CHEF_GIT_URL};

export SERVER_PORT=${SERVER_PORT:-8444};

USER_ID=${LOCAL_USER_ID:-9001}
GROUP_ID=${LOCAL_GROUP_ID:-9001}

# need to override this in docker compose template to use proxy
PROXY_HOST=${LOCAL_PROXY_HOST:-}

echo "APPHOME is $APP_HOME";

if [ $(cat /etc/passwd | grep aaiadmin | wc -l) -eq 0 ]; then
	groupadd aaiadmin -g ${GROUP_ID} || {
		echo "Unable to create the group id for ${GROUP_ID}";
		exit 1;
	}
	useradd --shell=/bin/bash -u ${USER_ID} -g ${GROUP_ID} -o -c "" -m aaiadmin || {
		echo "Unable to create the user id for ${USER_ID}";
		exit 1;
	}
fi;

chown -R aaiadmin:aaiadmin /opt/app /opt/aai/logroot /var/chef
find /opt/app/ -name "*.sh" -exec chmod +x {} +

gosu aaiadmin ln -s bin scripts
gosu aaiadmin ln -s /opt/aai/logroot/AAI-CACHER logs
mkdir -p /opt/app/aai-cacher/logs/gc
chown -R aaiadmin:aaiadmin /opt/app/aai-cacher/logs

if [ -f ${APP_HOME}/aai.sh ]; then
    mv ${APP_HOME}/aai.sh /etc/profile.d/aai.sh
    chmod 755 /etc/profile.d/aai.sh

    scriptName=$1;

    if [ ! -z $scriptName ]; then

        if [ -f ${APP_HOME}/bin/${scriptName} ]; then
            shift 1;
            gosu aaiadmin ${APP_HOME}/bin/${scriptName} "$@" || {
                echo "Failed to run the ${scriptName}";
                exit 1;
            }
        else
            echo "Unable to find the script ${scriptName} in ${APP_HOME}/bin";
            exit 1;
        fi;

        exit 0;
    fi;
fi;

if [ -f ${APP_HOME}/scripts/install/cacher-swm-vars.sh ]; then
    source ${APP_HOME}/scripts/install/cacher-swm-vars.sh;
fi;

MIN_HEAP_SIZE=${MIN_HEAP_SIZE:-512m};
MAX_HEAP_SIZE=${MAX_HEAP_SIZE:-1024m};
MAX_PERM_SIZE=${MAX_PERM_SIZE:-512m};
PERM_SIZE=${PERM_SIZE:-512m};

JAVA_CMD="exec gosu aaiadmin java";

JVM_OPTS="${PRE_JVM_ARGS} -Xloggc:/opt/app/aai-cacher/logs/gc/aai_gc.log";
JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=/opt/app/aai-cacher/logs/ajsc-jetty/heap-dump";
JVM_OPTS="${JVM_OPTS} -Xms${MIN_HEAP_SIZE}";
JVM_OPTS="${JVM_OPTS} -Xmx${MAX_HEAP_SIZE}";

JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails";
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCTimeStamps";
JVM_OPTS="${JVM_OPTS} -XX:MaxPermSize=${MAX_PERM_SIZE}";
JVM_OPTS="${JVM_OPTS} -XX:PermSize=${PERM_SIZE}";

JVM_OPTS="${JVM_OPTS} -server";
JVM_OPTS="${JVM_OPTS} -XX:NewSize=512m";
JVM_OPTS="${JVM_OPTS} -XX:MaxNewSize=512m";
JVM_OPTS="${JVM_OPTS} -XX:SurvivorRatio=8";
JVM_OPTS="${JVM_OPTS} -XX:+DisableExplicitGC";
JVM_OPTS="${JVM_OPTS} -verbose:gc";
JVM_OPTS="${JVM_OPTS} -XX:+UseParNewGC";
JVM_OPTS="${JVM_OPTS} -XX:+CMSParallelRemarkEnabled";
JVM_OPTS="${JVM_OPTS} -XX:+CMSClassUnloadingEnabled";
JVM_OPTS="${JVM_OPTS} -XX:+UseConcMarkSweepGC";
JVM_OPTS="${JVM_OPTS} -XX:-UseBiasedLocking";
JVM_OPTS="${JVM_OPTS} -XX:ParallelGCThreads=4";
JVM_OPTS="${JVM_OPTS} -XX:LargePageSizeInBytes=128m";
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails";
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCTimeStamps";
JVM_OPTS="${JVM_OPTS} -Dsun.net.inetaddr.ttl=180";
JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError";
JVM_OPTS="${JVM_OPTS} ${POST_JVM_ARGS}";
JVM_OPTS="${JVM_OPTS} -Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true";

JAVA_OPTS="${JAVA_OPTS} -DAJSC_HOME=$APP_HOME";
if [ -f ${INTROSCOPE_LIB}/Agent.jar ] && [ -f ${INTROSCOPE_AGENTPROFILE} ]; then
        JAVA_OPTS="${JAVA_OPTS} -javaagent:${INTROSCOPE_LIB}/Agent.jar -noverify -Dcom.wily.introscope.agentProfile=${INTROSCOPE_AGENTPROFILE} -Dintroscope.agent.agentName=cacher"
fi
JAVA_OPTS="${JAVA_OPTS} -Dserver.port=${SERVER_PORT}";
JAVA_OPTS="${JAVA_OPTS} -DBUNDLECONFIG_DIR=./resources";
JAVA_OPTS="${JAVA_OPTS} -Dserver.local.startpath=${RESOURCES_HOME}";
JAVA_OPTS="${JAVA_OPTS} -DAAI_CHEF_ENV=${AAI_CHEF_ENV}";
JAVA_OPTS="${JAVA_OPTS} -DSCLD_ENV=${SCLD_ENV}";
JAVA_OPTS="${JAVA_OPTS} -DAFT_ENVIRONMENT=${AFT_ENVIRONMENT}";
JAVA_OPTS="${JAVA_OPTS} -DAAI_BUILD_VERSION=${AAI_BUILD_VERSION}";
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom";
JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=./resources/logback.xml";
JAVA_OPTS="${JAVA_OPTS} -Dloader.path=$APP_HOME/resources";
if [ ! -z ${PROXY_HOST} ]; then
        JAVA_OPTS="${JAVA_OPTS} -DproxySet=true -DproxyHost=${PROXY_HOST} -DproxyPort=8080";
fi        
JAVA_OPTS="${JAVA_OPTS} ${POST_JAVA_OPTS}";

JAVA_MAIN_JAR=$(ls lib/aai-cacher*.jar);

${JAVA_CMD} ${JVM_OPTS} ${JAVA_OPTS} -jar ${JAVA_MAIN_JAR};