import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val elapsedTime = measureTimeMillis { CheckDailyOHLC().run() }
    println("CheckDailyOHLC elapsed time is $elapsedTime milliseconds")
}

class Bar(var date: Date = Date(0L), var open: BigDecimal = BigDecimal.ZERO,
          var high: BigDecimal = open, var low: BigDecimal = open, var close: BigDecimal = open)

class CheckDailyOHLC {
    companion object {
        const val sSymbol = "RUT"
        val sBaseDir = "${System.getProperty("user.home")}/"
        val sIBDataDir = "${sBaseDir}IBData/"
        val sYahooFilePath = "$sIBDataDir$sSymbol/rut.csv"
        val sInDir = "$sIBDataDir$sSymbol/"
        val sInFileRegex = Regex(sSymbol + "_\\d{8,8}[.]txt")

        val sYahooFileDateFormat = SimpleDateFormat("M/d/y", Locale.getDefault())
        val sIBFileNameDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val sIBFileDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    }

    val yahooBars = mutableMapOf<Date, Bar>()

    fun run() {
        val yahooFile = File(sYahooFilePath)
        check(yahooFile.exists()) { "*** Error: Yahoo data file $sYahooFilePath does not exist" }

        // add Yahoo data to yahooBars
        yahooFile.useLines {
            lines ->
            // skip header line
            lines.drop(1).forEachIndexed { line_no, line ->
                val fields = line.split(',')
                if (fields.size > 4) {
                    val yahooBar = Bar(
                            date = sYahooFileDateFormat.parse(fields[0]),
                            open = BigDecimal(fields[1]),
                            high = BigDecimal(fields[2]),
                            low = BigDecimal(fields[3]),
                            close = BigDecimal(fields[4])
                    )
                    yahooBars[yahooBar.date] = yahooBar
                } else
                    println("*** Error: Line $line_no of daily yahoo data")
            }
        }

        // read 5sec data from IBData directory and get daily OHLC from each file
        val ib_files = File(sInDir).listFiles { file -> file.isFile && file.name matches sInFileRegex }
        check(ib_files != null && ib_files.isNotEmpty()) { "*** Error: no iB data files in $sInDir" }
        ib_files.sort()

        val ohlcBar = Bar()
        ib_files.forEach { file ->
            val ibFilename = file.name
            println("Processing $ibFilename...")
            val sFileDate = ibFilename.substring(sSymbol.length + 1, sSymbol.length + 9)
            val fileDate = sIBFileNameDateFormat.parse(sFileDate)
            file.useLines {
                lines ->
                lines.forEachIndexed lit@ { line_no, line ->
                    val fields = line.split(',')
                    if (fields.size < 3) {
                        println("*** Error: Too few fields. Line $line_no of $ibFilename")
                        return@lit
                    }

                    ohlcBar.date = sIBFileDateFormat.parse(fields[0])
                    if (ohlcBar.date != fileDate) {
                        println("*** Error: line date not same as file date in line $line_no of $ibFilename")
                        return@lit
                    }

                    val price = BigDecimal(fields[2])
                    ohlcBar.close = price
                    if (line_no == 0) {
                        ohlcBar.open = price
                        ohlcBar.high = price
                        ohlcBar.low = price
                    } else {
                        if (price > ohlcBar.high)
                            ohlcBar.high = price
                        else if (price < ohlcBar.low)
                            ohlcBar.low = price
                    }
                }
            }

            // now compare IB bars with yahooBars
            val sDate = sIBFileDateFormat.format(ohlcBar.date)
            val yahooBar = yahooBars[ohlcBar.date]
            if (yahooBar != null) {
                val opendiff = yahooBar.open - ohlcBar.open
                val highdiff = yahooBar.high - ohlcBar.high
                val lowdiff = yahooBar.low - ohlcBar.low
                val closediff = yahooBar.close - ohlcBar.close
                println("$sDate: opendiff=$opendiff highdiff=$highdiff lowdiff=$lowdiff closediff=$closediff")
            } else
                println("yahoo file does not contain entry for ib file of date $sDate")
        }
    }
}
