
!define ZERONAME "I2P-Zero"
!define I2P64INSTDIR "$PROGRAMFILES64\I2P\"
!define I2P32INSTDIR "$PROGRAMFILES32\I2P\"
!define ZEROINSTDIR "$PROGRAMFILES64\${APPNAME}\"

function buildZero
	!system "echo '#! /usr/bin/env sh' > build-docker.sh"
	!system "echo 'docker rm -f i2p-zero-build' >> build-docker.sh"
	!system "echo 'docker run -td --name i2p-zero-build --rm ubuntu' >> build-docker.sh"
	!system "echo 'docker exec -ti i2p-zero-build bash -c ;' >> build-docker.sh"
	!system "echo '  apt-get update && ' >> build-docker.sh"
	!system "echo '  apt-get -y install git wget zip unzip && ' >> build-docker.sh"
	!system "echo '  git clone https://github.com/i2p-zero/i2p-zero.git && ' >> build-docker.sh"
	!system "echo '  cd i2p-zero && bash bin/build-all-and-zip.sh;' >> build-docker.sh"
	!system "echo 'docker cp i2p-zero-build:/i2p-zero/dist-zip' ./ >> build-docker.sh"
	!system "echo 'docker container stop i2p-zero-build' >> build-docker.sh"
	!system "sed -i $\"s|;|'|g$\" build-docker.sh"
	!system "chmod +x build-docker.sh"
	!system ./build-docker.sh
	!system "unzip dist-zip/i2p-zero-win-gui.*.zip"
	!system "rm -rf I2P-Zero"
	!system "mv i2p-zero-win-gui.* I2P-Zero"
functionEnd

function installZero
	${If} ${FileExists} `$I2P64INSTDIR\I2P.exe`
		SetOutPath $ZEROINSTDIR
	${If} ${FileExists} `$I2P32INSTDIR\I2P.exe`
		SetOutPath $ZEROINSTDIR
	${Else}
		SetOutPath $ZEROINSTDIR
		File /a /r "./I2P-Zero/"

		CreateShortcut "$SMPROGRAMS\Run I2P-Zero.lnk" "$ZEROINSTDIR\router\i2p-zero.exe"
	${EndIf}

functionEnd

function uninstallZero
	Delete "$SMPROGRAMS\Run I2P-Zero.lnk"
	RMDir $INSTDIR

functionEnd