#!/bin/bash
NAME=$(whoami)

HOST="localhost"
DOMAIN="jitsi"
PORT="5347"
SECRET="YOURSECRET1"
JVB_HOME=$(pwd)
CPLEX_FOLDER="/users/${NAME}/CPLEX_Studio127"

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CPLEX_FOLDER/cplex/bin/x86-64_linux
mvn compile exec:java -Dexec.args="--host=$HOST --domain=$DOMAIN --port=$PORT --secret=$SECRET" -Djava.library.path=$JVB_HOME/lib/native/linux-64:$CPLEX_FOLDER/cplex/bin/x86-64_linux -Djava.util.logging.config.file=$JVB_HOME/lib/logging.properties
