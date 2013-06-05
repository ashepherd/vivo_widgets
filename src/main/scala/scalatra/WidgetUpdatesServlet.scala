package edu.duke.oit.vw.scalatra

import akka.actor.Actor._
import akka.actor.{Actor,ActorSystem,Props}

import edu.duke.oit.vw.utils.{ElvisOperator,Json,Int,WidgetLogging,Timer}
import edu.duke.oit.vw.solr.VivoSolrIndexer
import edu.duke.oit.vw.solr.VivoSearcher
import edu.duke.oit.vw.models.Person
import edu.duke.oit.vw.jena.JenaCache
import java.net.URL
import org.scalatra._
import scalate.ScalateSupport
import edu.duke.oit.vw.scalatra.BasicAuth.AuthenticationSupport
import edu.duke.oit.vw.queue._

class WidgetUpdatesFilter extends ScalatraFilter 
  with AuthenticationSupport 
  with WidgetLogging 
  with Timer {
  
  post("/updates/rebuild/index") {
    basicAuth
    log.info("rebuilding the index...")
    WidgetsConfig.prepareCore
    val vsi = new VivoSolrIndexer(WidgetsConfig.server, WidgetsConfig.widgetServer)
    vsi.indexAll()
    Json.toJson(Map("complete" -> true))
  }

 post("/updates/rebuild/person") {
    basicAuth
    log.info("rebuilding the person index...")
    params.get("uri") match {
      case Some(uri:String) => {
        log.info("rebuilding for: " + uri)
        WidgetsConfig.prepareCore
        val vsi = new VivoSolrIndexer(WidgetsConfig.server, WidgetsConfig.widgetServer)
        vsi.indexPerson(uri)
        Json.toJson(Map("complete" -> true))
      }
      case _ => "Not a valid request"
    }

  }

  post("/updates/uri") {
    basicAuth
    WidgetsConfig.prepareCore
    params.get("message") match {
      case Some(message:String) => {
        val system =  JenaCache.system
        val indexUpdater = system.actorOf(Props[IndexUpdater], name = "indexupdater")
        indexUpdater ! message
        Json.toJson(Map("message" -> "Sent to Indexupdater"))
      }
      case _ => "Not a valid request"
    }
  }
  
  get("/updates/test") {
    "test---"
  }

  notFound {
    filterChain.doFilter(request, response)
  }
  
}
