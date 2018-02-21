cd $HOME
mkdir weka
cd weka
wget http://prdownloads.sourceforge.net/weka/weka-3-8-1.zip
unzip weka-3-8-1.zip
mvn install:install-file -DlocalRepositoryPath=$HOME/.m2/repository/ -DcreateChecksum=true -Dpackaging=jar -Dfile=$HOME/weka/weka-3-8-1/weka.jar -DgroupId=weka -DartifactId=weka -Dversion=1.0

