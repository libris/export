System.err.println "DEBUG: " + new Date()
System.err.println "DEBUG: " + System.getProperty("user.dir")

import se.kb.libris.util.marc.*
import se.kb.libris.util.marc.io.*
import se.kb.libris.export.ExportProfile
import se.kb.libris.export.MergeRecords
import groovy.xml.XmlUtil

profile = new ExportProfile(new File("etc/export.properties"))
config = new ConfigSlurper().parse(new File("etc/config.properties").toURL())

def toXml(node) {
  return XmlUtil.serialize(node)
}

def get(url) {
  def conn = url.toURL().openConnection()

  if (config.Password != "") {
    def authString  = "${config.User}:${config.Password}".getBytes().encodeBase64().toString()
    conn.setRequestProperty( "Authorization", "Basic ${authString}" )
  }

  return conn.content.text
}

def getRecord(type, id) {
  def uri = "http://libris.kb.se/resource/${type}/${id}"
  def url = "${config.OaiPmhBaseUrl}${type}/oaipmh?verb=GetRecord&metadataPrefix=marcxml&identifier=${uri}"

  return xml = new XmlSlurper(false, false).parseText(get(url)).GetRecord.record
}

// assumes to get all records from one request, which is naive in the general case but works in this specific one
def listRecords(type, set) {
  def url = "${config.OaiPmhBaseUrl}${type}/oaipmh?verb=ListRecords&metadataPrefix=marcxml&set=${set}"

  return new XmlSlurper(false, false).parseText(get(url)).ListRecords.record.metadata.record
}

def getHoldings(bib_id) {
  return listOaiPmhRecord("hold", "bibid${bib_id}")
}

def getMerged(bib_id) {
  // Step 1 - get bib record
  def record = getRecord("bib", bib_id)
  def auth_ids = record.header.setSpec.findAll({ x -> x.toString().startsWith("authority") }).collect { x -> x.toString() }

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
  def auths = new HashSet<MarcRecord>()
  if (!profile.getProperty("authtype", "NONE").equalsIgnoreCase("NONE")) {
    auth_ids.each { auth_id ->
      auths.add(MarcXmlRecordReader.fromXml(toXml(getRecord("auth", auth_id.toString().substring(10)).metadata.record)))
    }
  }

  // Step 3 - get holdings records
  def holdings = new TreeMap<String, MarcRecord>()
  if (!profile.getProperty("holdtype", "NONE").equalsIgnoreCase("NONE")) {
    listRecords("hold", "bibid:${bib_id}").each { x->
      def mr = MarcXmlRecordReader.fromXml(toXml(x))
      holdings.put(mr.iterator("852").next().iterator("b").next().getData(), mr)
    }
  }

  return profile.mergeRecord(bib, holdings, auths)
  //return [ auths, MarcXmlRecordReader.fromXml(bib), holdings ]
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

