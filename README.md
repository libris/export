# Export

Librisexport via OAI-PMH

## Krav

### Komponenter

* Java 6+ JDK (Java Development Kit). Java JRE är ej tillräckligt.

### Konfiguration

* Kopiera etc/config_xl.properties.in till etc/config_xl.properties och fyll i rätt värden
* Kopiera etc/export.properties.in till etc/export.properties och anpassa efter behov

## Exempel
Ange bib_id manuellt och få posterna utskrivna i terminalen (högst oanvändbart annat än i testsyfte). Använd Ctrl-D för att avsluta

    # gradlew -q get_records_xl
    
Skapa en fil med bib_id:n för (1) ändrade bibposter, (2) bibposter med ändrade auktoritetsposter och (3) bibposter med ändrade holdingsposter.

    # gradlew -q list_changes_xl -Prange=2017-11-01T00:00:00Z,2018-01-01T23:59:59Z > fil.txt

Skicka en en fil med bib_id (`fil.txt`) till exportprogrammet och spara posterna i `utfil.txt`

    # cat fil.txt | gradlew -q get_records_xl > utfil.txt
    
## Genomföra en testexport
[Guide för att genomföra en testexport](https://github.com/libris/export/blob/master/docs/manuell_export.md) givet en given exportprofil för att testa den senaste versionen av Libris XL