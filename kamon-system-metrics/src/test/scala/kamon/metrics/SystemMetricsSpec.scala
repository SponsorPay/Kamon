/* =========================================================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.metric

import java.lang.management.ManagementFactory

import com.typesafe.config.ConfigFactory
import kamon.system.jmx.GarbageCollectionMetrics
import kamon.testkit.BaseKamonSpec
import scala.collection.JavaConverters._

class SystemMetricsSpec extends BaseKamonSpec("system-metrics-spec") with RedirectLogging {

  override lazy val config =
    ConfigFactory.parseString(
      """
        |kamon.metric {
        |  tick-interval = 1 hour
        |}
        |
        |akka {
        |  extensions = ["kamon.system.SystemMetrics"]
        |}
      """.stripMargin)

  override protected def beforeAll(): Unit =
    Thread.sleep(2000) // Give some room to the recorders to store some values.

  "the Kamon System Metrics module" should {
    "record count and time garbage collection metrics" in {
      val availableGarbageCollectors = ManagementFactory.getGarbageCollectorMXBeans.asScala.filter(_.isValid)

      for (collectorName ← availableGarbageCollectors) {
        val sanitizedName = GarbageCollectionMetrics.sanitizeCollectorName(collectorName.getName)
        val collectorMetrics = takeSnapshotOf(s"$sanitizedName-garbage-collector", "system-metric")

        collectorMetrics.gauge("garbage-collection-count").get.numberOfMeasurements should be > 0L
        collectorMetrics.gauge("garbage-collection-time").get.numberOfMeasurements should be > 0L
      }
    }

    "record used, max and committed heap metrics" in {
      val heapMetrics = takeSnapshotOf("heap-memory", "system-metric")

      heapMetrics.gauge("heap-used").get.numberOfMeasurements should be > 0L
      heapMetrics.gauge("heap-max").get.numberOfMeasurements should be > 0L
      heapMetrics.gauge("heap-committed").get.numberOfMeasurements should be > 0L
    }

    "record used, max and committed non-heap metrics" in {
      val nonHeapMetrics = takeSnapshotOf("non-heap-memory", "system-metric")

      nonHeapMetrics.gauge("non-heap-used").get.numberOfMeasurements should be > 0L
      nonHeapMetrics.gauge("non-heap-max").get.numberOfMeasurements should be > 0L
      nonHeapMetrics.gauge("non-heap-committed").get.numberOfMeasurements should be > 0L
    }

    "record daemon, count and peak jvm threads metrics" in {
      val threadsMetrics = takeSnapshotOf("threads", "system-metric")

      threadsMetrics.gauge("daemon-thread-count").get.numberOfMeasurements should be > 0L
      threadsMetrics.gauge("peak-thread-count").get.numberOfMeasurements should be > 0L
      threadsMetrics.gauge("thread-count").get.numberOfMeasurements should be > 0L
    }

    "record loaded, unloaded and current class loading metrics" in {
      val classLoadingMetrics = takeSnapshotOf("class-loading", "system-metric")

      classLoadingMetrics.gauge("classes-loaded").get.numberOfMeasurements should be > 0L
      classLoadingMetrics.gauge("classes-unloaded").get.numberOfMeasurements should be > 0L
      classLoadingMetrics.gauge("classes-currently-loaded").get.numberOfMeasurements should be > 0L
    }
  }

  def isLinux: Boolean =
    System.getProperty("os.name").indexOf("Linux") != -1

}
