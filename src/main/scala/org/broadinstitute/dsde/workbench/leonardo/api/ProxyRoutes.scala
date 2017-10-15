package org.broadinstitute.dsde.workbench.leonardo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.workbench.leonardo.model.{ClusterName, GoogleProject}
import org.broadinstitute.dsde.workbench.leonardo.service.ProxyService

/**
  * Created by rtitle on 8/4/17.
  */
trait ProxyRoutes { self: LazyLogging =>
  val proxyService: ProxyService
  protected val tokenCookieName = "FCToken"
  private val config = ConfigFactory.parseResources("leonardo.conf").withFallback(ConfigFactory.load())
  private val whitelist = config.as[(List[String])]("whitelist")

  protected val proxyRoutes: Route = getUserEmailForCookieAuthed() { userEmail =>
    if (whitelist.contains(userEmail)) {
      pathPrefix("notebooks" / Segment / Segment) { (googleProject, clusterName) =>
        extractRequest { request =>
          cookie(tokenCookieName) { tokenCookie => // rejected with MissingCookieRejection if the cookie is not present
            complete {
              // Proxy logic handled by the ProxyService class
              proxyService.proxy(GoogleProject(googleProject), ClusterName(clusterName), request, tokenCookie)
            }
          }
        }
      }
    } else throw AuthorizationError(userEmail)
  }



  def getUserEmailForCookieAuthed(): Directive1[String]= {
    def accessTokenHeaderDirective: Directive1[String] = headerValueByName("OIDC_access_token")
    for {
      accessToken <- accessTokenHeaderDirective
    } yield proxyService.getCachedEmailFromToken(accessToken)
  }

}
