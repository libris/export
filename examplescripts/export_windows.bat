@echo off

rem Det här skriptet kan användas som exempel på hur man automatiskt hämtar poster från Libris
rem Innan du använder det, se till att du fyllt i filerna:
rem etc/config_xl.properties
rem etc/export.properties
rem Lämpligen körs detta skript minut-vis som ett schemalagt jobb. SE TILL ATT DET INTE STARTAS INNAN FÖREGÅENDE KÖRNING ÄR KLAR!

rem Om exportprogrammet inte kompilerats än
set jarpath=build/libs/export-3.0.0-alpha.jar
IF NOT EXIST %jarpath% (
	gradlew.bat jar
)

rem Om vi kör för första gången, använd 'nu' som start-tid.
IF NOT EXIST lastRun.timestamp (
	set currentTime=%DATE%T%TIME:~0,8%Z
	echo %currentTime%>lastRun.timestamp
)

rem Avgör tidsintervall
set /p startTime=<lastRun.timestamp
set stopTime=%DATE%T%TIME:~0,8%Z

java -jar %jarpath% ListChanges_xl -Prange^=%startTime%,%stopTime% | java -jar %jarpath% GetRecords_xl > export.txt

rem Om allt gick bra, uppdatera tidsstämpeln
echo %stopTime%>lastRun.timestamp

rem DINA ÄNDRINGAR HÄR, gör något produktivt med datat i 'export.txt', t ex:
rem Type export.txt