//package domain.transformer
//
//import com.typesafe.scalalogging.LazyLogging
//import domain.MaxMindGeoDb
//import domain.transformers.Ip2GeoConverter
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//
///**
//  * Created by kayvan on 11/5/16.
//  */
//class Ip2GeoSpecifications extends org.specs2.mutable.Specification
//  with LazyLogging {
//
//  "Specifications for IP to GeoLocation computations".title
//
//  "valid GeoLocation is computed for a valid IP address" >> {
//    val maxMind = new MaxMindGeoDb()
//    val testIpAddr = "204.28.115.197"
//    val computed = Await.result(
//      Ip2GeoConverter.ip2futureGeo(testIpAddr)(maxMind.ip2ipLocation),
//      Duration("5 second"))
//    logger.info(s"computed geoLocation = ${computed}")
//    computed.ip_address === testIpAddr
//  }
//
//  "empty GeoLocation is computed for a invalid IP address" >> {
//    val maxMind = new MaxMindGeoDb()
//    val testIpAddr = "0.0.0.0"
//    val computed = Await.result(
//      Ip2GeoConverter.ip2futureGeo(testIpAddr)(maxMind.ip2ipLocation),
//      Duration("5 second"))
//    logger.info(s"computed geoLocation = ${computed}")
//    computed.ip_address === testIpAddr
//  }
//}
