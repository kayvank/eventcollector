package utils

import java.io.{File, FileOutputStream}
import java.net.URL
import java.util.zip.GZIPInputStream
import java.nio.channels.Channels
import akka.actor.ActorLogging
import scala.util.{Failure, Success, Try}

/**
  * Created by kayvan on 5/3/16.
  */

trait URLIO {
  this: ActorLogging =>

  def gunzip(url: String, path: String, file: String): Try[String] = {
    val gzipStream = Channels.newChannel(
      new GZIPInputStream(
        new URL(url).openStream))
    log.debug(s"attempting to download maxmind db into ${path + "/" + file}")

    val tryfos = Try(new FileOutputStream(path + "/" + file))
    tryfos.map(fos => {
      def getmyFile(offset: Long): Unit = {
        val res: Long = fos.getChannel.transferFrom(gzipStream, offset, Long.MaxValue)
        if (res > 0)
          getmyFile(res + offset)
        else {
          fos.close
          gzipStream.close
        }
      }
      getmyFile(0)
      path + "/" + file
    })
  }

  def downLoadMaxMinDB(path:String = Global.cfgVevo.getString("geoip.path"),
                       file: String = Global.cfgVevo.getString("geoip.file")): Try[String] = {
    val dir = new File(path)
    if (!dir.exists)
      dir.mkdirs
    val ret = gunzip(url = Global.cfgVevo.getString("geoip.url"),
      path = path, file = file)
    ret match {
      case Failure(e) =>
        log.error(s"maxmind download has failed with exception=${e}")
      case Success(s) =>
        log.info(s"mzxmind dowanloa completed to ${path}/${file}")
    }
    ret
  }
}
