@echo off
set DIR=%~dp0
"%DIR%\java" --enable-preview -cp %DIR%\..\i2p.base\jbigi.jar -m org.getmonero.i2p.zero %*