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

if [ ! -d "$basedir/import/i2p.i2p" ]; then
  git clone https://github.com/i2p/i2p.i2p.git
fi

cd "$basedir/import/i2p.i2p"
git fetch
git checkout tags/i2p-0.9.48
cd ..

if [ ! -d "$basedir/import/jdks" ]; then
  mkdir -p jdks
  mkdir -p jdks/linux jdks/mac jdks/win
  wget --directory-prefix=jdks/linux $JDK_DOWNLOAD_URL_LINUX
  wget --directory-prefix=jdks/mac $JDK_DOWNLOAD_URL_MAC
  wget --directory-prefix=jdks/win $JDK_DOWNLOAD_URL_WIN

  tar zxf jdks/linux/$JDK_DOWNLOAD_FILENAME_LINUX -C jdks/linux/
  tar zxf jdks/mac/$JDK_DOWNLOAD_FILENAME_MAC -C jdks/mac/
  unzip -q jdks/win/$JDK_DOWNLOAD_FILENAME_WIN -d jdks/win/
fi

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

if [ ! -d "$basedir/import/javafx-sdks" ]; then
  mkdir -p javafx-sdks
  mkdir -p javafx-sdks/linux javafx-sdks/mac javafx-sdks/win

  wget --directory-prefix=javafx-sdks/linux $JAVAFX_SDK_DOWNLOAD_URL_LINUX
  wget --directory-prefix=javafx-sdks/mac $JAVAFX_SDK_DOWNLOAD_URL_MAC
  wget --directory-prefix=javafx-sdks/win $JAVAFX_SDK_DOWNLOAD_URL_WIN

  unzip -q javafx-sdks/linux/`basename $JAVAFX_SDK_DOWNLOAD_URL_LINUX` -d javafx-sdks/linux/
  unzip -q javafx-sdks/mac/`basename $JAVAFX_SDK_DOWNLOAD_URL_MAC` -d javafx-sdks/mac/
  unzip -q javafx-sdks/win/`basename $JAVAFX_SDK_DOWNLOAD_URL_WIN` -d javafx-sdks/win/
fi

if [ ! -d "$basedir/import/javafx-jmods" ]; then
  mkdir -p javafx-jmods
  mkdir -p javafx-jmods/linux javafx-jmods/mac javafx-jmods/win
  wget --directory-prefix=javafx-jmods/linux $JAVAFX_JMODS_DOWNLOAD_URL_LINUX
  wget --directory-prefix=javafx-jmods/mac $JAVAFX_JMODS_DOWNLOAD_URL_MAC
  wget --directory-prefix=javafx-jmods/win $JAVAFX_JMODS_DOWNLOAD_URL_WIN

  set +e
  unzip -q javafx-jmods/linux/$JAVAFX_JMODS_DOWNLOAD_FILENAME_LINUX -d javafx-jmods/linux/
  unzip -q javafx-jmods/mac/$JAVAFX_JMODS_DOWNLOAD_FILENAME_MAC -d javafx-jmods/mac/
  unzip -q javafx-jmods/win/$JAVAFX_JMODS_DOWNLOAD_FILENAME_WIN -d javafx-jmods/win/
  set -e
fi

if [ ! -d "$basedir/import/jpackage" ]; then
  mkdir -p jpackage/linux jpackage/win
  set +e
  unzip -q $JAVA_HOME_LINUX/jmods/jdk.incubator.jpackage.jmod -d jpackage/linux/
  unzip -q $JAVA_HOME_WIN/jmods/jdk.incubator.jpackage.jmod -d jpackage/win/
  set -e
fi

