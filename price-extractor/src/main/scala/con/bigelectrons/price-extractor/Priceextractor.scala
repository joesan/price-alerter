package con.bigelectrons.priceextractor

import net.ruippeixotog.scalascraper.browser.{JsoupBrowser, HtmlUnitBrowser, Browser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex

object PriceSniffer {

  // Common CSS selectors to try
  val commonPriceSelectors = Seq(
    ".price", ".product-price", ".price--main", ".price-value",
    ".current-price", ".price-amount", "#price", ".price-box"
  )

  // Regex to catch euro / number patterns (very basic)
  // E.g. matches "€ 1.234,56" or "1.234,56 €" or "EUR 1.234,56"
  val euroRegex: Regex = """(?i)(?:€|\bEUR\b)?\s*([0-9]{1,3}(?:[.,\s][0-9]{3})*(?:[.,][0-9]{1,2})?)\s*(?:€|\bEUR\b)?""".r

  /** Normalize a string like "1.234,56" or "1,234.56" to BigDecimal */
  def normalizeNumber(raw: String): Option[BigDecimal] = {
    val s0 = raw.trim.replace("\u00A0", "").replace(" ", "")
    val s = if (s0.contains(",") && s0.contains(".")) {
      // decide which is thousands vs decimal by last occurrence
      if (s0.lastIndexOf(',') > s0.lastIndexOf('.')) {
        s0.replace(".", "").replace(",", ".")
      } else {
        s0.replace(",", "")
      }
    } else if (s0.contains(",")) {
      // assume comma is decimal
      s0.replace(".", "").replace(",", ".")
    } else {
      s0.replace(",", "")
    }
    Try(BigDecimal(s)).toOption
  }

  /** Try extracting via ld+json offers / schema.org Product */
  def extractFromLdJson(doc: net.ruippeixotog.scalascraper.model.Document): Seq[(BigDecimal, String)] = {
    // find all <script type="application/ld+json">
    val scripts = doc >?> elementList("script[type=application/ld+json]")
    scripts.toSeq.flatten.flatMap { e =>
      val jsonText = e.underlyingElement.html   // raw JSON inside
      Try {
        import scala.util.parsing.json.JSON
        JSON.parseFull(jsonText)
      } match {
        case Success(Some(js: Map[String, Any])) =>
          // simplistic, look for "offers" → "price"
          js.get("offers") match {
            case Some(of: Map[String, Any]) =>
              of.get("price") match {
                case Some(p: String) =>
                  normalizeNumber(p).map(bd => (bd, s"ld+json price in offers"))
                case Some(pd: Double) =>
                  Some((BigDecimal(pd), "ld+json numeric offers.price"))
                case _ => None
              }
            case _ => None
          }
        case _ => None
      }
    }
  }

  /** Try some meta / itemprop tags */
  def extractFromMeta(doc: net.ruippeixotog.scalascraper.model.Document): Seq[(BigDecimal, String)] = {
    val hints = Seq(
      "meta[property=product:price:amount]" -> "meta product:price:amount",
      "meta[property=og:price:amount]" -> "meta og:price:amount",
      "meta[name=price]" -> "meta name=price",
      "[itemprop=price]" -> "itemprop price"
    )
    hints.flatMap { case (sel, hint) =>
      doc >?> element(sel) match {
        case Some(elem) =>
          val v = elem.attr("content").trim match {
            case s if s.nonEmpty => s
            case _ => elem.text.trim
          }
          normalizeNumber(v).map(bd => (bd, hint))
        case None => None
      }
    }
  }

  /** Try common CSS selectors */
  def extractFromCommonCss(doc: net.ruippeixotog.scalascraper.model.Document): Seq[(BigDecimal,String)] = {
    commonPriceSelectors.flatMap { sel =>
      doc >?> element(sel) match {
        case Some(elem) =>
          val txt = elem.text.trim
          normalizeNumber(txt).map(bd => (bd, s"css-$sel"))
        case None => None
      }
    }
  }

  /** Regex scan of visible text */
  def extractFromRegex(doc: net.ruippeixotog.scalascraper.model.Document): Seq[(BigDecimal, String)] = {
    val alltext = doc.root.innerHtml // or doc.toHtml, or doc.text
    euroRegex.findAllMatchIn(alltext).flatMap { m =>
      val numStr = m.group(1)
      normalizeNumber(numStr).map(bd => (bd, s"regex '${m.matched}'"))
    }.toSeq
  }

  /** Try a browser (Jsoup or HtmlUnit) to fetch & extract price */
  def attempt(browser: Browser, url: String): Option[(BigDecimal, String)] = {
    Try(browser.get(url)) match {
      case Failure(err) =>
        println(s"[${browser.getClass.getSimpleName}] failed fetch: $err")
        None
      case Success(doc) =>
        val cands = Seq(
          extractFromLdJson(doc),
          extractFromMeta(doc),
          extractFromCommonCss(doc),
          extractFromRegex(doc)
        ).flatten

        // choose best = highest price (plausible) with hint
        if (cands.nonEmpty) {
          Some(cands.maxBy(_._1))
        } else {
          None
        }
    }
  }

  /** Public method: sniff price, trying Jsoup first, then fallback to JS rendering */
  def sniffPrice(url: String): Option[(BigDecimal, String)] = {
    // try JsoupBrowser first (fast)
    val browserJs = JsoupBrowser()
    attempt(browserJs, url) match {
      case Some(best) => Some(best)
      case None =>
        println("JsoupBrowser failed to extract; falling back to HtmlUnitBrowser")
        val browserHtmlUnit = HtmlUnitBrowser()
        attempt(browserHtmlUnit, url)
    }
  }

  // Simple test
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: PriceSniffer <product-url>")
      sys.exit(1)
    }
    val url = args(0)
    sniffPrice(url) match {
      case Some((price, hint)) =>
        println(s"Detected price: €$price  via $hint")
      case None =>
        println("No price detected.")
    }
  }
}

