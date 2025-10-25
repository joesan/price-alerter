package com.bigelectrons.priceextractor

import cats.effect.std.Semaphore
import com.bigelectrons.priceextractor.PriceExtractor.*
import cats.effect.{IO, IOApp}
import cats.implicits.*


object PriceWatcherMain extends IOApp.Simple {

  private def sniffPriceIO(entry: ProductRequest): IO[Option[ProductInfo]] = IO {
    println(s"🔍 Fetching: ${entry.shop} -> ${entry.url}")
    sniffPrice(entry).map { product =>
      product.copy(isPriceReduced = product.price < entry.alertBelow)
    }
  }

  def printTopBuildCombos(products: Seq[ProductInfo], topN: Int = 5): Unit = {
    println("\n🔧 Top Component Builds:\n")

    // Group and sort each component type by price ascending
    val groupedByType: Map[ComponentType, Seq[ProductInfo]] =
      products.groupBy(_.componentType).view.mapValues(_.sortBy(_.price)).toMap

    // Find the maximum number of builds possible (based on the type with the most items)
    val maxBuildCount = groupedByType.values.map(_.size).maxOption.getOrElse(0)
    if (maxBuildCount == 0) {
      println("❌ No components available to generate builds.")
      return
    }

    // Construct builds index-wise (0th = cheapest of each type, etc.)
    val builds: Seq[(Seq[ProductInfo], BigDecimal)] = (0 until maxBuildCount).map { idx =>
      val combo = groupedByType.values.flatMap(_.lift(idx)).toSeq
      val totalPrice: BigDecimal = combo.map(_.price).sum
      (combo, totalPrice)
    }

    // Limit output to topN builds (in order of index, not total price)
    builds.take(topN).zipWithIndex.foreach { case ((combo, total), idx) =>
      println(s"\n🏅 Build #${idx + 1} - Total Price: €${Console.BOLD}${Console.RED}$total${Console.RESET}")
      combo.foreach { p =>
        val coloredPrice = s"${Console.BOLD}${Console.RED}€${p.price}${Console.RESET}"
        println(f"- ${p.shop}%-14s | ${p.componentType}%-12s | $coloredPrice | ${p.url}")
      }
    }
  }

  def run: IO[Unit] = {
    val productList = loadProductRequests.products
    val parallelism = 4

    for {
      startTime <- IO(System.currentTimeMillis())

      // ✅ Create a semaphore instance with 4 permits
      semaphore <- Semaphore[IO](parallelism)

      // ✅ Use `semaphore.permit` on the instance, not the object
      results <- productList.parTraverse { req =>
        semaphore.permit.use(_ => sniffPriceIO(req))
      }

      validResults = results.flatten

      _ <- IO {
        println("\n📊 Price Report:\n")
        printTable(validResults)

        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        println(s"\n⏱️  Price extraction completed in $duration seconds.")

        printTopBuildCombos(validResults, topN = 3)
      }
    } yield ()
  }
}
