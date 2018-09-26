# NEVER RUN THIS TEST CASE ON AN ENVIRONMENT WHERE THE DATA MATTERS!
# These tests are not portable.

import os
import json
import xml.etree.ElementTree as ET

base_uri = 'http://kblocalhost.kb.se:5000/'
export_url = 'http://localhost:8080/marc_export/'
    
## Util-stuff
    
def reset():
    os.system("psql whelk_dev -c \"delete from lddb__identifiers where id in (select id from lddb where changedIn = 'integtest');\"")
    os.system("psql whelk_dev -c \"delete from lddb__versions where id in (select id from lddb where changedIn = 'integtest');\"")
    os.system("psql whelk_dev -c \"delete from lddb__dependencies where id in (select id from lddb where changedIn = 'integtest');\"")
    os.system("psql whelk_dev -c \"delete from lddb where changedIn = 'integtest';\"")

def importBib(jsonstring, agent, systemid):
    jsonstring = jsonstring.replace("TEMPID", systemid)
    jsonstring = jsonstring.replace("TEMPBASEURI", base_uri)
    os.system("psql whelk_dev -c 'insert into lddb values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'bib', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__versions (id, data, collection, changedIn, changedBy, checksum) values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'bib', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid, '0', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid+'#it', '1', 'true'))

def importHold(jsonstring, agent, systemid, itemof, sigel):
    jsonstring = jsonstring.replace("TEMPID", systemid)
    jsonstring = jsonstring.replace("TEMPBASEURI", base_uri)
    jsonstring = jsonstring.replace("TEMPITEMOF", itemof)
    jsonstring = jsonstring.replace("TEMPSIGEL", sigel)
    os.system("psql whelk_dev -c 'insert into lddb values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'hold', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__versions (id, data, collection, changedIn, changedBy, checksum) values($${}$$, $${}$$, $${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, jsonstring, 'hold', 'integtest', agent, '0'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid, '0', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__identifiers (id, iri, graphIndex, mainId) values($${}$$, $${}$$, $${}$$, $${}$$);'".format(systemid, base_uri+systemid+'#it', '1', 'true'))
    os.system("psql whelk_dev -c 'insert into lddb__dependencies (id, relation, dependsOnId) values($${}$$, $${}$$, $${}$$);'".format(systemid, 'itemOf', itemof))

def updateRecord(agent, systemid, timestring):
    os.system("psql whelk_dev -c 'insert into lddb__versions (id, data, collection, changedIn, checksum, changedBy, modified) select id, data, collection, changedIn, checksum, $${}$$ as changedBy, $${}$$ as modified from lddb where id = $${}$$;'".format(agent, timestring, systemid))
    os.system("psql whelk_dev -c 'update lddb set modified = $${}$$, changedBy = $${}$$ where id = $${}$$;'".format(timestring, agent, systemid))

def setModified(systemid, timestring):
    os.system("psql whelk_dev -c 'update lddb set modified = $${}$$ where id = $${}$$;'".format(timestring, systemid))

def setDeleted(systemid):
    os.system("psql whelk_dev -c 'update lddb set deleted = true where id = $${}$$;'".format(systemid))

def doExport(fromTime, toTime, profileName):
    print('curl -XPOST "{}?from={}&until={}" --data-binary @./testdata/profiles/{}.properties > export.dump'.format(export_url, fromTime, toTime, profileName))
    os.system('curl -XPOST "{}?from={}&until={}" --data-binary @./testdata/profiles/{}.properties > export.dump'.format(export_url, fromTime, toTime, profileName))

def assertExported(record001, failureMessage):
    with open('export.dump') as fh:
        dump = fh.read()
    if not dump:
        failedCases.append(failureMessage)
        return
    xmlDump = ET.fromstring(dump)
    for elem in xmlDump.findall("{http://www.loc.gov/MARC21/slim}record/{http://www.loc.gov/MARC21/slim}controlfield[@tag='001']"):
        if elem.text == record001:
            return
    failedCases.append(failureMessage)

def assertNotExported(record001, failureMessage):
    with open('export.dump') as fh:
        dump = fh.read()
    if not dump:
        return
    xmlDump = ET.fromstring(dump)
    for elem in xmlDump.findall("{http://www.loc.gov/MARC21/slim}record/{http://www.loc.gov/MARC21/slim}controlfield[@tag='001']"):
        if elem.text == record001:
            failedCases.append(failureMessage)
            return
    return
    
## Init

with open('testdata/bib0.jsonld') as fh:
    bibtemplate = fh.read()
with open('testdata/hold0.jsonld') as fh:
    holdtemplate = fh.read()

failedCases = []
    
########## TESTCASES ##########

# Normal new bib and hold should show up in export
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "SEK")
updateRecord("SEK", "tttttttttttttttt", "2250-01-01 12:00:00")
updateRecord("SEK", "hhhhhhhhhhhhhhhh", "2250-01-01 12:00:00")
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

# Only bib was updated bib should be exported
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "SEK")
setModified("tttttttttttttttt", "2250-01-01 12:00:00")
setModified("hhhhhhhhhhhhhhhh", "2150-01-01 12:00:00") # out of range
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "bare_SEK")
assertExported("tttttttttttttttt", "Test 3")

# Updated bib without hold, should be exported when locations=*
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
setModified("tttttttttttttttt", "2250-01-01 12:00:00")
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "default_ALL")
assertExported("tttttttttttttttt", "Test 4")

# holdtype=none must not result in empty exports
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "SEK")
setModified("tttttttttttttttt", "2250-01-01 12:00:00")
setModified("hhhhhhhhhhhhhhhh", "2250-01-01 12:00:00")
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "hold_none_SEK")
assertExported("tttttttttttttttt", "Test 5")

# New bib with ony hold for other sigel, should not be exported
reset()
importBib(bibtemplate, "SEK", "tttttttttttttttt")
importHold(holdtemplate, "SEK", "hhhhhhhhhhhhhhhh", "tttttttttttttttt", "INTESEK")
setModified("tttttttttttttttt", "2250-01-01 12:00:00")
setModified("hhhhhhhhhhhhhhhh", "2250-01-01 12:00:00")
doExport("2250-01-01T11:00:00Z", "2250-01-01T15:00:00Z", "bare_SEK")
assertNotExported("tttttttttttttttt", "Test 6")


########## SUMMARY ##########

if not failedCases:
    print("*** ALL TESTS OK!")
else:
    print("*** THERE WERE FAILED TESTS:")
    for message in failedCases:
        print(message)
