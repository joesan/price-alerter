package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor._

import cats.effect.{IO, IOApp}
import cats.implicits._


object PriceWatcherMain extends IOApp.Simple {

  private def sniffPriceIO(entry: ProductRequest): IO[Option[ProductInfo]] = IO {
    println(s"🔍 Fetching: ${entry.shop} -> ${entry.url}")
    sniffPrice(entry).map { product =>
      product.copy(isPriceReduced = product.price < entry.alertBelow)
    }
  }

  def run: IO[Unit] = {
    val startTime = System.currentTimeMillis()
    val productList = loadProductRequests.products

    for {
      results <- productList.parTraverse(sniffPriceIO) // run in parallel
      validResults = results.flatten
      _ <- IO {
        println("\n📊 Price Report:\n")
        printTable(validResults)
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        println(s"\n⏱️  Completed in $duration seconds.")
        println("\n✅ Price extraction completed successfully.")
      }
    } yield ()
  }
}
