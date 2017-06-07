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
    def ret = new TreeSet<String>()

    for (String location: profile.getSet("locations")) {
        def data = null

        if (location.equals("*")) {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml_expanded&from=${from}&until=${until}&set=bib"))
        } else {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml_expanded&from=${from}&until=${until}&set=bib:${location}"))
        }

        data.ListIdentifiers.header.each { header ->
            if (! header.@status.toString().equals("deleted"))
                ret.add(header.identifier.toString())
        }
    }

    System.err.println "DEBUG: found ${ret.size()} updated and/or created bib records"

    return ret
}

def listHoldIdentifiers(from, until) {
    def ret = new TreeSet<String>()

    for (String location: profile.getSet("locations")) {
        def data = null

        if (location.equals("*")) {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}&set=hold"))
        } else {
            data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}&set=hold:${location}"))
        }

        data.ListIdentifiers.about.each { about ->
            ret.add( about.itemOf.@id.toString() )
        }
    }

    System.err.println "DEBUG: found ${ret.size()} bib records with changes in related holding records"

    return ret
}

def getChangedRecords(from, until) {
    def ids = new TreeSet<String>()

    ids.addAll(listBibIdentifiers(from, until))
    //ids.addAll(listAuthIdentifiers(from, until)) // Included implicitly with the usage of *_expanded metadataPrefix
    ids.addAll(listHoldIdentifiers(from, until))

    return ids
}

def from = args[0], until = args.size()==2? args[1]:"2050-01-01T00:00:00Z"

System.err.println "DEBUG: from:${from} until:${until}"

for (String id: getChangedRecords(from, until))
    println id