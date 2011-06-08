package edu.duke.oit.vw.queue

import org.apache.activemq.broker.{BrokerService,TransportConnector}
import java.net.URI

import edu.duke.oit.vw.solr.VivoSolrIndexer

import akka.actor.Actor
import akka.util.Logging

object IndexUpdater { // }extends Logging {

  def start(vsi: Option[VivoSolrIndexer], hostname:String, port:Int, serviceId: String="VivoWidgets") = {
    Actor.remote.start(hostname, port) //Start the server
    Actor.remote.register(serviceId, Actor.actorOf(new IndexUpdater(vsi))) //Register the actor with the specified service id
  }

}

import edu.duke.oit.jena.utils._

case class UpdateMessage(uri:String, from:Option[String])


/**
 * Wraps the lift-json parsing and extraction of a person.
 */
object UpdateMessage {
  def apply(json:String) = {
    import net.liftweb.json._
    // Brings in default date formats etc.
    implicit val formats = DefaultFormats 

    val j = JsonParser.parse(json)
    j.extract[UpdateMessage]
  }
}


class IndexUpdater(vsi: Option[VivoSolrIndexer]=None) extends Actor with Logging {

  def this() = {
    this(None)
  }

  def receive = {
    case msg:String => {
      log.debug(">> received message")
      val msgString = msg // .bodyAs[String]
      log.debug(">> msgString: " +msgString)
      val updateMessage = UpdateMessage(msgString)

      log.debug(">> reindex: " + updateMessage.uri)
      
      import edu.duke.oit.vw.solr.VivoSolrIndexer
      import edu.duke.oit.vw.scalatra.WidgetsConfig
      
      val vsi = new VivoSolrIndexer(WidgetsConfig.server, WidgetsConfig.widgetServer)  
      vsi.reindexUri(updateMessage.uri)
      log.debug(">> finished reindexing " + updateMessage.uri)
    }
    case _ => { 
      println(">> no message!!")
    }
  }

}