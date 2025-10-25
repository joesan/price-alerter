package com.bigelectrons

import pureconfig._
import pureconfig.generic.derivation.default._
import pureconfig.error.CannotConvert
import pureconfig.ConfigReader.Result


package object priceextractor {

  sealed trait ComponentType

  object ComponentType {
    case object GroupSet extends ComponentType
    case object WheelSet extends ComponentType
    case object HandleBar extends ComponentType
    case object BrakeRotor extends ComponentType

    val allComponentTypes: List[ComponentType] =
      List(GroupSet, WheelSet, HandleBar)

    def fromString(s: String): Either[CannotConvert, ComponentType] = s.toLowerCase match {
      case "wheelset" => Right(WheelSet)
      case "groupset" => Right(GroupSet)
      case "handlebar" => Right(HandleBar)
      case "brakerotor" => Right(BrakeRotor)
      case other => Left(CannotConvert(other, "ComponentType", s"Unrecognized component type: $other"))
    }

    implicit val configReader: ConfigReader[ComponentType] =
      ConfigReader.fromString[ComponentType](fromString)
  }

  // Output case class
  case class ProductInfo(shop: String, url: String, title: String, componentType: ComponentType, price: BigDecimal, sourceHTMLSelector: String, isPriceReduced: Boolean = false)

  // Input case classes
  case class ProductRequest(shop: String, url: String, componentType: ComponentType, titleSelector: String, priceSelector: String, alertBelow: BigDecimal) derives ConfigReader
  case class ProductRequests(products: Seq[ProductRequest]) derives ConfigReader

  def loadProductRequests: ProductRequests = {
    val config = ConfigSource.resources("product-source.conf").load[ProductRequests] match {
      case Right(conf) => conf
      case Left(errors) =>
        println("❌ Failed to load config:")
        errors.toList.foreach(println)
        sys.exit(1)
    }
    config
  }

  def printTable(rows: Seq[ProductInfo]): Unit = {
    // ANSI formatting codes
    val BOLD_RED = "\u001b[1m\u001b[31m"
    val RESET = "\u001b[0m"

    val header = f"${"Shop"}%-14s | ${"Title"}%-50s | ${"Price"}%-10s | ${"Source HTML Selector"}"
    println(header)
    println("-" * header.length)

    rows.foreach { row =>
      val coloredPrice = f"$BOLD_RED€${row.price}%-8s$RESET"
      println(f"${row.shop}%-14s | ${row.title}%-50s | $coloredPrice | ${row.sourceHTMLSelector}")
    }
  }
}
