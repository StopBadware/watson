#!/usr/bin/env bash

PLAY_VERSION=2.2.1
wget -nv http://downloads.typesafe.com/play/${PLAY_VERSION}/play-${PLAY_VERSION}.zip
unzip play-${PLAY_VERSION}.zip -d ${HOME}
# Manually populate the play directory with missing jars - TODO: remove this hack (see http://stackoverflow.com/questions/21361621/)
SCALA_VERSION=2.10
MAVEN_PLAY=http://repo.typesafe.com/typesafe/maven-releases/com/typesafe/play/play_${SCALA_VERSION}/${PLAY_VERSION}/play_${SCALA_VERSION}-${PLAY_VERSION}
LOCAL_REPO=${HOME}/play-${PLAY_VERSION}/repository/local/com.typesafe.play/play_${SCALA_VERSION}/${PLAY_VERSION}
mkdir ${LOCAL_REPO}/srcs/ ${LOCAL_REPO}/poms/
wget -nv ${MAVEN_PLAY}.pom --output-document ${LOCAL_REPO}/poms/play_${SCALA_VERSION}.pom
wget -nv ${MAVEN_PLAY}-sources.jar --output-document ${LOCAL_REPO}/srcs/play_${SCALA_VERSION}-sources.jar
wget ${MAVEN_PLAY}-test-sources.jar --output-document ${LOCAL_REPO}/srcs/play_${SCALA_VERSION}-test-sources.jar
sudo chown -R vagrant:vagrant /home/vagrant/play-${PLAY_VERSION}
echo export PATH=\$PATH:${HOME}/play-${PLAY_VERSION} >> ${HOME}/.bashrc
#source ${HOME}/.bashrc
#cd /vagrant
#play ~run
