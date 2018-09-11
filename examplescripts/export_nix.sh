#!/bin/bash

# Det här skriptet kan användas som exempel på hur man automatiskt hämtar poster från Libris
# Innan du använder det, se till att du fyllt i filerna:
# etc/config_xl.properties
# etc/export.properties
#
# Lämpligen körs detta skript minut-vis m h a cron.

set -e

# Se till att vi inte kör flera instanser av skriptet samtidigt
[ "${FLOCKER}" != "$0" ] && exec env FLOCKER="$0" flock -en "$0" "$0" "$@" || :

# Om exportprogrammet inte kompilerats än
JARPATH="build/libs/export-3.0.0-alpha.jar"
if [ ! -e $JARPATH ]
then
    ./gradlew jar
fi

# Om vi kör för första gången, sätt 'nu' till start-tid
LASTRUNTIMEPATH="lastRun.timestamp"
if [ ! -e $LASTRUNTIMEPATH ]
then
    date -u +%Y-%m-%dT%H:%M:%SZ > $LASTRUNTIMEPATH
fi

# Avgör vilket tidsintervall vi ska hämta
STARTTIME=`cat $LASTRUNTIMEPATH`
STOPTIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)

java -jar $JARPATH ListChanges_xl -Prange=$STARTTIME,$STOPTIME > bibids
java -jar $JARPATH GetRecords_xl > export.txt < bibids

# Om allt gick bra, uppdatera tidsstämpeln
echo $STOPTIME > $LASTRUNTIMEPATH

# DINA ÄNDRINGAR HÄR, gör något produktivt med datat i 'export.txt', t ex:
# cat export.txt
