package domain

import com.snowplowanalytics.maxmind.iplookups.{IpLocation, IpLookups, _}
import utils.Global._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by kayvan on 11/9/16.
  */
class MaxMindGeoDb(maxmindGeoDbFile: String =
s"${cfgVevo.getString("geoip.path")}/${cfgVevo.getString("geoip.file")}") {

  import spray.caching.{Cache, LruCache}
  import scala.concurrent.duration._

  lazy private val cache: Cache[IpLookupResult] =
    LruCache(maxCapacity = 5000, timeToLive = 1 hour)

  lazy private val ipLookups: IpLookups = IpLookups(
    geoFile = Some(maxmindGeoDbFile),
    ispFile = None,
    orgFile = None,
    domainFile = None,
    memCache = false,
    lruCache = 0)

  private def getIpInfo(ipAddr: String) = cache(ipAddr) {
    ipLookups.performLookups(ipAddr)
  }

  val ip2ipLocation: String => Future[Option[IpLocation]] =
    ip => getIpInfo(ip).map(x => x._1)
}
