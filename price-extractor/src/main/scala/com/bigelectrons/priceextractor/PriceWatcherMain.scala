package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor._

import scala.util.{Failure, Success, Try}


object PriceWatcherMain {

  def main(args: Array[String]): Unit = Try {
      val results = loadProductRequests.products.flatMap { entry =>
        println(s"🔍 Fetching: ${entry.shop} -> ${entry.url}")
        sniffPrice(entry).map(elem => elem.copy(isPriceReduced = elem.price < entry.alertBelow))
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

