# Export

Librisexport via OAI-PMH. Det här programmet erbjuder en metod för att hålla ett bibliotekssystem kontinuerligt uppdaterat med data ifrån Libris. Programmet fungerar så att det vid körning genererar en exportfil med alla uppdateringar som skett inom ett visst tidsintervall. Dom exportfiler som genereras är likadana som dom som hämtas från Libris via FTP. Fördelen med att använda det här programmet istället för att hämta filer via FTP, är att uppdateringar av poster kan nå ut mycket snabbare. Det här programmet kan köras när och hur ofta man vill för att få hem uppdateringar och man behöver alltså inte vänta på att filer ska genereras över natten. För att få kontinuerliga uppdateringar utan att behöva administrera tidsintervallet för hand, så finns det skript i katalogen examplescripts som sköter detta automatiskt.

## Systemkrav

* Progammet curl finns förinstallerat på Linux och OSX. Om du använder Windows kan du ladda ner det ifrån: https://curl.haxx.se/windows/ Se till att filerna ifrån bin katalogen ligger i samma katalog som du kör exportprogrammet ifrån.
* En korrekt inställd klocka. Det är mycket viktigt att den dator som kör skripten/programmet har en korrekt inställd klocka. Det finns en säkerhetsmarginal på 10 sekunder, men om datorns klocka går mer än 10 sekunder före den korrekta tiden så finns en risk att man missar ändringar som görs i Libris. Använd en NTP-server för att se till att datorns klocka går rätt.
* För att använda exempelskripten på OSX krävs också programmet flock (kan installeras med Homebrew: `brew install flock`)


## Uppsättning

Detta bör göras av en person som är kunning inom IT-administration. För att använda export-programmet i drift rekommenderar vi att man gör ändringar i något av skripten under exampelscripts (för att passa det lokala systemet). Dessa skript kan anropas kontinuerligt och hämtar då allt data som ändrats sedan föregående gång dom kördes. Förslagsvis schemaläggs dessa att köras varje minut.

### Exportprofil

För att programmet ska fungera behöver en så kallad export-profil finnas i filen etc/export.properties. Har ni ingen export-profil sedan tidigare så kan en enkel sådan kopieras från filen etc/export.properties.in
Exportprofilen reglerar vissa delar av hur export-filerna ska se ut, t ex ifall poster ska exporteras som MARCXML eller ISO2709. I exportprofilen behöver också det "sigel" ni använder finnas med ("locations=[ERT SIGEL]")

### Steg för steg

Steg för steg görs en uppsättning av exportprogrammet så här:

1. Ladda ned exportprogrammet, antingen genom "download-knappen" på github, eller genom att använda git (git clone https://github.com/libris/export.git)
1. Kopiera filen etc/export.properties.in till etc/export.properties
   1. Öppna etc/export.properties i en texteditor och ändra raden "locations=*" till "locations=ERT_SIGEL"
1. Kopiera filen examplescripts/export_windows.bat till ./export.bat (alternativt export_nix.sh om ni kör på ett unix/linux/bsd/osx-system)
   1. Öppna ./export.bat och hitta raden där det står "DINA ÄNDRINGAR HÄR"
   1. Fyll i nödvändiga kommandon för att ladda in en befintlig marc-fil till ert system. Tyvärr går detta till på olika sätt för olika system. Er systemleverantör kan svara på hur ni ska göra för ert system.
1. Schemalägg körning av ./export.bat så ofta som ni vill ha uppdateringar. Se till att skriptet körs i den katalog där det ligger.
