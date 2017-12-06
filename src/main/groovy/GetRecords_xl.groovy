System.err.println "DEBUG: " + new Date()
System.err.println "DEBUG: " + System.getProperty("user.dir")

import se.kb.libris.util.marc.*
import se.kb.libris.util.marc.io.*
import se.kb.libris.export.ExportProfile
import se.kb.libris.export.MergeRecords
import groovy.xml.XmlUtil

profile = new ExportProfile(new File("etc/export.properties"))
config = new ConfigSlurper().parse(new File("etc/config_xl.properties").toURL())

// Tolerate both trailing slash and lack thereof
if (!config.OaiPmhBaseUrl.endsWith("/"))
  config.OaiPmhBaseUrl = config.OaiPmhBaseUrl + "/"
if (!config.URIBase.endsWith("/"))
  config.URIBase = config.URIBase + "/"

def toXml(node) {
  return XmlUtil.serialize(node)
}

def get(url) {
  def conn = url.toURL().openConnection()

  if (config.Password != "") {
    def authString  = "${config.User}:${config.Password}".getBytes().encodeBase64().toString()
    conn.setRequestProperty( "Authorization", "Basic ${authString}" )
  }
  byte[] body = conn.getInputStream().getBytes();
  return new String(body, "UTF-8")
}

def getRecord(id) {
  id = java.net.URLEncoder.encode(id, "UTF-8")
  def url = "${config.OaiPmhBaseUrl}?verb=GetRecord&metadataPrefix=marcxml_includehold_expanded&identifier=${id}"
  return xml = new XmlSlurper(false, false).parseText(get(url)).GetRecord.record
}

def getMerged(bib_id) {
  // Step 1 - get bib record
  def record = getRecord(bib_id)

  if (record.metadata.record.size() == 0) {
      System.err.println("WARNING - NO SUCH BIB_ID: " + bib_id);
      return []
  }

  def bib = MarcXmlRecordReader.fromXml(toXml(record.metadata.record))

  // filter out license or e-record?
  if (profile.filter(bib)) {
    System.err.println("FILTERED: " + MergeRecords.format(bib));
    return []
  } else {
    System.err.println(MergeRecords.format(bib));
  }

  // Step 2 - find and get authority records
  def auth_ids = []
  record.metadata.record.datafield.subfield.each {
    if (it.@code.text().equals("0") && (it.text().startsWith("https://id.kb.se/") || it.text().startsWith("https://libris.kb.se/"))) {
      auth_ids.add(it.text())
    }
  }

  def auths = new HashSet<MarcRecord>()
  if (!profile.getProperty("authtype", "NONE").equalsIgnoreCase("NONE")) {
    auth_ids.each { auth_id ->
      getRecord(auth_id).metadata.record.each {
        auths.add(MarcXmlRecordReader.fromXml(toXml(it)))
      }
    }
  }

  // Step 3 - get holdings records
  def holdings = new TreeMap<String, MarcRecord>()
  if (!profile.getProperty("holdtype", "NONE").equalsIgnoreCase("NONE")) {
    record.about.holding.each { holding ->
      holdings.put(holding.@sigel.toString(), MarcXmlRecordReader.fromXml(toXml(getRecord("${config.URIBase}" + holding.@id.toString()).metadata.record)))
    }
  }

  return profile.mergeRecord(bib, holdings, auths)
}

def writer = (profile.getProperty("format", "ISO2709").equalsIgnoreCase("MARCXML"))? new MarcXmlRecordWriter(System.out, profile.getProperty("characterencoding")):new Iso2709MarcRecordWriter(System.out, profile.getProperty("characterencoding"))

System.in.eachLine() { line ->
  if (line.trim() != "") {
    getMerged(line).each {
      record -> writer.writeRecord(record)
    }
  }
}

writer.close()
