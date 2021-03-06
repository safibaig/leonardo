package org.broadinstitute.dsde.workbench.leonardo.monitor

import akka.actor.{ActorRef, Props}
import akka.testkit.TestKit
import org.broadinstitute.dsde.workbench.leonardo.config.MonitorConfig
import org.broadinstitute.dsde.workbench.leonardo.dao.DataprocDAO
import org.broadinstitute.dsde.workbench.leonardo.db.DbReference
import org.broadinstitute.dsde.workbench.leonardo.model.Cluster

import scala.concurrent.duration._

object TestClusterSupervisorActor {
  def props(gdDAO: DataprocDAO, dbRef: DbReference, testKit: TestKit): Props =
    Props(new TestClusterSupervisorActor(gdDAO, dbRef, testKit))
}

/**
  * Extends ClusterMonitorSupervisor so the akka TestKit can watch the child ClusterMontitorActor's.
  */
class TestClusterSupervisorActor(gdDAO: DataprocDAO, dbRef: DbReference, testKit: TestKit) extends ClusterMonitorSupervisor(MonitorConfig(100 millis), gdDAO, dbRef) {
  override def createChildActor(cluster: Cluster): ActorRef = {
    val child = super.createChildActor(cluster)
    testKit watch child
    child
  }
}
