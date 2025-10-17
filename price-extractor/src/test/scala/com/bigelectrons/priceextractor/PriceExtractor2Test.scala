package com.bigelectrons.priceextractor

import org.scalatest.funsuite.AnyFunSuite

final class PriceExtractor2Test extends AnyFunSuite {

  test("sniff for price - Bike Discount Shimano GRX RX820/RX610") {
    PriceExtractor2.sniffPrice("https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc") match {
      case Some(info) =>
        println(s"✅ [${info.title}] — €${info.price} (${info.source})")
        assert(info.price > BigDecimal(0))
      case None =>
        fail("❌ Could not extract title or price.")
    }
  }

  test("sniff for price - Bike-24 Shimano GRX RX610") {
    PriceExtractor2.sniffPrice("https://www.bike24.com/p2790652.html") match {
      case Some(info) =>
        println(s"✅ [${info.title}] — €${info.price} (${info.source})")
        assert(info.price > BigDecimal(0))
      case None =>
        fail("❌ Could not extract title or price.")
    }
  }
}
