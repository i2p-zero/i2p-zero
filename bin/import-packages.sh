#!/bin/bash
set -e
set -o pipefail

if [ $(uname -s) = Darwin ]; then
    basedir=$(dirname $(cd "$(dirname "$0")"; pwd -P))
else
    basedir=$(dirname $(dirname $(readlink -fm $0)))
fi

source "$basedir/bin/java-config.sh"

mkdir -p "$basedir/import"
cd "$basedir/import"

i2pTag="i2p-0.9.48"

if [ -d "$basedir/import/i2p.i2p" ]; then
  cd "$basedir/import/i2p.i2p"
  # check if we've shallow-copied the commit for the required i2p tag
  if [[ ! ("$(git tag)" == "$i2pTag" && "$(git rev-list -n 1 $i2pTag)" == "$(git rev-parse HEAD)") ]]; then
    cd ..
    rm -fr "$basedir/import/i2p.i2p"
  else
    cd ..
  fi
else
  git clone --branch $i2pTag --depth 1 https://github.com/i2p/i2p.i2p.git
fi


for target in ${TARGETS};do
  if [ ! -d "$basedir/import/jdks/${target}" ]; then
    mkdir -p jdks/${target}

    JDK_FILENAME=${variables["JDK_DOWNLOAD_FILENAME_$target"]}
    wget --directory-prefix=jdks/${target} "https://github.com/AdoptOpenJDK/openjdk${JDK_MAJOR_VERSION}-binaries/releases/download/jdk-${JDK_VERSION_URL_ENC}/${JDK_FILENAME}"
    ${variables["DECOMPRESS_$target"]} jdks/${target}/${JDK_FILENAME}
  fi
done;

if [ ! -d "$basedir/import/apache-ant-1.10.7" ]; then
  wget https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.7-bin.tar.gz
  tar zxf apache-ant-1.10.7-bin.tar.gz
fi

if [ ! -d "$basedir/import/commons-compress-1.20" ]; then
  mkdir -p commons-compress-1.20
  wget --directory-prefix=commons-compress-1.20 https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar
fi

if [ ! -d "$basedir/import/org-json" ]; then
  mkdir -p org-json
  wget --directory-prefix=org-json https://repo1.maven.org/maven2/org/json/json/20200518/json-20200518.jar
fi

if [ ! -d "$basedir/import/jetty-lib" ]; then
  mkdir -p jetty-lib
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-server/9.4.26.v20200117/jetty-server-9.4.26.v20200117.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-util/9.4.26.v20200117/jetty-util-9.4.26.v20200117.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-http/9.4.26.v20200117/jetty-http-9.4.26.v20200117.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-io/9.4.26.v20200117/jetty-io-9.4.26.v20200117.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar
  wget --directory-prefix=jetty-lib https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-jmx/9.4.26.v20200117/jetty-jmx-9.4.26.v20200117.jar
fi

for target in ${TARGETS};do
  if [ ! -d "$basedir/import/javafx-sdks/${target}" ]; then
    mkdir -p javafx-sdks/${target}
    SDK_FILENAME=${variables["JAVAFX_SDK_FILENAME_$target"]}
    wget --directory-prefix=javafx-sdks/${target} https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/${SDK_FILENAME}
    unzip -q javafx-sdks/${target}/${SDK_FILENAME} -d javafx-sdks/${target}/
  fi
done;

for target in ${TARGETS};do
  if [ ! -d "$basedir/import/javafx-jmods/${target}" ]; then
    mkdir -p javafx-jmods/${target}
    wget --directory-prefix=javafx-jmods/${target} https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/${variables["JAVAFX_JMODS_FILENAME_$target"]}
    set +e
    unzip -q javafx-jmods/${target}/${variables["JAVAFX_JMODS_FILENAME_$target"]} -d javafx-jmods/${target}/
    set -e
  fi
done;

for target in ${TARGETS};do
  if [ ${target} != "mac" ] && [ ! -d "$basedir/import/jpackage/${target}" ]; then
    mkdir -p jpackage/${target}
    set +e
    unzip -q jdks/${target}/${variables["JAVA_HOME_$target"]}/jmods/jdk.incubator.jpackage.jmod -d jpackage/${target}/
    set -e
  fi
done;
