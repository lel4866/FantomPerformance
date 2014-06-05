using [java] java.lang

final class CheckDailyOHLC {
    const static Str sSymbol := "RUT"
    const static Str sBaseDir := "/Users/" + System.getProperty("user.name") + "/"
    const static Str sIBDataDir := sBaseDir + "IBData/"
    const static Str sYahooFilePath := sIBDataDir + sSymbol + "/rut.csv"
    const static Str sInDir := sIBDataDir + sSymbol + "/"
    const static Str sInFilePattern := sSymbol + "_\\d{8,8}[.]txt"
    const static Regex inFileRegex := Regex.fromStr(sInFilePattern)
    Date:Bar yahooBars := [:]

    static sys::Void main(Str[] args) {
        start_time := Duration.now
        CheckDailyOHLC main := CheckDailyOHLC()
        main.run()
        end_time := Duration.now
        elapsed_time := (end_time - start_time).toMillis
        echo("duration = $elapsed_time")
    }
      
    sys::Void run() {
        // read data from Yahoo Finance
        yahooFile := File.os(sYahooFilePath)
        if (!yahooFile.exists) {
            echo("*** Error: Yahoo data file $sYahooFilePath does not exist")
            return
        }
        
        lineno := 0
        yahooFile.in.eachLine |line| {
            lineno++
            if (lineno > 1) { // skip header line
                fields := line.split(',')
                if (fields.size > 4) {
                    yahooBar := Bar()
                    yahooBar.date = Date.fromLocale(fields[0], "M/D/Y")
                    yahooBar.open = fields[1].toDecimal
                    yahooBar.high = fields[2].toDecimal
                    yahooBar.low = fields[3].toDecimal
                    yahooBar.close = fields[4].toDecimal
                    yahooBars[yahooBar.date] = yahooBar
                }
                else
                    echo("*** Error: Line $lineno of daily yahoo data")
            }
        }
        
        // read 5sec data from IBData directory and get daily OHLC from each file
        File[] ibFiles := File.os(sInDir).listFiles().exclude { !inFileRegex.matches(it.name) }
        if (ibFiles.size == 0) {
            echo("*** Error: no iB data files in $sInDir")
            return
        }
        ibFiles.sort
        
        ohlcBar := Bar()
        ibFiles.each |ibFile| {
            ibFilename := ibFile.name()
            echo("Processing ${ibFilename}...")
            sFileDate := ibFilename[sSymbol.size+1..-5]
            fileDate := Date.fromLocale(sFileDate, "YYYYMMDD")
            ibStream := ibFile.in
            lineno = 0
            Decimal price := 0d
            ibStream.eachLine |line| {
                lineno++
                //echo("line $lineno: $line")
                fields := line.split(',')
                if (fields.size < 3)
                    echo("*** Error: Too few fields. Line $lineno of $ibFilename")
                else {
                    ohlcBar.date = Date.fromLocale(fields[0], "MM/DD/YYYY")
                    if (ohlcBar.date != fileDate)
                        echo("*** Error: line date not same as file date in line $lineno of IB file $ibFilename")
                    else {
                        ohlcBar.close = price = fields[2].toDecimal()
                        if (lineno == 1)
                            ohlcBar.open = ohlcBar.high = ohlcBar.low = price
                        else {
                            if (price > ohlcBar.high)
                                ohlcBar.high = price
                            else if (price < ohlcBar.low)
                                ohlcBar.low = price
                        }
                    }
                }
            }
            ibStream.close
            
            // now compare IBBars with yahooBars
            sDate := ohlcBar.date.toLocale("MM/DD/YYYY")
            yahooBar := yahooBars[ohlcBar.date]
            if (yahooBar != null) {
                opendiff := yahooBar.open - ohlcBar.open
                highdiff := yahooBar.high - ohlcBar.high
                lowdiff := yahooBar.low - ohlcBar.low
                closediff := yahooBar.close - ohlcBar.close
                echo("$sDate: opendiff=$opendiff highdiff=$highdiff lowdiff=$lowdiff closediff=$closediff")
            }
            else
                echo("yahoo file does not contain entry for ib file of date $sDate")
        }
    }
}

final class Bar {
    Date date := Date.defVal
    Decimal open := 0d
    Decimal high := 0d
    Decimal low := 0d
    Decimal close := 0d
}
