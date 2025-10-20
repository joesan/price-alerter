package com.bigelectrons.priceextractor

import com.bigelectrons.priceextractor.PriceExtractor._

import cats.effect.{IO, IOApp}
import cats.implicits._


object PriceWatcherMain extends IOApp.Simple {

  private def sniffPriceIO(entry: ProductRequest): IO[Option[ProductInfo]] = IO {
    println(s"🔍 Fetching: ${entry.shop} -> ${entry.url}")
    sniffPrice(entry).map { product =>
      product.copy(isPriceReduced = product.price < entry.alertBelow)
    }
  }

  def printTopBuildCombos(products: Seq[ProductInfo], topN: Int = 5): Unit = {
    println("\n🔧 Top Component Builds:\n")

    val groupedByType: Map[ComponentType, Seq[ProductInfo]] = products.groupBy(_.componentType)

    val cheapestByType: Map[ComponentType, ProductInfo] = groupedByType.collect {
      case (ctype, items) if items.nonEmpty =>
        ctype -> items.minBy(_.price)
    }

    // For ranking, we need all possible combinations (Cartesian product)
    val groupedLists: List[List[ProductInfo]] = ComponentType.allComponentTypes.map { ctype =>
      groupedByType.getOrElse(ctype, Seq.empty).sortBy(_.price).take(3).toList
    }

    val allCombos: List[List[ProductInfo]] = groupedLists match {
      case List(a, b, c) =>
        for {
          x <- a
          y <- b
          z <- c
        } yield List(x, y, z)
      case _ =>
        println("❌ Not enough components to generate build combos.")
        Nil
    }

    val ranked = allCombos.map { combo =>
      (combo, combo.map(_.price).sum)
    }.sortBy(_._2)

    ranked.take(topN).zipWithIndex.foreach { case ((combo, total), idx) =>
      println(s"\n🏅 Build #${idx + 1} - Total Price: €${Console.BOLD}${Console.RED}$total${Console.RESET}")
      combo.foreach { p =>
        val coloredPrice = s"${Console.BOLD}${Console.RED}€${p.price}${Console.RESET}"
        println(f"- ${p.shop}%-14s | ${p.componentType}%-10s | $coloredPrice | ${p.url}")
      }
    }
  }

  def run: IO[Unit] = {
    val startTime = System.currentTimeMillis()
    val productList = loadProductRequests.products

    for {
      results <- productList.parTraverse(sniffPriceIO) // run in parallel
      validResults = results.flatten
      _ <- IO {
        println("\n📊 Price Report:\n")
        printTable(validResults)
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        println(s"\n⏱️  Price extraction Completed in $duration seconds.")
        // Print top build combos (cheapest combinations)
        printTopBuildCombos(validResults, topN = 3)
      }
    } yield ()
  }
}
