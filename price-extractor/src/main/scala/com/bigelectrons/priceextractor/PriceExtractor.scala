package com.bigelectrons.priceextractor

import com.microsoft.playwright.*
import com.microsoft.playwright.options.{LoadState, WaitUntilState}

import scala.jdk.CollectionConverters.*


object PriceExtractor {

  def normalizeNumber(text: String): Option[BigDecimal] = {
    val cleaned = text.replaceAll("[^0-9,\\.]", "") // Keep digits, commas, dots
    val normalized = cleaned.replace(',', '.') // Replace comma with dot for decimals
    try {
      Some(BigDecimal(normalized))
    } catch {
      case _: NumberFormatException => None
    }
  }

  def extractPriceFromMeta(page: Page): Option[(BigDecimal, String)] = {
    val selectors = Seq(
      ("meta[property=product:price:amount]", "product:price"),
      ("meta[property=og:price:amount]", "og:price"),
      ("meta[name=price]", "meta name=price"),
      ("meta[itemprop=price]", "itemprop=price")
    )

    selectors.view
      .flatMap { case (selector, hint) =>
        val element = page.locator(selector)
        if (element.count() > 0) {
          val handle = element.first()
          val content = Option(handle.getAttribute("content")).filter(_.nonEmpty)
            .orElse(Some(handle.textContent()).filter(_.nonEmpty))

          content.flatMap(normalizeNumber).map(price => (price, s"Meta tag [$hint]"))
        } else None
      }
      .headOption
  }

  def extractTitleFromMeta(page: Page): Option[(String, String)] = {
    val selectors = Seq(
      ("meta[name=description]", "meta name=description"),
      ("meta[property=og:description]", "og:description"),
      ("meta[name=twitter:description]", "twitter:description"),
      ("meta[itemprop=description]", "itemprop=description")
    )

    selectors.view
      .flatMap { case (selector, source) =>
        val locator = page.locator(selector)
        if (locator.count() > 0) {
          val handle = locator.first()
          Option(handle.getAttribute("content"))
            .filter(_.nonEmpty)
            .map(desc => (desc.trim, s"Meta tag [$source]"))
        } else None
      }
      .headOption
  }

  def extractProductTitle(page: Page): Option[String] = {
    val selectors = Seq(
      "h1.product--title", // bike-discount
      "div.product-detail-information-area__header h1.product-detail-information-area__product-name", // bike24
      "h1.product-title[itemprop='name']" // r2bike
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
      "#netz-price", // Specific to Bike-Discount
      ".price_wrapper .special-price", // R2 Bike
      ".special-price", // Existing generic special price
      ".price__value", // Bike-24 specific
      ".price .d-flex", // Nested span inside price, matches your snippet's inner span
      ".product-price", // Common in ecommerce
      ".price--main", // Common in ecommerce
      ".price-value", // Fallbacks
      ".price-amount",
      ".product-price--sale",
      ".price" // Generic fallback
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

  def sniffPrice(shop: String, url: String): Option[ProductInfo] = {
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

    // --- TITLE EXTRACTION ---
    val titleOpt: Option[(String, String)] = extractTitleFromMeta(page)
      .orElse(extractProductTitle(page).map(t => (t, "Extracted via CSS selector")))

    // --- PRICE EXTRACTION ---
    val priceOpt = extractPriceFromMeta(page)
      .orElse(extractPriceFromSelectors(page))

    browser.close()
    pw.close()

    for {
      (title, source) <- titleOpt
      (price, source) <- priceOpt
    } yield ProductInfo(shop, title, price, source)
  }
}
