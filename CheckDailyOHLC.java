import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;

public class CheckDailyOHLC {
    static final String sSymbol = "RUT";
    static final String sBaseDir = "c:/Users/" + System.getProperty("user.name") + "/";
    static final String sIBDataDir = sBaseDir + "IBData/";
    static final String sYahooFilePath = sIBDataDir + sSymbol + "/rut.csv";
    static final String sInDir = sIBDataDir + sSymbol + "/";
    static final String sInFilePattern = sSymbol + "_\\d{8,8}[.]txt";

    static class Bar {
        LocalDate date;
        double open;
        double high;
        double low;
        double close;
    }

    Map<LocalDate, Bar> yahooBars = new HashMap<>();

    public static void main(String[] args) throws IOException {
        LocalTime start_time = LocalTime.now();
        CheckDailyOHLC object = new CheckDailyOHLC();
        object.run();
        LocalTime end_time = LocalTime.now();
        Duration elapsed_time = Duration.between(start_time, end_time);
        System.out.printf("CheckDailyOHLC elapsed time is %d milliseconds\n", elapsed_time.toMillis());
    }

    void run() throws IOException {
        File yahooFile = new File(sYahooFilePath);
        if (!yahooFile.exists()) {
            System.out.println("*** Error: Yahoo data file " + sYahooFilePath + " does not exist");
            System.exit(1);
        }

        int line_no;
        String[] fields;
        List<String> lines = Files.readAllLines(yahooFile.toPath());
        lines.remove(0); // skip header
        line_no = 0;
        for (String line : lines) {
            line_no++;
            fields = line.split(",");
            if (fields.length < 5) {
                System.out.println("*** Error: Line " + line_no + " of daily yahoo data");
                continue;
            }
            Bar yahooBar = new Bar();
            yahooBar.date = LocalDate.parse(fields[0], DateTimeFormatter.ofPattern("M/d/y"));
            yahooBar.open = Double.parseDouble(fields[1]);
            yahooBar.high = Double.parseDouble(fields[2]);
            yahooBar.low = Double.parseDouble(fields[3]);
            yahooBar.close = Double.parseDouble(fields[4]);
            yahooBars.put(yahooBar.date, yahooBar);
        }

        // read 5sec data from IBData directory and get daily OHLC from each file
        File[] ib_files = getFiles(sInDir, sInFilePattern);
        if (ib_files == null || ib_files.length == 0)
            System.out.println("*** Error: no iB data files in " + sInDir);

        Bar ohlcBar = new Bar();
        double price;
        //assert ib_files != null;
        for (File ib_file : ib_files) {
            String ibFilename = ib_file.getName();
            lines = Files.readAllLines(Paths.get(sInDir + ibFilename));
            System.out.println("Processing " + ibFilename + "...");
            String sFileDate = ibFilename.substring(sSymbol.length() + 1, sSymbol.length() + 9);
            LocalDate fileDate = LocalDate.parse(sFileDate, DateTimeFormatter.BASIC_ISO_DATE);
            line_no = 0;
            for (String line : lines) {
                line_no++;
                fields = line.split(",");

                if (fields.length < 3) {
                    System.out.println("*** Error: Too few fields. Line " + line_no + " of " + ibFilename);
                    continue;
                }

                ohlcBar.date = LocalDate.parse(fields[0], DateTimeFormatter.ofPattern("M/d/y"));
                if (!ohlcBar.date.equals(fileDate)) {
                    System.out.println("*** Error: line date not same as file date in line " + line_no + " of IB file " + ibFilename);
                    continue;
                }

                // ignore lines before 9:30
                LocalTime tickTime = LocalTime.parse(fields[1], DateTimeFormatter.ISO_LOCAL_TIME);
                if (tickTime.isBefore(LocalTime.of(9, 30)))
                    continue;

                ohlcBar.close = price = Double.parseDouble(fields[2]);
                if (line_no == 1)
                    ohlcBar.open = ohlcBar.high = ohlcBar.low = price;
                else {
                    if (price > ohlcBar.high)
                        ohlcBar.high = price;
                    else if (price < ohlcBar.low)
                        ohlcBar.low = price;
                }
            }

            // now compare IBBars with yahooBars
            Bar yahooBar = yahooBars.get(ohlcBar.date);
            String sDate = ohlcBar.date.format(DateTimeFormatter.BASIC_ISO_DATE);
            if (yahooBar != null) {
                double opendiff = yahooBar.open - ohlcBar.open;
                double highdiff = yahooBar.high - ohlcBar.high;
                double lowdiff = yahooBar.low - ohlcBar.low;
                double closediff = yahooBar.close - ohlcBar.close;
                NumberFormat formatter = new DecimalFormat("#0.00");
                System.out.println(sDate + ": opendiff=" + formatter.format(opendiff) + " highdiff=" + formatter.format(highdiff) + " lowdiff=" + formatter.format(lowdiff) + " closediff=" + formatter.format(closediff));
            }
            else
                System.out.println("yahoo file does not contain entry for ib file of date " + sDate);
        }
    }

    File[] getFiles(String dirname, String pattern) {
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            System.out.println("****Error:" + dirname + " is not a directory");
            return null;
        }

        return dir.listFiles((File f) -> f.getName().matches(pattern));
    }
}