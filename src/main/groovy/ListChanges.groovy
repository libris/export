System.err.println "DEBUG: " + new Date()

import se.kb.libris.util.marc.*
import se.kb.libris.util.marc.io.*
import se.kb.libris.export.ExportProfile
import se.kb.libris.export.MergeRecords
import groovy.xml.XmlUtil

profile = new ExportProfile(new File("etc/export.properties"))
config = new ConfigSlurper().parse(new File("etc/config.properties").toURL())

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
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}"))
    } else {
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}&set=location:${location}"))
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
                System.err.println("# DELETED: " + id)
            } else {
              out = true
              ret.add(Integer.parseInt(id))
            }
          }
        }

        //if (!out) System.err.println("SKIPPED: " + id)
      }

      token = data.ListIdentifiers.resumptionToken.size() == 0? "":data.ListIdentifiers.resumptionToken.toString()
      System.err.println("DEBUG: resumptionToken=${token}")
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
    }
  }

  return ret
}

def listHoldIdentifiers(from, until) {
  def ret = new TreeSet<Integer>()

  for (String location: profile.getSet("locations")) {
    def data = null

    if (location.equals("*")) {
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}hold/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}"))
    } else {
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}hold/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}&set=location:${location}"))
    }

    def token = null
    while (token != "") {
      data.ListIdentifiers.header.each { header ->
        def id = header.identifier.toString().split('/')[-1]
        def deleted = false
        def bibid = ""

        header.setSpec.each { setSpec ->
          def set = setSpec.toString()
          if (set.startsWith("bibid:")) bibid = set.substring(6)

          if (header.@status.toString().equals("deleted")) {
            deleted = true
            System.err.println("# DELETED: " + id)
          }
        }

        if (!deleted) ret.add(Integer.parseInt(bibid))

        //if (!out) System.err.println("SKIPPED: " + id)
      }

      token = data.ListIdentifiers.resumptionToken.size() == 0? "":data.ListIdentifiers.resumptionToken.toString()
      System.err.println("DEBUG: resumptionToken=${token}")
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}hold/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
    }
  }

  return ret
}

def ids = new TreeSet<Integer>()

ids.addAll(listBibIdentifiers("2016-03-01T00:00:00Z", "2016-03-01T23:59:59Z"))
ids.addAll(listHoldIdentifiers("2016-03-01T00:00:00Z", "2016-03-01T23:59:59Z"))

for (Integer id: ids)
  println id
