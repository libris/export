import se.kb.libris.util.marc.*
import se.kb.libris.util.marc.io.*
import se.kb.libris.export.ExportProfile
import se.kb.libris.export.MergeRecords
import groovy.xml.XmlUtil

profile = new ExportProfile(new File("etc/export.properties"))
config = new ConfigSlurper().parse(new File("etc/config.properties").toURL())

def get(url) {
  //System.err.println "DEBUG: " + url
  def conn = url.toURL().openConnection()

  if (config.Password != "") {
    def authString  = "${config.User}:${config.Password}".getBytes().encodeBase64().toString()
    conn.setRequestProperty( "Authorization", "Basic ${authString}" )
  }

  return conn.content.text
}

def listAuthIdentifiers(from, until) {
  def ret = new TreeSet<Integer>()
  def data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}auth/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&from=${from}&until=${until}"))

  def token = null
  while (token != "") {
    data.ListIdentifiers.header.identifier.each { id ->
      def auth_id = id.toString().split('/')[-1]
      def data2 = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&metadataPrefix=marcxml&set=authority:${auth_id}"))

      def token2 = null
      while (token2 != "") {
        data2.ListIdentifiers.header.each { header ->
          def bib_id = id.toString().split('/')[-1]
          def out = false;

          header.setSpec.each { setSpec ->
            def set = setSpec.toString()
            if (!out && (set.startsWith("location:") && set.substring(9) in profile.getSet("locations") || "*" in profile.getSet("locations"))) {
              if (header.@status.toString().equals("deleted")) {
                  //System.err.println("# DELETED: " + id)
              } else {
                out = true
                ret.add(Integer.parseInt(bib_id))
              }
            }
          }
        }

        token2 = data2.ListIdentifiers.resumptionToken.size() == 0? "":data2.ListIdentifiers.resumptionToken.toString()

        if (!token2.equals("")) {
          //System.err.println("DEBUG: resumptionToken=${token}")
          data2 = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&resumptionToken=${token2}"))
        }
      }
    }

    token = data.ListRecords.resumptionToken.size() == 0? "":data.ListRecords.resumptionToken.toString()

    if (!token.equals("")) {
      //System.err.println("DEBUG: resumptionToken=${token}")
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}auth/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
    }
  }

  System.err.println "DEBUG: found ${ret.size()} bib records with changes in related auth records"

  return ret
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
                //System.err.println("# DELETED: " + id)
            } else {
              out = true
              ret.add(Integer.parseInt(id))
            }
          }
        }

        //if (!out) System.err.println("SKIPPED: " + id)
      }

      token = data.ListIdentifiers.resumptionToken.size() == 0? "":data.ListIdentifiers.resumptionToken.toString()
      //System.err.println("DEBUG: resumptionToken=${token}")
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}bib/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
    }
  }

  System.err.println "DEBUG: found ${ret.size()} updated and/or created bib records"

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
        }

        if (header.@status.toString().equals("deleted")) {
          deleted = true
        }

        if (header.@status.toString().equals("deleted")) {
          //System.err.println("DEBUG: DELETED location:${location} holding_id:${id} bib_id:${bibid}")
        } else {
          ret.add(Integer.parseInt(bibid))
        }
      }

      token = data.ListIdentifiers.resumptionToken.size() == 0? "":data.ListIdentifiers.resumptionToken.toString()
      //System.err.println("DEBUG: resumptionToken=${token}")
      data = new XmlSlurper(false, false).parseText(get("${config.OaiPmhBaseUrl}hold/oaipmh?verb=ListIdentifiers&resumptionToken=${token}"))
    }
  }

  System.err.println "DEBUG: found ${ret.size()} bib records with changes in related holding records"

  return ret
}

def from = args[0], until = args.size()==2? args[1]:"2050-01-01T00:00:00Z"
def ids = new TreeSet<Integer>()

System.err.println "DEBUG: from:${from} until:${until}"

ids.addAll(listBibIdentifiers(from, until))
ids.addAll(listAuthIdentifiers(from, until))
ids.addAll(listHoldIdentifiers(from, until))

for (Integer id: ids)
  println id
