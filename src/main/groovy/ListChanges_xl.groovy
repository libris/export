import se.kb.libris.export.ExportProfile

profile = new ExportProfile(new File("etc/export.properties"))
config = new ConfigSlurper().parse(new File("etc/config_xl.properties").toURL())

def get(url) {
    def conn = url.toURL().openConnection()

    if (config.Password != "") {
        def authString  = "${config.User}:${config.Password}".getBytes().encodeBase64().toString()
        conn.setRequestProperty( "Authorization", "Basic ${authString}" )
    }

    return conn.content.text
}


def listBibIdentifiers(from, until) {
    def ret = new TreeSet<Integer>()

    for (String location: profile.getSet("locations")) {
        def data = null

        if (location.equals("*")) {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml_expanded&from=${from}&until=${until}"))
        } else {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml_expanded&from=${from}&until=${until}&set=bib:${location}"))
        }

        def token = null
        while (token != "") {
            data.ListIdentifiers.header.each { header ->
                def id = header.identifier.toString().split('/')[-1]
                def out = false;

                header.setSpec.each { setSpec ->
                    def set = setSpec.toString()
                    if (!out && (set.startsWith("location:") && set.substring(9) in profile.getSet("locations") || "*" in profile.getSet("locations"))) {
                        if (header.@status.toString().equals("deleted")) {
                            //System.err.println("# DELETED: " + id)
                        } else {
                            out = true
                            ret.add(config.URIBase + id)
                        }
                    }
                }
            }

            token = data.ListIdentifiers.resumptionToken.size() == 0? "":data.ListIdentifiers.resumptionToken.toString()
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
        }
    }

    System.err.println "DEBUG: found ${ret.size()} updated and/or created bib records"

    return ret
}

def getChangedRecords(from, until) {
    def ids = new TreeSet<Integer>()

    ids.addAll(listBibIdentifiers(from, until))
    //ids.addAll(listAuthIdentifiers(from, until)) // Included implicitly with the usage of *_expanded metadataPrefix
    //ids.addAll(listHoldIdentifiers(from, until)) // Included implicitly with the usage of bib:[location] sets (defined as bibs with holdings owned by [location])

    return ids
}

def from = args[0], until = args.size()==2? args[1]:"2050-01-01T00:00:00Z"

System.err.println "DEBUG: from:${from} until:${until}"

for (String id: getChangedRecords(from, until))
    println id