#!/bin/bash

rm -r ./jicofo-linux-x64-1.0-SNAPSHOT/
mvn -U clean package -DskipTests -Dassembly.skipAssembly=false
cd target/
unzip jicofo-linux-x64-1.0-SNAPSHOT.zip
mv ./jicofo-linux-x64-1.0-SNAPSHOT/ ../
