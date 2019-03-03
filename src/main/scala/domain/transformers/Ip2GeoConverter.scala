package domain.transformers

import com.snowplowanalytics.maxmind.iplookups.IpLocation
import domain.Domain
import domain.Domain.GeoIp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by kayvan on 11/5/16.
  */

object Ip2GeoConverter {

  def nulGeo(ip: String) = GeoIp(ip_address = ip,
    lat = 0F,
    lng = 0F,
    city = "",
    region = "",
    postal_code = "",
    country = "")

  val geoIpFactory: (String, IpLocation) => GeoIp =
    (ip, ipLocation) => {
      Domain.GeoIp(
        ip_address = ip,
        lat = ipLocation.latitude,
        lng = ipLocation.longitude,
        city = ipLocation.city.getOrElse(""),
        region = ipLocation.region.getOrElse(""),
        postal_code = ipLocation.postalCode.getOrElse(""),
        country = ipLocation.countryCode)
    }

  val ip2futureGeo: String => (String => Future[Option[IpLocation]]) => Future[GeoIp] = {
    ip => ip2ipLookup => ip2futureGeoFunc(ip, ip2ipLookup)
  }

  private def ip2futureGeoFunc(ip: String, ip2ipLo: String => Future[Option[IpLocation]]): Future[GeoIp] = {
    (
      for {
        c <- ip2ipLo(ip)
        g = c.map(iploc => Ip2GeoConverter.geoIpFactory(ip, iploc))
      } yield (g)
      ) map( _.getOrElse(nulGeo(ip)) )
  }
}
