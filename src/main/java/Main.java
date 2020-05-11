import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    /**
     * Application Entry Point
     *
     */
    public static void main(String[] args) throws Exception {
        // read files sent by Matilda - this will represent daily sales record for a cupcake type
        List<String> cupcakeSalesRecords = retrieveSalesRecordFileNames();

        // create excel file from template
        createNewXlsReportFromTemplate();

        // for each cupcake type:
        for(String cupcakeSalesRecord : cupcakeSalesRecords) {

            int cupcakeType = ProductRef.valueOf(getProductType(cupcakeSalesRecord)).getProductId();

            // insert new records from .txt files in database
            updateSalesRecordsInDatabase(cupcakeSalesRecord, cupcakeType);

            // query from db for reports needed (weekly, monthly, yearly)
            List<ReportEntity> records = queryReportFromDatabase(cupcakeType);

            // print to excel
            populateXlsReport(records, cupcakeType);
        }

        updateXlsGrandTotals();
        updateMonthNumberToShortForm();
    }

    /**
     * Copy must be created to not overwrite existing template created for Matilda's revenue report.
     * This method creates a copy of the template to the designated output location.
     *
     */
    private static void createNewXlsReportFromTemplate() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(Constants.OUTPUT_PATH)) {
            File file = new File(Constants.REPORT_TEMPLATE_PATH);
            Files.copy(file.toPath(), fileOutputStream);

            Workbook workbook = new HSSFWorkbook();
            workbook.write(fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Modify month numbers in xls file to actual month names to make excel file easier to read for Matilda.
     *
     */
    private static void updateMonthNumberToShortForm() throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(Constants.OUTPUT_PATH));
        Workbook workbook = WorkbookFactory.create(inputStream);

        for(Sheet currentSheet : workbook) {
            // no month data for "Yearly" sheet; therefore, skip
            if(Constants.REPORT_SHEET_YEARLY.equals(currentSheet.getSheetName())) {
                continue;
            }

            Row startRow = currentSheet.getRow(Constants.START_ROW_ALL_SHEETS_IDX);
            int endRowIdx = currentSheet.getLastRowNum(); // not zero based index
            Row endRow = currentSheet.getRow(endRowIdx);

            while(doesNotContainValue(endRow)) {
                endRow = currentSheet.getRow(--endRowIdx);
            }

            endRowIdx = endRowIdx + 1;

            for(int rowNo = Constants.START_ROW_ALL_SHEETS_IDX; rowNo < endRowIdx; rowNo++) {
                Row currentRow = currentSheet.getRow(rowNo);
                Cell currentCell = currentRow.getCell(Constants.MONTH_COLUMN_ALL_SHEETS_IDX);
                int monthNumber = Double.valueOf(currentCell.getNumericCellValue()).intValue();
                currentCell.setCellValue(Month.of(monthNumber).toString());
            }
        }

        inputStream.close();

        // Write the output to the file
        FileOutputStream outputStream = new FileOutputStream(Constants.OUTPUT_PATH);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    /**
     * Calculate grand totals from report of each cupcake type and write to xls file
     *
     */
    private static void updateXlsGrandTotals() throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(Constants.OUTPUT_PATH));
        Workbook workbook = WorkbookFactory.create(inputStream);

        for(Sheet currentSheet : workbook) {
            Row startRow = currentSheet.getRow(Constants.START_ROW_ALL_SHEETS_IDX);
            int grandTotalStartCell = startRow.getLastCellNum() - Constants.GRAND_TOTAL_DETAILS_SPAN;
            int[] totals = new int[3];

            int computeStartIndexCell;
            if(Constants.REPORT_SHEET_YEARLY.equals(currentSheet.getSheetName())) {
                computeStartIndexCell = Constants.YEARLY_DATE_COL_HEADER_SPAN;
            } else if(Constants.REPORT_SHEET_MONTHLY.equals(currentSheet.getSheetName())) {
                computeStartIndexCell = Constants.MONTHLY_DATE_COL_HEADER_SPAN;
            } else if(Constants.REPORT_SHEET_WEEKLY.equals(currentSheet.getSheetName())) {
                computeStartIndexCell = Constants.WEEKLY_DATE_COL_HEADER_SPAN;
            } else {
                continue;
            }
            int computeEndIndexCell = grandTotalStartCell;

            // clean up null/"blank" rows

            int sheetActualLastRowNum = currentSheet.getLastRowNum(); // not zero based index
            Row sheetActualLastRow = currentSheet.getRow(sheetActualLastRowNum);

            while(doesNotContainValue(sheetActualLastRow)) {
                sheetActualLastRow = currentSheet.getRow(--sheetActualLastRowNum);
            }

            sheetActualLastRowNum = sheetActualLastRowNum + 1;

            for(int rowNo = Constants.START_ROW_ALL_SHEETS_IDX; rowNo < sheetActualLastRowNum; rowNo++) {
                Row currentRow = currentSheet.getRow(rowNo);
                // get sum of product details (items sold, unit price, total revenue)
                for(int i = 0; i < Constants.GRAND_TOTAL_DETAILS_SPAN; i++) {
                    int partialSumForCell = 0;
                    for(int j = i + computeStartIndexCell; j < computeEndIndexCell; j+=Constants.PRODUCT_SALES_DETAILS_SPAN) {
                        // already existing, so read-only numeric values, no need to check if null
                        Cell currentCell = currentRow.getCell(j);
                        partialSumForCell = partialSumForCell + Double.valueOf(currentCell.getNumericCellValue()).intValue();
                    }
                    totals[i] = partialSumForCell;
                }
                // write total values to grand total cells
                for(int i = 0; i < Constants.GRAND_TOTAL_DETAILS_SPAN; i++) {
                    Cell currentCell = currentRow.getCell(grandTotalStartCell + i);
                    if(Objects.isNull(currentCell))
                        currentCell = currentRow.createCell(grandTotalStartCell + i);
                    currentCell.setCellValue(totals[i]);
                }
            }
        }

        inputStream.close();

        // Write the output to the file
        FileOutputStream outputStream = new FileOutputStream(Constants.OUTPUT_PATH);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    /**
     * Used to clean up extra lines in xls file and determine actual last row,
     * by checking a cell in each row (starting from "lastRowNum" of sheet)
     *
     */
    private static boolean doesNotContainValue(Row row) {
        // get arbitrary cell number in index 0
        return StringUtils.isEmpty(String.valueOf(row.getCell(0))) ||
                StringUtils.isWhitespace(String.valueOf(row.getCell(0))) ||
                StringUtils.isBlank(String.valueOf(row.getCell(0))) ||
                String.valueOf(row.getCell(0)).length() == 0 ||
                row.getCell(0) == null;
    }

    /**
     * Populate xls report with results of database query
     *
     */
    private static void populateXlsReport(List<ReportEntity> records, int productId) throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(Constants.OUTPUT_PATH));
        Workbook workbook = WorkbookFactory.create(inputStream);

        Sheet yearlySheet = workbook.getSheet(Constants.REPORT_SHEET_YEARLY);
        Sheet monthlySheet = workbook.getSheet(Constants.REPORT_SHEET_MONTHLY);
        Sheet weeklySheet = workbook.getSheet(Constants.REPORT_SHEET_WEEKLY);
        int yearlySheetRowCount = Constants.START_ROW_ALL_SHEETS_IDX,
            monthlySheetRowCount = Constants.START_ROW_ALL_SHEETS_IDX,
            weeklySheetRowCount = Constants.START_ROW_ALL_SHEETS_IDX;

        for(ReportEntity record : records) {
            int[] date = {record.getYear(), record.getMonth(), record.getWeek()};
            int[] sales = {record.getUnitsSold(), record.getUnitPrice(), record.getRevenue()};
            if(record.getMonth() == 0) {
                // yearly total
                Row row = yearlySheet.getRow(yearlySheetRowCount);
                if(Objects.isNull(row))
                    row = weeklySheet.createRow(yearlySheetRowCount);

                // prepare date columns
                int i;
                for(i = 0; i < Constants.YEARLY_DATE_COL_HEADER_SPAN; i++) {
                    Cell cell = row.getCell(i);
                    if(Objects.isNull(cell))
                        cell = row.createCell(i);
                    cell.setCellValue(date[i]);
                }

                // prepare sales details
                int productTypeStartPtr = i + (Constants.PRODUCT_SALES_DETAILS_SPAN * ((productId / 100) - 1));
                int productTypeEndPtr = productTypeStartPtr + Constants.PRODUCT_SALES_DETAILS_SPAN;
                int j = 0;

                for(j = productTypeStartPtr, i = 0; j < productTypeEndPtr; j++, i++) {
                    Cell cell = row.getCell(j);
                    if(Objects.isNull(cell))
                        cell = row.createCell(j);
                    cell.setCellValue(sales[i]);
                }

                yearlySheetRowCount = yearlySheetRowCount + 1;

            } else if(record.getWeek() == 0) {
                // monthly total
                Row row = monthlySheet.getRow(monthlySheetRowCount);
                if(Objects.isNull(row))
                    row = weeklySheet.createRow(monthlySheetRowCount);

                // prepare date columns
                int i;
                for(i = 0; i < Constants.MONTHLY_DATE_COL_HEADER_SPAN; i++) {
                    Cell cell = row.getCell(i);
                    if(Objects.isNull(cell))
                        cell = row.createCell(i);
                    cell.setCellValue(date[i]);
                }

                // prepare sales details
                int productTypeStartPtr = i + (Constants.PRODUCT_SALES_DETAILS_SPAN * ((productId / 100) - 1));
                int productTypeEndPtr = productTypeStartPtr + Constants.PRODUCT_SALES_DETAILS_SPAN;
                int j = 0;

                for(j = productTypeStartPtr, i = 0; j < productTypeEndPtr; j++, i++) {
                    Cell cell = row.getCell(j);
                    if(Objects.isNull(cell))
                        cell = row.createCell(j);
                    cell.setCellValue(sales[i]);
                }

                monthlySheetRowCount = monthlySheetRowCount + 1;

            } else {
                // weekly total
                Row row = weeklySheet.getRow(weeklySheetRowCount);
                if(Objects.isNull(row))
                  row = weeklySheet.createRow(weeklySheetRowCount);

                // prepare date columns
                int i;
                for(i = 0; i < Constants.WEEKLY_DATE_COL_HEADER_SPAN; i++) {
                    Cell cell = row.getCell(i);
                    if(Objects.isNull(cell))
                        cell = row.createCell(i);
                    cell.setCellValue(date[i]);
                }

                // prepare sales details
                int productTypeStartPtr = i + (Constants.PRODUCT_SALES_DETAILS_SPAN * ((productId / 100) - 1));
                int productTypeEndPtr = productTypeStartPtr + Constants.PRODUCT_SALES_DETAILS_SPAN;
                int j = 0;

                for(j = productTypeStartPtr, i = 0; j < productTypeEndPtr; j++, i++) {
                    Cell cell = row.getCell(j);
                    if(Objects.isNull(cell))
                        cell = row.createCell(j);
                    cell.setCellValue(sales[i]);
                }

                weeklySheetRowCount = weeklySheetRowCount + 1;
            }
        }

        inputStream.close();

        // Write the output to the file
        FileOutputStream outputStream = new FileOutputStream(Constants.OUTPUT_PATH);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    /**
     * Retrieve records from the text files provided and compare count with current records in database.
     * if counts do not match (files > records in db), then there is need to update new records in database.
     *
     */
    private static void updateSalesRecordsInDatabase(String salesRecord, int productId) throws Exception {
        // extract line count from file corresponding to cupcake type (list)
        List<Integer> cupcakeDailySalesCount = retrieveSalesRecordContents(salesRecord);
        int newIndex = cupcakeDailySalesCount.size();

        // query from database current count of daily sales records for cupcake (int)
        int currentIndex = queryCountDailySalesRecordByType(productId);

        if(currentIndex == newIndex) {
            System.out.println("No new records to insert in database.");
            return;
        }

        // prepare new records to insert in DB
        List<Integer> newRecordsToInsertInDb = cupcakeDailySalesCount.subList(currentIndex, newIndex);
        Collections.reverse(newRecordsToInsertInDb);
        Map<LocalDate, Integer> cupcakeDailySalesCountDate = new HashMap<>();
        LocalDate correspondingDate = LocalDate.now().plusDays(1);

        for(Integer newRecord : newRecordsToInsertInDb) {
            correspondingDate = correspondingDate.minusDays(1); // today
            cupcakeDailySalesCountDate.put(correspondingDate, newRecord);
        }

        // insert new records in DB
        int insertCount = 0;
        for(Map.Entry<LocalDate, Integer> entry : cupcakeDailySalesCountDate.entrySet()) {
            insertCount = insertCount + insertSalesRecordWhere(entry.getKey(), productId, entry.getValue());
        }
        System.out.println("Successfully inserted " + insertCount + " new records.");
    }

    /**
     * Method to facilitate query of report contents from database
     *
     */
    private static List<ReportEntity> queryReportFromDatabase(int productId) {
        DatabaseManager mgr = new DatabaseManager();
        return mgr.getRevenueReportBy(productId);
    }

    /**
     * Method to facilitate entry of new records from text files to database
     *
     */
    private static int insertSalesRecordWhere(LocalDate salesDate, Integer productId, Integer salesCount) {
        DatabaseManager mgr = new DatabaseManager();
        int resultCount = mgr.insertIntoProductSalesValues(salesDate, productId, salesCount);
        return (resultCount == -1) ? 0 : 1;
    }

    /**
     * Method to facilitate count of existing records in database by type
     *
     */
    private static int queryCountDailySalesRecordByType(int productId) throws Exception {
        DatabaseManager mgr = new DatabaseManager();
        int resultCount = mgr.getProductSalesCountBy(productId);
        if (resultCount == -1) {
            throw new Exception();
        }
        return resultCount;
    }

    /**
     * Method to read from text files
     *
     */
    private static List<Integer> retrieveSalesRecordContents(String cupcakeSalesRecord) {
        System.out.println(cupcakeSalesRecord);
        List<String> cupcakeDailySalesCount = new ArrayList<>();

        // read file using traditional BufferedReader
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(cupcakeSalesRecord));
            reader.readLine();
            while(Objects.nonNull(line = reader.readLine())) {
                cupcakeDailySalesCount.add(line);
            }
            reader.close();
        } catch(IOException ioEx) {
            System.out.println("Error!");
        }

        return cupcakeDailySalesCount.stream().map(rec -> Integer.parseInt(rec)).collect(Collectors.toList());
    }

    /**
     * Method to get type of product (Basic/Deluxe cupcake) based on the file name of text file passed
     *
     */
    private static String getProductType(String salesRecord) {
        return salesRecord.substring(salesRecord.lastIndexOf("\\") + 1, salesRecord.lastIndexOf("."));
    }

    /**
     * Method to retrieve files from preset directory where the text files are located
     *
     */
    private static List<String> retrieveSalesRecordFileNames() {
        List<String> salesRecordFileNames = null;
        try(Stream<Path> walk = Files.walk(Paths.get(Constants.CUPCAKE_SALES_DIR))) {
            salesRecordFileNames = walk.filter(Files::isRegularFile)
                                        .map(file -> file.toString())
                                        .collect(Collectors.toList());
        } catch(IOException e) {
            e.printStackTrace();
        }

        return salesRecordFileNames;
    }
}
