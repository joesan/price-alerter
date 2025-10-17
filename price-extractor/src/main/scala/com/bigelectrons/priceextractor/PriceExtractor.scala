package com.bigelectrons.priceextractor

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.libs.json._
import scala.jdk.CollectionConverters._
import scala.util.Try

import scala.util.matching.Regex


object PriceExtractor {

  // Common CSS selectors to try
  val commonPriceSelectors = Seq(
    ".price", ".product-price", ".price--main", ".price-value",
    ".current-price", ".price-amount", "#price", ".price-box"
  )

  // Regex to catch euro / number patterns (very basic)
  // E.g. matches "€ 1.234,56" or "1.234,56 €" or "EUR 1.234,56"
  val euroRegex: Regex = """(?i)(?:€|\bEUR\b)?\s*([0-9]{1,3}(?:[.,\s][0-9]{3})*(?:[.,][0-9]{1,2})?)\s*(?:€|\bEUR\b)?""".r

  def normalizeNumber(raw: String): Option[BigDecimal] = {
    val s0 = raw.trim.replace("\u00A0", "").replace(" ", "")
    val s = if (s0.contains(",") && s0.contains(".")) {
      if (s0.lastIndexOf(',') > s0.lastIndexOf('.')) {
        s0.replace(".", "").replace(",", ".")
      } else {
        s0.replace(",", "")
      }
    } else if (s0.contains(",")) {
      s0.replace(".", "").replace(",", ".")
    } else {
      s0.replace(",", "")
    }

    Try(BigDecimal(s)).toOption
  }

  def extractFromLdJson(doc: Document): Seq[(BigDecimal, String)] = {
    val scripts = doc.select("script[type=application/ld+json]").asScala

    scripts.flatMap { script =>
      val jsonText = script.html()
      Try(Json.parse(jsonText)).toOption match {
        case Some(jsValue) =>
          val offers = (jsValue \ "offers").toOption
          offers match {
            case Some(offerJsValue) =>
              val prices = offerJsValue match {
                case JsArray(arr) =>
                  arr.flatMap { offer =>
                    (offer \ "price").asOpt[String]
                  }
                case JsObject(_) =>
                  (offerJsValue \ "price").asOpt[String].toSeq
                case _ => Seq.empty
              }
              prices.flatMap(p => normalizeNumber(p).map((_, "ld+json price via Play JSON")))
            case None => Seq.empty
          }
        case None => Seq.empty
      }
    }.toSeq
  }

  def extractFromMeta(doc: Document): Seq[(BigDecimal, String)] = {
    val selectors = Seq(
      ("meta[property=product:price:amount]", "product:price"),
      ("meta[property=og:price:amount]", "og:price"),
      ("meta[name=price]", "meta price"),
      ("[itemprop=price]", "itemprop")
    )

    selectors.flatMap { case (selector, hint) =>
      val el = doc.select(selector).first()
      if (el != null) {
        val content = Option(el.attr("content")).filter(_.nonEmpty).getOrElse(el.text())
        normalizeNumber(content).map((_, s"meta tag $hint"))
      } else None
    }
  }

  def extractFromCss(doc: Document): Seq[(BigDecimal, String)] = {
    commonPriceSelectors.flatMap { selector =>
      val el = doc.select(selector).first()
      if (el != null) {
        val text = el.text()
        normalizeNumber(text).map((_, s"CSS selector $selector"))
      } else None
    }
  }

  def extractFromRegex(doc: Document): Seq[(BigDecimal, String)] = {
    val text = doc.text()
    euroRegex.findAllMatchIn(text).flatMap { m =>
      val numStr = m.group(1)
      normalizeNumber(numStr).map((_, s"regex '${m.matched}'"))
    }.toSeq
  }

  def sniffPrice(url: String): Option[(BigDecimal, String)] = {
    println(s"Fetching $url...")
    val doc = Jsoup.connect(url)  
      .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
      .header("Accept‑Language", "en‑US,en;q=0.9")
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .referrer("https://www.bike-discount.de/")
      .timeout(15000).userAgent("Mozilla/5.0")
      .get()

    val candidates = extractFromLdJson(doc) ++
      extractFromMeta(doc) ++
      extractFromCss(doc) ++
      extractFromRegex(doc)

    candidates.sortBy(-_._1).headOption
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: PriceSniffer <product_url>")
      System.exit(1)
    }
    val url = args(0)
    sniffPrice(url) match {
      case Some((price, hint)) =>
        println(s"✅ Detected price: €$price (via $hint)")
      case None =>
        println("❌ No price detected.")
    }
  }
}


