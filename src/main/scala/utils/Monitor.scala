package utils

import kamon.Kamon

object Monitor {

  object Counter {
    val endoValid = Kamon.metrics.counter("endo.valid")
    val endoInvalid = Kamon.metrics.counter("endo.invalid")
    val endoBatch = Kamon.metrics.counter("endo.batch")
    val http200 = Kamon.metrics.counter("http200")
    val http400 = Kamon.metrics.counter("http400")
    val geoipLookup = Kamon.metrics.counter("geoip.lookup")
    val geoipDownload=Kamon.metrics.counter("geoip.db.download")
    val geoipDownloadFailure=Kamon.metrics.counter("geoip.failed.db.download")
  }

  def invalid = {
    Counter.http400.increment()
    Counter.endoInvalid.increment()
  }
  def valid = {
    Counter.http200.increment()
    Counter.endoValid.increment()
    Counter.geoipLookup.increment()
  }
  def batch = {
    Counter.endoBatch.increment()
    Counter.http200.increment()
  }
  def geoipDownload = Counter.geoipDownload.increment()
  def geoipDownloadFailure = Counter.geoipDownloadFailure.increment()
}
