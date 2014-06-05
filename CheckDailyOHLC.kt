import java.time.LocalDate
import java.util.HashMap
import java.io.File
import kotlin.util.measureTimeMillis
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.text.DecimalFormat

fun main(args: Array<String>) {
    val elapsedTime = measureTimeMillis { CheckDailyOHLC().run() }
    println("CheckDailyOHLC elapsed time is $elapsedTime milliseconds")
}

class Bar() {
    var date: LocalDate = LocalDate.MIN
    var open: Double = 0.0
    var high: Double = 0.0
    var low: Double = 0.0
    var close: Double = 0.0
}

class CheckDailyOHLC() {
    class object {
        val sSymbol = "RUT"
        val sBaseDir = "C:/Users/" + System.getProperty("user.name") + "/"
        val sIBDataDir = sBaseDir + "IBData/"
        val sYahooFilePath = sIBDataDir + sSymbol + "/rut.csv"
        val sInDir = sIBDataDir + sSymbol + "/"
        val sInFilePattern = sSymbol + "_\\d{8,8}[.]txt"
    }

    val yahooBars = HashMap<LocalDate, Bar>()

    fun run() {
        val yahooFile = File(sYahooFilePath)
        if (!yahooFile.exists()) {
            println("*** Error: Yahoo data file $sYahooFilePath does not exist")
            return
        }

        // add Yahoo data to yahooBars
        yahooFile.forEachLineWithLinenum { line, line_no ->
            if (line_no > 0) {
                val fields = line.split(',')
                if (fields.size > 4) {
                    var yahooBar = Bar()
                    yahooBar.date = LocalDate.parse(fields[0], DateTimeFormatter.ofPattern("M/d/y")) ?: LocalDate.MIN
                    yahooBar.open = fields[1].toDouble()
                    yahooBar.high = fields[2].toDouble()
                    yahooBar.low = fields[3].toDouble()
                    yahooBar.close = fields[4].toDouble()
                    yahooBars.put(yahooBar.date, yahooBar)
                } else
                    println("*** Error: Line $line_no of daily yahoo data")
            }
        }

        // read 5sec data from IBData directory and get daily OHLC from each file
        val ib_files = File(sInDir).listFiles { it.isFile() && it.name.matches(sInFilePattern) }
        if (ib_files == null || ib_files.isEmpty()) {
            println("*** Error: no iB data files in $sInDir")
            return
        }

        var ohlcBar = Bar()
        ib_files.forEach { file ->
            val ibFilename = file.getName()
            println("Processing " + ibFilename + "...")
            val sFileDate = ibFilename.substring(sSymbol.length() + 1, sSymbol.length() + 9)
            val fileDate = ldParse(sFileDate, DateTimeFormatter.BASIC_ISO_DATE)
            file.forEachLineWithLinenum @lit {(line, line_no): Unit ->
                val fields = line.split(',')
                if (fields.size < 3) {
                    println("*** Error: Too few fields. Line $line_no of $ibFilename")
                    return@lit
                }

                ohlcBar.date = LocalDate.parse(fields[0], DateTimeFormatter.ofPattern("M/d/y")) ?: LocalDate.MIN
                if (ohlcBar.date != fileDate) {
                    println("*** Error: line date not same as file date in line $line_no of $ibFilename")
                    return@lit
                }

                // ignore lines before 9:30am
                val tickTime = LocalTime.parse(fields[1], DateTimeFormatter.ISO_LOCAL_TIME) ?: LocalTime.MIDNIGHT
                if (tickTime.isBefore(LocalTime.of(9, 30)))
                    return@lit

                ohlcBar.close = java.lang.Double.parseDouble(fields[2])
                if (line_no == 1L) {
                    ohlcBar.open = ohlcBar.close
                    ohlcBar.high = ohlcBar.close
                    ohlcBar.low = ohlcBar.close
                    return@lit
                }

                if (ohlcBar.close > ohlcBar.high)
                    ohlcBar.high = ohlcBar.close
                else if (ohlcBar.close < ohlcBar.low)
                    ohlcBar.low = ohlcBar.close
            }

            // now compare IB bars with yahooBras
            val yahooBar = yahooBars[ohlcBar.date]
            val sDate = ohlcBar.date.format(DateTimeFormatter.BASIC_ISO_DATE)
            if (yahooBar != null) {
                val opendiff = yahooBar.open - ohlcBar.open
                val highdiff = yahooBar.high - ohlcBar.high
                val lowdiff = yahooBar.low - ohlcBar.low
                val closediff = yahooBar.close - ohlcBar.close
                val formatter = DecimalFormat("#0.00")
                println(sDate + ": opendiff=" + formatter.format(opendiff) + " highdiff=" + formatter.format(highdiff) + " lowdiff=" + formatter.format(lowdiff) + " closediff=" + formatter.format(closediff))
            }
            else
                println("yahoo file does not contain entry for ib file of date " + sDate)
        }
    }
}

fun File.forEachLineWithLinenum(block: (String, Long) -> Unit): Unit {
    var __line_no = 0L
    forEachLine { block(it, __line_no++) }
}

fun ldParse(dateString: String, dtf : DateTimeFormatter?=DateTimeFormatter.ISO_LOCAL_DATE): LocalDate {
    return LocalDate.parse(dateString, dtf) ?: LocalDate.MIN
}

fun Double.fmtDec(digitsToRightOfDec: Int = 2) = java.lang.String.format("%.${digitsToRightOfDec}f", this)
