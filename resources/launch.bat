@echo off
set DIR=%~dp0
"%DIR%\java" -cp %DIR%\..\i2p.base\jbigi.jar -m org.getmonero.i2p.embedded --i2p.dir.base=%DIR%\..\i2p.base --i2p.dir.config=%DIR%\..\i2p.config %*