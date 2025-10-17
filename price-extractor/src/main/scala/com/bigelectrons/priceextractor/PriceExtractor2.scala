package com.bigelectrons.priceextractor

import com.microsoft.playwright.*
import com.microsoft.playwright.options.{LoadState, WaitUntilState}

import scala.jdk.CollectionConverters.*
import scala.util.Try

object PriceExtractor2 {

  def normalizeNumber(raw: String): Option[BigDecimal] = {
    val cleaned = raw.replace("€", "").replace(",", ".").replaceAll("[^\\d.]", "")
    Try(BigDecimal(cleaned)).toOption
  }

  def extractProductTitle(page: Page): Option[String] = {
    val selectors = Seq(
      "h1.product--title", // bike-discount
      "div.product-detail-information-area__header h1.product-detail-information-area__product-name" // bike24
      // add more here for other sites
    )

    selectors.view
      .flatMap { selector =>
        val locator = page.locator(selector)
        if (locator.count() > 0)
          Some(locator.first().textContent().trim)
        else None
      }
      .headOption
  }

  def extractPriceFromSelectors(page: Page): Option[(BigDecimal, String)] = {
    val selectors = Seq(
      "#netz-price",            // ✅ Specific to Bike-Discount
      ".price__value",          // ✅ Specific to Bike-24
      ".product-price",         // Common in ecommerce
      ".price",                 // Generic
      ".price--main",           // Common in ecommerce
      ".price-value",           // Fallbacks
      ".price-amount",
      ".product-price--sale",
      ".special-price"
    )

    selectors.view
      .flatMap { selector =>
        val locator = page.locator(selector)
        if (locator.count() > 0) {
          val text = locator.first().textContent().trim()
          normalizeNumber(text).map(price => (price, s"Selector: $selector"))
        } else None
      }
      .headOption
  }

  def extractPriceFromJsonLd(page: Page): Option[(BigDecimal, String)] = {
    val scripts = page.locator("script[type='application/ld+json']").allTextContents().asScala
    scripts.flatMap { json =>
      val pricePattern = """"price"\s*:\s*"?(\\d+[.,]?\d*)"?""".r
      pricePattern.findFirstMatchIn(json).flatMap { m =>
        normalizeNumber(m.group(1)).map(price => (price, "JSON-LD"))
      }
    }.headOption
  }

  def sniffPrice(url: String): Option[ProductInfo] = {
    val pw = Playwright.create()
    val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))

    val context = browser.newContext(new Browser.NewContextOptions()
      .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117 Safari/537.36")
      .setBypassCSP(true)
      .setLocale("en-US")
    )

    val page = context.newPage()
    page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
    page.waitForLoadState(LoadState.DOMCONTENTLOADED)

    val titleOpt = extractProductTitle(page)
    val priceOpt = extractPriceFromSelectors(page).orElse(extractPriceFromJsonLd(page))

    browser.close()
    pw.close()

    for {
      title <- titleOpt
      (price, source) <- priceOpt
    } yield ProductInfo(title, price, source)
  }

  def main(args: Array[String]): Unit = {
    val url = "https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc"
    sniffPrice("https://www.bike-discount.de/en/shimano-grx-rx820/610-1x12-speed-group-disc") match {
      case Some(info) =>
        println(s"✅ [${info.title}] — €${info.price} (${info.source})")
      case None =>
        println("❌ Could not extract title or price.")
    }
  }
}

