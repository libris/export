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

## Windows
Coming soon ...

## Användning - exempel

* Kopiera etc/config.properties.in till etc/config.properties och fyll i rätt värden
* Kopiera etc/export.properties.in till etc/export.properties och anpassa efter behov
* Använd gradle för att hämta poster

    gradle -q get_records
