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
    
Skicka en en fil med bib_id (`fil.txt`) till exportprogrammet och spara posterna i `utfil.txt`

    # cat fil.txt | gradle -q get_records > utfil.txt
