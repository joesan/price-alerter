package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor.sniffPrice
import org.scalatest.funsuite.AnyFunSuite

final class PriceExtractorTest extends AnyFunSuite {

  private val listOfShops: ProductRequests = loadProductRequests

  test("sniff for price for all shops configured in product-source.conf") {
    val results = listOfShops.products.collect {
      case entry if entry.url.toLowerCase.contains("bike24") =>
        println(s"✅ Successfully extracted price for: ${entry.shop}")
        sniffPrice(entry)
    }
    printTable(results.flatten)
  }
}
