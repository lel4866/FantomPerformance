FantomPerformance
=================

Examples written in Fantom and other languages (Java, Kotlin, etc.) to compare performance

CheckDailyOHLC

This program processes a directory of stock data files in directory C:\Users\{username}\IBData\{symbol}. 
The sample files stored here are for the {symbol} RUT, so they should be placed in
C:\Users\{username}\IBData\RUT. These files have names like RUT_20130913.txt. Each file is a comma
separated value file of ticks. The first line is a header giving the column names. Each following
line looks like:

09/13/2013,09:30:20,1051.98

The sample files have a line for every 5 seconds during the trading day.

There is an additional file in C:\Users\{username}\IBData\RUT named rut.csv. It is a file downloaded
from finance.yahoo.com that contains lines which are the date, time, open, high, low, and close for
a number of trading days (plus a couple of other columns). A typical line looks like:

2014-04-21,1139.59,1142.39,1132.22,1142.31,26425000,1142.31

The goal of the program is to compare the open, high, low, and close reported by yahoo for a given day
to the open, high, low, and close computed from each tick file for that day. The tick files are downloaded
from Interactive Brokers. So we want to see how closely the real-time data from IB compares to the data
downloaded from Yahoo Finance.

