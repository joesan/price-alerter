package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor._
import pureconfig.*

import scala.util.{Failure, Success, Try}


object PriceWatcherMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigSource.resources("product-source.conf").load[ProductRequests] match {
      case Right(conf) => conf
      case Left(errors) =>
        println("❌ Failed to load config:")
        errors.toList.foreach(println)
        sys.exit(1)
    }

    Try {
      val results = config.products.flatMap { entry =>
        println(s"🔍 Fetching: ${entry.shop} -> ${entry.url}")
        sniffPrice(entry.shop, entry.url).map(elem => elem.copy(isPriceReduced = elem.price < entry.alertBelow))
      }
      println("\n📊 Price Report:\n")
      printTable(results)
    } match {
      case Success(_) => println("\n✅ Price extraction completed successfully.")
      case Failure(ex) =>
        ex.printStackTrace()
        println(s"\n❌ Error during price extraction: ${ex.getMessage}")
    }
  }
}

