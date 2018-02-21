#!/bin/bash
#install java
bash install_java.sh
#install maven
bash install_maven.sh
#install nodejs
cd node-v6.9.4/
./configure
make
sudo make install
cd ..
#prosody
sudo apt-get install -y prosody
sudo cp jitsi.cfg.lua /etc/prosody/conf.avail/
sudo ln -s /etc/prosody/conf.avail/jitsi.cfg.lua /etc/prosody/conf.d/jitsi.cfg.lua
sudo prosodyctl cert generate jitsi
sudo prosodyctl register focus auth.jitsi YOURSECRET3
sudo prosodyctl restart
#nginx
sudo apt-get install -y nginx
PWD_=${PWD//\//\\/}
sed -i "/ssl_certificate /c\\\\tssl_certificate ${PWD_}\/certs\/domain.crt;" jitsi
sed -i "/ssl_certificate_key /c\\\\tssl_certificate_key ${PWD_}\/certs\/domain.key;" jitsi
sudo cp jitsi /etc/nginx/sites-available
sudo ln -s /etc/nginx/sites-available/jitsi /etc/nginx/sites-enabled/jitsi
sudo invoke-rc.d nginx restart
#jitsi-meet
sed -i "/\"lib-jitsi-meet\"/c\\\\t\"lib-jitsi-meet\": \"file:\/\/${PWD_}\/lib-jitsi-meet\"," jitsi-meet/package.json
sudo cp -r jitsi-meet/ /srv/; sudo mv /srv/jitsi-meet /srv/jitsi
cd /srv/jitsi
sudo npm install
sudo make
cd $HOME/jitsi-videobridge
#CPLEX
cp /groups/wall2-ilabt-iminds-be/shift-tv/cplex_studio127.linux-x86-64.bin .
bash install_cplex.sh
mkdir $HOME/.sip-communicator
cp sip-communicator.properties $HOME/.sip-communicator/
cd libjitsi
./compile.sh
cd ..
cd jicofo
./compile.sh
cd ..
echo "Installation is DONE"
