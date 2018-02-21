# jitsi-videobridge

## Install Java
- run ``` ./install_java.sh``` (java jdk tar is downloaded in the script)

## Install maven
- run ``` ./install_maven.sh```

## Install NodeJs
- ``` cd node-v6.9.4/ ```
- ``` ./configure ```
- ``` make ```
- ``` sudo make install ```

## Network description

This how the network looks like:
```
                   +                           +
                   |                           |
                   |                           |
                   v                           |
                  443                          |
               +-------+                       |
               |       |                       |
               | NginX |                       |
               |       |                       |
               +--+-+--+                       |
                  | |                          |
+------------+    | |    +--------------+      |
|            |    | |    |              |      |
| jitsi-meet +<---+ +--->+ prosody/xmpp |      |
|            |files 5280 |              |      |
+------------+           +--------------+      v
                     5222,5347^    ^5347      4443
                +--------+    |    |    +-------------+
                |        |    |    |    |             |
                | jicofo +----^    ^----+ videobridge |
                |        |              |             |
                +--------+              +-------------+
```

## Install prosody
```sh
apt-get install prosody
```
## Configure prosody
- ```cp jitsi.cfg.lua /etc/prosody/conf.avail/ ``` (it may be useful to change to root: ```sudo su ```)
- ``` ln -s /etc/prosody/conf.avail/jitsi.cfg.lua /etc/prosody/conf.d/jitsi.cfg.lua ```
- Generate certs for the domain:
```sh
prosodyctl cert generate jitsi
```
- Create conference focus user:
```sh
prosodyctl register focus auth.jitsi YOURSECRET3
```
- Restart prosody XMPP server with the new config
```sh
prosodyctl restart
```

## Install nginx
``` sudo apt-get install nginx ```
## Configure nginx
- edit paths to certificate and private key in file "jitsi"
- ``` cp jitsi /etc/nginx/sites-available ```
- Add link for the added configuration
```sh
ln -s /etc/nginx/sites-available/jitsi /etc/nginx/sites-enabled/jitsi
```
- Restart nginx:
```
sudo invoke-rc.d nginx restart
```

## Install jitsi-meet
- Change location of the "lib-jitsi-meet" inside file package.json to $YOUR+PATH_TO_JITSI_MEET/lib-jistsi-meet
- Execute the following commands:
```
sudo cp -r jitsi-meet/ /srv/; sudo mv /srv/jitsi-meet /srv/jitsi
cd /srv/jitsi
sudo npm install
sudo make
```

## Install CPLEX (to solve ILP problem)
- Download IBM CPLEX: https://ibm.onthehub.com/WebStore/OfferingDetails.aspx?o=9b4eadea-9776-e611-9421-b8ca3a5db7a1
- Modify the script install_cplex.sh with the name of the installers and the installation folder (note: the installation folder in the script must be the same as that requested during the CPLEX installation process)
- Execute ./install_cplex.sh

## Install WEKA (machine learning framework for clustering algorithm)
- Execute ./install_weka.sh

## Compile libjitsi
- Inside the libjitsi folder execute ./compile.sh (change the field HOME_FOLDER)

## Install jitsi-videobrigde
- In the user home that will be starting Jitsi Videobridge create `.sip-communicator` folder and copy sip-communicator.properties to the folder
- start the videobridge (in compile.sh, modify the JVB_HOME data field): 
``` 
cd jitsi-videobrigde
./compile.sh
```
- In case of maven error, use the -U option on the mvn command (inside compile.sh)
- If you get the error "[WARNING] The POM for cplex:cplex:jar:1.0 is missing, no dependency information available",
  Add the following lines in the pom.xml: 
```   
<dependency>
  <groupId>cplex</groupId>
  <artifactId>cplex</artifactId>
  <version>1.0</version>
  <scope>system</scope>
  <systemPath>$HOME/CPLEX_Studio127/cplex/lib/cplex.jar</systemPath>
</dependency>
```

## Intall and run Jitsi Conference Focus (jicofo)
- Inside the jicofo folder, execute ./compile.sh
- To run jicof execute:
``` 
cd jicofo/jicofo-linux-x64-1.0-SNAPSHOT
./jicofo.sh --host=localhost --domain=jitsi --secret=YOURSECRET2 --user_domain=auth.jitsi --user_name=focus --user_password=YOURSECRET3
```

## Install chrome on sender/client
- Use the image available at "urn:publicid:IDN+wall1.ilabt.iminds.be+image+wall2-ilabt-iminds-be:SenderClientFinal" when initializing the node on the Virtual Wall
- Execute the following script (in JFed: Timeline Viewer -> Instant -> execute for all senders and clients):
```
bash /SFU/config_client.sh
```

## Experiments in Google Chrome

- Senders: ``` rm -r .cache/google-chrome/Default/Cache/*; google-chrome --enable-logging --v=1 --no-first-run --no-default-brower-check --disable-translate --use-fake-device-for-media-stream --disable-gpu "https://jitsi/test?simulcast=false&sendonly=true" ```
- Receivers: ``` rm -r .cache/google-chrome/Default/Cache/*; google-chrome --enable-logging --v=1 --no-first-run --no-default-brower-check --disable-translate --use-fake-device-for-media-stream --disable-gpu "https://jitsi/test?recvonly=true&simulcast=false" ```

