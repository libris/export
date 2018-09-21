# NEVER RUN THIS TEST CASE ON AN ENVIRONMENT WHERE THE DATA MATTERS!
# These tests are not portable. They require a locally running OAIPMH server on http://localhost:8080/oaipmh/, tied to a local postgresql instance which can be logged into (with full rights) using psql -qAt whelk_dev

import os
import json
import xml.etree.ElementTree as ET

base_uri = 'http://kblocalhost.kb.se:5000/'
    
## Util-stuff
    
def reset():
    os.system("psql whelk_dev -c \"delete from lddb__identifiers where id in (select id from lddb where changedIn = 'integtest');\"")
    os.system("psql whelk_dev -c \"delete from lddb__dependencies where id in (select id from lddb where changedIn = 'integtest');\"")
    os.system("psql whelk_dev -c \"delete from lddb where changedIn = 'integtest';\"")

def importBib(jsonstring, agent, systemid):
    jsonstring = jsonstring.replace("TEMPID", systemid)
    jsonstring = jsonstring.replace("TEMPBASEURI", base_uri)
    os.system("psql whelk_dev -c 'insert into lddb values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'bib', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid, '0', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid+'#it', '1', 'true'))

def importHold(jsonstring, agent, systemid, itemof, sigel):
    jsonstring = jsonstring.replace("TEMPID", systemid)
    jsonstring = jsonstring.replace("TEMPBASEURI", base_uri)
    jsonstring = jsonstring.replace("TEMPITEMOF", itemof)
    jsonstring = jsonstring.replace("TEMPSIGEL", sigel)
    os.system("psql whelk_dev -c 'insert into lddb values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'hold', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid, '0', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid+'#it', '1', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__dependencies (id, relation, dependsOnId) values($${}$$, $${}$$, $${}$$);'".format(systemid, 'itemOf', itemof))

def setModified(systemid, timestring):
    os.system("psql whelk_dev -c 'update lddb set modified = $${}$$ where id = $${}$$;'".format(timestring, systemid))
    os.system("psql whelk_dev -c 'update lddb set depMaxModified = $${}$$ where id = $${}$$;'".format(timestring, systemid))
    
def doExport(fromTime, toTime, profileName):
    os.system("cp -f ./testdata/profiles/{}.properties ./etc/export.properties".format(profileName))
    os.system("java -jar export.jar ListChanges_xl -Prange={},{} > bibids".format(fromTime, toTime))
    os.system("java -jar export.jar GetRecords_xl > export.dump < bibids")

def assertExported(record001, failureMessage):
    with open('export.dump') as fh:
        dump = fh.read()
    xmlDump = ET.fromstring(dump)
    for elem in xmlDump.findall("{http://www.loc.gov/MARC21/slim}record/{http://www.loc.gov/MARC21/slim}controlfield[@tag='001']"):
        print (elem.text)
        if elem.text == record001:
            return
    failedCases.append(failureMessage)

def assertNotExported(record001, failureMessage):
    with open('export.dump') as fh:
        dump = fh.read()
    xmlDump = ET.fromstring(dump)
    for elem in xmlDump.findall("{http://www.loc.gov/MARC21/slim}record/{http://www.loc.gov/MARC21/slim}controlfield[@tag='001']"):
        print (elem.text)
        if elem.text == record001:
            failedCases.append(failureMessage)
    return
    
## Init

with open('testdata/bib0.jsonld') as fh:
    bibtemplate = fh.read()
with open('testdata/hold0.jsonld') as fh:
    holdtemplate = fh.read()

os.chdir("..")
os.system("./gradlew jar")
os.system("cp build/libs/export-3.0.0-alpha.jar ./integtest/export.jar")
os.chdir("integtest")
os.system("mkdir -p ./etc")

with open("./etc/config_xl.properties", "w") as fh:
    fh.write('OaiPmhBaseUrl="http://localhost:8080/oaipmh/"\n')
    fh.write('URIBase="{}"\n'.format(base_uri))

failedCases = []
    
########## TESTCASES ##########

# Normal new bib and hold should show up in export
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "SEK")
setModified("tttttttttttttttt", "2250-01-01 12:00:00")
setModified("hhhhhhhhhhhhhhhh", "2250-01-01 12:00:00")
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "bare_SEK")
assertExported("tttttttttttttttt", "Test 1")

# Only hold was updated bib should be exported
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "SEK")
setModified("tttttttttttttttt", "2150-01-01 12:00:00") # out of range
setModified("hhhhhhhhhhhhhhhh", "2250-01-01 12:00:00")
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "bare_SEK")
assertExported("tttttttttttttttt", "Test 2")


########## SUMMARY ##########

if not failedCases:
    print("*** ALL TESTS OK!")
else:
    print("*** THERE WERE FAILED TESTS:")
    for message in failedCases:
        print(message)
