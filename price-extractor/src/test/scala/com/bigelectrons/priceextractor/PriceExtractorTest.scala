package com.bigelectrons.priceextractor

import org.scalatest.funsuite.AnyFunSuite

final class PriceExtractorTest extends AnyFunSuite {

  val listOfShops: Map[String, String] = Map(
    "BIKE24" -> "https://www.bike24.com/p2790652.html",
    "Bike-Discount" -> "https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc"
  )

  test("sniff for price - Bike Discount Shimano GRX RX820/RX610") {
    PriceExtractor.sniffPrice("Bike Discount", "https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc") match {
      case Some(info) =>
        println(s"✅ [${info.title}] — €${info.price} (${info.sourceHTMLSelector})")
        assert(info.price > BigDecimal(0))
      case None =>
        fail("❌ Could not extract title or price.")
    }
  }

  test("sniff for price - Bike-24 Shimano GRX RX610") {
    PriceExtractor.sniffPrice("Bike24", "https://www.bike24.com/p2790652.html") match {
      case Some(info) =>
        println(s"✅ [${info.title}] — €${info.price} (${info.sourceHTMLSelector})")
        assert(info.price > BigDecimal(0))
      case None =>
        fail("❌ Could not extract title or price.")
    }
  }

  test("sniff for price - BIKE24, Bike-Discount Shimano GRX RX610") {
    val results = listOfShops.map { entry =>
      val (shop, url) = entry
      PriceExtractor.sniffPrice(shop, url) 
    }
    printTable(results.flatten.toSeq)
  }
}
