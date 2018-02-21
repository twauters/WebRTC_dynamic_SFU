INSTALLER="cplex_studio127.linux-x86-64.bin"
FOLDER="$HOME/CPLEX_Studio127"

chmod +x $INSTALLER
./$INSTALLER
mvn install:install-file -DlocalRepositoryPath=$HOME/.m2/repository/ -DcreateChecksum=true -Dpackaging=jar -Dfile=$FOLDER/cplex/lib/cplex.jar -DgroupId=cplex -DartifactId=cplex -Dversion=1.0
