package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor.{extractFromRegex, sniffPrice}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.*

import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.matching.Regex


final class PriceExtractorTest extends AnyFunSuite {

  test("extract single EUR value with € prefix") {
    val html = "<p>This item costs €1,234.56</p>"
    val doc = Jsoup.parse(html)
    val result = extractFromRegex(doc)

    assert(result.nonEmpty)
    assert(result.head._1 == BigDecimal("1234.56"))
    assert(result.head._2.contains("regex"))
  }

  test("extract single EUR value with € suffix") {
    val html = "<p>This item costs 829.00€</p>"
    val doc = Jsoup.parse(html)
    val result = extractFromRegex(doc)

    assert(result.nonEmpty)
    assert(result.head._1 == BigDecimal("829.00"))
    assert(result.head._2.contains("regex"))
  }

  test("extract multiple EUR values with and without EUR/€") {
    val html =
      """
        |<div>
        |  Price: 1.999,99 EUR
        |  Another: € 2.345,00
        |  And again: EUR 3,210.50
        |</div>
        |""".stripMargin
    val doc = Jsoup.parse(html)
    val result = extractFromRegex(doc)

    assert(result.length == 3)
    assert(result.map(_._2).forall(_.contains("regex")))
  }

  test("ignore invalid number formats") {
    val html = "<span>Cost: €abc</span>"
    val doc = Jsoup.parse(html)
    val result = extractFromRegex(doc)

    assert(result.isEmpty)
  }

  test("handle empty document") {
    val doc = Jsoup.parse("")
    val result = extractFromRegex(doc)

    assert(result.isEmpty)
  }

  // *******************************************************************************************************************
  // *Sniffer tests*
  // *******************************************************************************************************************

  test("sniffPriceFromDoc extracts price from HTML") {
    val result = sniffPrice("https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc")
    println(result)
  }
}
