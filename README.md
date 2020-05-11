# MatildaCupcakeRevenue

**Weekly Project Club Challenge 1**

**Description:**

This Java application takes in 2 text files (Basic.txt, Deluxe.txt, Total.txt) sent by Matilda, which are placed in a set location on the file system.

Daily sales records for each type (Basic, Deluxe) are extracted and assigned their corresponding dates.

These records are checked if existing in the database (by date). Only new records (after last database update) are inserted.

A query is then executed, which returns data needed for weekly/monthly/yearly report.

These details are printed to an .xls file, based off a template created for the report.

Generated .xls file contains three sheets "Yearly", "Monthly", "Weekly", which summarizes revenues per item and their totals, for all dates covered by the .txt files provided.

A sample generated .xls report by the application can be found in src/main/resources/Total_Revenue_Report.xls


**Assumptions held during development:**

                1. Matilda's business operates every day

                2. Matilda does not skip days on the .txt files being sent

                3. On days where she has no sales, she will still record 0

                4. No modifications are being made to the previous lines in the .txt files


**Other notes:**

                1. Total.txt was not used. Totals are being calculated and returned by the query inside application.


**APIs used:**

                PostgreSQL JDBC - for the database connection

                Apache POI - for interacting with xls file/s

                Apache Commons - for use of StringUtils

                Commons DbUtils - to supplement PostgreSQL JDBC
