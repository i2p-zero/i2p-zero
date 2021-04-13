UniCode true

# define name of installer
OutFile "../I2P-Zero-installer.exe"
 
# define installation directory
!define APPNAME "I2P-Zero"
InstallDir "$PROGRAMFILES64\${APPNAME}\"

!define LICENSE_TITLE "BSD 3-Clause License"
PageEx license
	licensetext "${LICENSE_TITLE}"
	licensedata "../LICENSE"
PageExEnd
Page instfiles

# For removing Start Menu shortcut in Windows 7
RequestExecutionLevel admin

!include i2p-zero.nsi

# start default section
Section

	# Call the function that builds an I2P-Zero in the current directory
	Call buildZero
	# Call the function that installs I2P-Zero
	Call installZero

	# create the uninstaller
	WriteUninstaller "$INSTDIR\uninstall.exe"
# end default section
SectionEnd

# uninstaller section start
Section "uninstall"

	# Call the function that un-installs I2P-Zero
	Call uninstallZero

	# first, delete the uninstaller
	Delete "$INSTDIR\uninstall.exe"

	Delete "$SMPROGRAMS\Run I2P-Zero.lnk"

	RMDir $INSTDIR
# uninstaller section end
SectionEnd