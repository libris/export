[Exportprogrammet](https://github.com/libris/export) använder XL:s OAI-PMH-gränssnitt för att hämta återkonverterade poster från XL och sedan behandla dem enligt de befintliga exportprofiler som finns uppsatta idag för varje bibliotek. De här exporterna kommer senare att automatiseras så att de körs med den efterfrågade periodiciteten och läggs upp på Libris FTP-server. Dock kan det under testfasen behövas att man skapar testexporter baserad på de senaste förändringarna. Därav denna guide.

# Process för att skapa en testexport

Kontakta det testande biblioteket och be om en lista på Libris-idn till de poster som de vill testa med.
1. Lägg in dessa idn i [example_records.tsv](https://github.com/libris/librisxl/blob/develop/librisxl-tools/scripts/example_records.tsv) enligt formen för de testposter som redan finns där. Se till att varje rad slutar med "MITT BIBLIOTEK TESTPOST" eller något annat som särskiljer just de raderna. Bli inte förvirrad av de aukt- och hold-idn som finns inlagda i filen. Alla poster som är länkade till bib-posten kommer att importeras utan att de specificeras.
Exempel:
```
# Exempel på test-poster från example_records.tsv
[...]
bib/861	Aufstieg und Niedergang der römischen… huvudpost - GRÖNKÖPING TESTPOST
bib/949	Aufstieg und Niedergang der römischen… delpost - GRÖNKÖPING TESTPOST
bib/16673615	[Berg AB - samling av trycksaker] vardagstryck - GRÖNKÖPING TESTPOST
bib/4112678	Aftonbladet tidning - GRÖNKÖPING TESTPOST
bib/10637523	[Generalskan af Kleen] bild - GRÖNKÖPING TESTPOST
bib/3029208	Europische Saterdaeghs Courant. rar - GRÖNKÖPING TESTPOST
bib/9035371	Tidning utan namn rar, s.l., s.n - GRÖNKÖPING TESTPOST
bib/17092552	Historia de gentibvs septentrionalibvs… f1700, digi - GRÖNKÖPING TESTPOST
bib/2892798	Historia de gentibvs septentrionalibvs… f1700 - GRÖNKÖPING TESTPOST
bib/14804481	Karlskrönikan handskrift - GRÖNKÖPING TESTPOST
bib/17029378	Karlskrönikan handskrift, digi - GRÖNKÖPING TESTPOST
bib/3551686	Bibliotečnoe delo i bibliografija v SSSR rysk - GRÖNKÖPING TESTPOST
bib/10113464	Read & log on komb. - GRÖNKÖPING TESTPOST
bib/1398684	Fjällhedens hemlighet monografi - GRÖNKÖPING TESTPOST
bib/10735562	Sista boken från Finistère  monografi - GRÖNKÖPING TESTPOST
bib/4109936	Diskulogen periodika - GRÖNKÖPING TESTPOST
bib/8261682	Mikrodatorn periodika - GRÖNKÖPING TESTPOST
bib/3413533	Hemmets journal periodika - GRÖNKÖPING TESTPOST
bib/8261848	Sveriges natur årsbok - GRÖNKÖPING TESTPOST
bib/12308780	Sverige 1:1 200 000 karta - GRÖNKÖPING TESTPOST
bib/10346689	General passcharta öfwer… karta, äldre - GRÖNKÖPING TESTPOST
bib/8211184	Nationalencyklopedin huvudpost - GRÖNKÖPING TESTPOST
bib/11852064	Fixa pengar, annars! ljudbok, AVM - GRÖNKÖPING TESTPOST
[...]
```

2. Sätt upp din lokala miljö och kör [setup_dev_whelk.sh](https://github.com/libris/librisxl/blob/develop/librisxl-tools/scripts/setup-dev-whelk.sh) för att hämta in de nya exempelposterna.
3. Kopiera etc/config_xl.properties.in till etc/config.properties och fyll i rätt värden.
Exempel på lokal konfiguration:
```yml
OaiPmhBaseUrl="http://localhost:8080/oaipmh/"
URIBase="http://127.0.0.1:5000/"
User=""
Password=""
```

4. Kopiera etc/export.properties.in till etc/export.properties och anpassa efter behov. Det här är "exportprofilerna".
När du väl skapat den behöver du bara ändra på format  beroende på om du vill ha ISO2709 eller MARCXML

```yml
# Exempelfil för Libris export via OAI-PMH
name=Grö
longname=Grönköpings folkbibliotek

# postutseende
authtype=interleaved
holdtype=interleaved
locations=Grö Grös

# datamassage
f003=SE-LIBR
lcsh=off
isbn=dehyphenate
move0359=on
sabtitles=off
extrafields=A\:650; B\:650; C\:650
nameform=standard
move240to244=off
move240to500=off
addauthlinks=off
generatedewey=off
generatesab=off
addauthids=on

# filtrering
licensefilter=on
biblevel=on
efilter=off

# output
composestrategy=composelatin1
format=ISO2709
#format=MARCXML
characterencoding=UTF-8
```
	
5. Starta din lokala OAI-PMH-server.
6. För att hämta alla poster som hör till testexporten, kör följande:   

```bash
grep "GRÖNKÖPING TESTPOST" ../librisxl/librisxl-tools/scripts/example_records.tsv |    
cut -f 1 |    
awk '$0="http://libris.kb.se/"$0' |    
gradle -q get_records_xl > gronkoping-test-iso2709.txt
```


>Förklaring:   
>Första raden söker igenom alla rader i example_records.tsv som matchar "GRÖNKÖPING TESTPOST".   
>Andra raden tar emot dessa rader och klipper ut den första kolumnen i varje rad.   
>Tredje raden skapar libris-uri:er av id:na.   
>Fjärde raden anropar exportprogrammet med de behandlade id:na och skriver den resulterande exporten till en fil.   

Anropet borde resultera i något i stil med 

```tsv
861       3-11-005837-5                          Aufstieg und Niedergang der rö
949       3-11-015006-9                          Aufstieg und Niedergang der rö
16673615                                         [Berg AB - samling av trycksak
4112678   1103-9000                              Aftonbladet 1830-
10637523                   art Röhl, Maria 18... [Generalskan af Kleen] Bild
3029208                                          Europische Saterdaeghs Courant
9035371                                          Tidning utan namn -
17092552                   Olaus Magnus, 1490... Historia de gentibvs septentri
2892798                    Olaus Magnus, 1490... Historia de gentibvs septentri
14804481                                         Karlskrönikan D 6 Handskrift]
17029378                                         Karlskrönikan D 6 Elektronisk
3551686   0208-2047                              Bibliotečnoe delo i bibliograf
10113464  91-44-04367-8                          Read & log on Cecilia Augutis
1398684                    Göransson-Ljungman... Fjällhedens hemlighet Kerstin
10735562  9789100119003    Malmsten, Bodil 19... Sista boken från Finistère Bod
4109936   1101-3826                              Diskulogen forum för diskussio
WARNING - NO SUCH BIB_ID: http://libris.kb.se/bib/8261682
3413533   0018-0327                              Hemmets journal 1921-
8261848   0349-5264                              Sveriges natur Svenska natursk
12308780  978-91-588-91...                       Sverige 1:1 200 000 Lantmäteri
10346689                   ctg Gedda, Petter ... General passcharta öfwer Öster
8211184   91-7024-621-1                          Nationalencyklopedin ett uppsl
11852064  91-85833-26-6    aut                   Fixa pengar, annars-! Lena Lil
```  
De rader med "WARNING - NO SUCH BIB_ID" beror oftast på att de inte kunnat återkonverteras. De kan också saknas i din lokala miljö. För att kontrollera, slå upp posten mot din egen OAI-PMH-server genom att - i enlighet med exemplet ovan - gå till [http://localhost:8080/oaipmh/?verb=GetRecord&metadataPrefix=marcxml_includehold_expanded&   identifier=http://libris.kb.se/bib/8261682](http://localhost:8080/oaipmh/?verb=GetRecord&metadataPrefix=marcxml_includehold_expanded&identifier=http://libris.kb.se/bib/8261682)
  

Du borde då troligen mötas av något i stil med
```xml
<?xml version="1.0" encoding="UTF-8"?>
<OAI-PMH 
    xmlns="http://www.openarchives.org/OAI/2.0/" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd">
    <responseDate>2017-06-29T08:18:27.553Z</responseDate>
    <request identifier="http://libris.kb.se/bib/8261682" metadataPrefix="marcxml_includehold_expanded" verb="GetRecord">http://localhost:8080/oaipmh/</request>
    <GetRecord>
        <record>
            <header>
                <identifier>http://127.0.0.1:5000/2kc87c1d11gzlzh</identifier>
                <datestamp>2017-06-28T12:18:33.703198Z</datestamp>
                <setSpec>bib</setSpec>
            </header>
            <metadata>Error: Document conversion failed.</metadata>
            <about>                
                <holding sigel="Ylm" id="fkrvqz6l06dvlwp"></holding>
                <holding sigel="NLT" id="fkrhm3tl1vlz5v4"></holding>
                <holding sigel="Alb2" id="26fnsch70l2459r"></holding>
            </about>
        </record>
    </GetRecord>
</OAI-PMH>
```

	

För att se en stack trace på ovanstående fel går man till loggen för OAI-PMH-serven, som redan borde skrivas ut i terminalen:
```
2017-06-29T10:18:27,540 [935548159@qtp-915318691-4] INFO  whelk.export.servlet.OaiPmh - Received request with verb: GetRecord from 0:0:0:0:0:0:0:1:60545.
2017-06-29T10:18:27,678 [935548159@qtp-915318691-4] ERROR whelk.export.servlet.ResponseCommon - Conversion failed for document: 2kc87c1d11gzlzh
org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object '4' with class 'java.lang.String' to class 'java.util.Map'
        at org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.continueCastOnSAM(DefaultTypeTransformation.java:405) ~[groovy-all-2.4.7.jar:2.4.7]
        at org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.continueCastOnNumber(DefaultTypeTransformation.java:319) ~[groovy-all-2.4.7.jar:2.4.7]
        at org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.castToType(DefaultTypeTransformation.java:232) ~[groovy-all-2.4.7.jar:2.4.7]
        at org.codehaus.groovy.runtime.ScriptBytecodeAdapter.castToType(ScriptBytecodeAdapter.java:603) ~[groovy-all-2.4.7.jar:2.4.7]
        at whelk.converter.JSONMarcConverter$_fromJson_closure1$_closure2.doCall(JSONMarcConverter.groovy:44) ~[xlcore.jar:?]
        at sun.reflect.GeneratedMethodAccessor147.invoke(Unknown Source) ~[?:?]
```

Nu ska det vara klart. Det är trevligt om man skickar exporten i två versioner till mottagaren: En ISO2709-fil för system-import och en MARCXML-fil som är mer lättläst.

# Export av enstaka poster
För att exportera enstaka poster är det lite enklare. Då behver man först starta export-programmet:
```
gradle -q get_records_xl > export_iso2709.txt 
```

Ge sedan ett ID till programmet via terminalen och den konverterade posten skrivs till filen.  Observera att posten måste finnas i din lokala miljö



