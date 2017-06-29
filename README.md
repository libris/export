# Export

    DISCLAIMER: denna programvara, och framförallt dokumentationen av den, är under uppbyggnad
    och ännu inte användbar fullt ut.

Librisexport via OAI-PMH

## Krav

### Komponenter

* Java 6+
* Groovy

### Byggverktyg

* Gradle 2.1+ (<http://gradle.org/>)

### Mac OS X via HomeBrew (http://brew.sh/)

    # brew install groovy
    # brew install gradle

### Linux

    # sudo apt-get install groovy
    # sudo apt-get install gradle

### Windows

* http://groovy-lang.org/download.html
* https://docs.gradle.org/current/userguide/installation.html

### Konfiguration

* Kopiera etc/config.properties.in till etc/config.properties och fyll i rätt värden
* Kopiera etc/export.properties.in till etc/export.properties och anpassa efter behov

## Exempel
Ange bib_id manuellt och få posterna utskrivna i terminalen (högst oanvändbart annat än i testsyfte). Använd Ctrl-D för att avsluta

    # gradle -q get_records
    
Skapa en fil med bib_id:n för (1) ändrade bibposter, (2) bibposter med ändrade auktoritetsposter och (3) bibposter med ändrade holdingsposter.

    # gradle -q list_changes -Prange=2016-03-01T00:00:00Z,2016-03-01T23:59:59Z > fil.txt

Skicka en en fil med bib_id (`fil.txt`) till exportprogrammet och spara posterna i `utfil.txt`

    # cat fil.txt | gradle -q get_records > utfil.txt
    
## Genomföra en testexport
[Guide för att genomföra en testexport](https://github.com/libris/export/blob/master/docs/manuell_export.md) givet en given exportprofil för att testa den senaste versionen av Libris XL