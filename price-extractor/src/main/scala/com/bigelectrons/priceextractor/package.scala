package com.bigelectrons

import pureconfig._
import pureconfig.generic.derivation.default._


package object priceextractor {
  // Output case class
  case class ProductInfo(shop: String, title: String, price: BigDecimal, sourceHTMLSelector: String, isPriceReduced: Boolean = false)

  // Input case classes
  case class ProductRequest(shop: String, url: String, titleSelector: String, priceSelector: String, alertBelow: BigDecimal) derives ConfigReader
  case class ProductRequests(products: Seq[ProductRequest]) derives ConfigReader

  def printTable(rows: Seq[ProductInfo]): Unit = {
    val header = f"${"Shop"}%-14s | ${"Title"}%-50s | ${"Price"}%-8s | ${"Source HTML Selector"}"
    println(header)
    println("-" * header.length)

    rows.foreach { row =>
      println(f"${row.shop}%-14s | ${row.title}%-50s | €${row.price}%-8s | ${row.sourceHTMLSelector}")
    }
  }
}
