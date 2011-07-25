package edu.duke.oit.vw.solr.test

import org.specs._
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList

import edu.duke.oit.vw.solr._
import edu.duke.oit.vw.scalatra.ScalateTemplateStringify
import edu.duke.oit.test.helpers.{TestServers,SampleLoader}

class VivoSolrIndexerSpec extends Specification with SampleLoader with ScalateTemplateStringify {

  val vivo = TestServers.vivo
  val solrSrv = TestServers.widgetSolr

  "A Vivo Solr Indexer" should {

    solrSrv.deleteByQuery("*:*")

    val vsi = new VivoSolrIndexer(vivo, solrSrv)

    "be connected to a vivo server with more than 0 people" in {
      // guard against running tests with bad db
      vivo.numPeople() must be_> (0)
    } tag ("focus")

    "create a document in the index for each person in vivo with their uri as the id and a json serialization in the 'json' field" in {
      vsi.indexPeople()
      val people = vivo.select(renderFromClassPath("sparql/facultyMember.ssp"))
      for (p <- people) {
        val uri = p('person).toString.replaceAll("<|>","")
        val query = new SolrQuery().setQuery("id:\"" + uri + "\"")
        val personDocs = solrSrv.query(query).getResults()
        personDocs.getNumFound() must_== 1

        val json = personDocs.iterator.next.get("json").toString
        val person = PersonExtraction(json)
        person.uri must_== uri
      }
    } tag ("focus")

    "update the indexed document for an individual" in {
      vsi.indexPeople()
      val people = vivo.select(renderFromClassPath("sparql/facultyMember.ssp"))

      val uri = people.head('person).toString.replaceAll("<|>","")
      vsi.reindexPerson(uri)
      val person = vsi.getPerson(uri)
      // println(">>>> person: " + person)
    } tag ("focus")

  } tag("focus")


}