# Price Extractor

A Scala 3 application for scraping product titles and prices from e-commerce websites using Playwright and Java 21.

## Features

- Extracts product titles and prices via CSS selectors and metadata tags
- Supports fallback strategies for robust extraction
- Uses Playwright for headless browser automation
- Compatible with Scala 3 and Java 21
- CI pipeline with GitHub Actions to run the main extractor after every commit to `master`

## Prerequisites

- Java 21 (tested with Eclipse Temurin distribution)
- [sbt](https://www.scala-sbt.org/) (Scala Build Tool)
- Playwright installed as a project dependency

## Getting Started

### Clone the repository

```bash
git clone https://github.com/joesan/price-alerter.git
cd price-alerter
```

### Build the project

```bash
sbt compile
```

### Run the main application

```bash
sbt "runMain com.bigelectrons.priceextractor.PriceWatcherMain"
```

### Running Tests

```bash
sbt test
```

### GitHub Actions CI

The project includes a GitHub Actions workflow (.github/workflows/run-main.yml) that:

- Runs on ubuntu-latest
- Sets up Java 21 and sbt
- Runs the main application after every commit on the master branch

## Project Structure
src/
main/
scala/            # Scala source files
test/
scala/            # Unit tests (if any)
build.sbt             # sbt build configuration
.github/
workflows/
main.yml          # GitHub Actions workflow

## Dependencies

- Scala 3.x
- Java 21
- Playwright for Java/Scala
- sbt

## Contribution

Feel free to open issues or submit pull requests.

## License

MIT License. See LICENSE file for details.
