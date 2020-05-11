import org.apache.commons.dbutils.DbUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String PG_DB_URL = "jdbc:postgresql://localhost/matildascupcakes";
    private static final String PG_DB_USER = "postgres";
    private static final String PG_DB_PW = "postgres";

    private Connection connect() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(PG_DB_URL, PG_DB_USER, PG_DB_PW);
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
        return connection;
    }

    public List<ReportEntity> getRevenueReportBy(int productId) {
        List<ReportEntity> resultList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connect();
            ps = conn.prepareStatement(generateRevenueRepostSql());
            ps.setInt(1, productId);
            ps.setInt(2, productId);
            ps.setInt(3, productId);
            ps.setInt(4, productId);
            rs = ps.executeQuery();

            while(rs.next()) {
                ReportEntity resultRow = new ReportEntity(
                    rs.getInt("year"),
                    rs.getInt("month"),
                    rs.getInt("week"),
                    rs.getInt("total_items_sold"),
                    rs.getInt("product_price"),
                    rs.getInt("total_revenues")
                );
                resultList.add(resultRow);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(conn);
        }

        return resultList;
    }

    /**
     * Query count from product_sales table by product name
     * @param productId the product id of product
     * @return resultCount
     */
    public int getProductSalesCountBy(int productId) {
        int resultCount = -1;
        String sql = "select count(*) from product_sales where product_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connect();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            rs = ps.executeQuery();
            rs.next();
            resultCount = rs.getInt(1);
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(conn);
        }

        return resultCount;
    }

    public int insertIntoProductSalesValues(LocalDate entryDate, Integer productId, Integer salesCount) {
        int updateCount = -1;
        String sql = "insert into product_sales values(?,?,?)";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connect();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(entryDate));
            ps.setInt(2, productId);
            ps.setInt(3, salesCount);

            updateCount = ps.executeUpdate();
            // System.out.println(ps.toString());

        } catch(SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(conn);
        }

        return updateCount;
    }

    private String generateRevenueRepostSql() {
        return "SELECT totals.*, price_ref.product_price, price_ref.product_price * totals.total_items_sold AS total_revenues " +
                " FROM (\n" +
                "        (SELECT date_part('year', entry_date) AS YEAR, NULL AS MONTH, NULL AS WEEK, sum(sales_count) AS total_items_sold\n" +
                "         FROM product_sales\n" +
                "         WHERE product_id = ?\n" +
                "         GROUP BY YEAR)\n" +
                "      UNION ALL\n" +
                "        (SELECT date_part('year', entry_date) AS YEAR, date_part('month', entry_date) AS MONTH, NULL AS WEEK, sum(sales_count) AS total_items_sold\n" +
                "         FROM product_sales\n" +
                "         INNER JOIN product_price USING (product_id)\n" +
                "         WHERE product_id = ?\n" +
                "         GROUP BY YEAR, MONTH)\n" +
                "      UNION ALL\n" +
                "        (SELECT date_part('year', entry_date) AS YEAR, date_part('month', entry_date) AS MONTH, " +
                " CAST(extract('day' FROM date_trunc('week', entry_date) - date_trunc('week', date_trunc('month', entry_date))) / 7 + 1 AS text) AS WEEK, " +
                " sum(sales_count) AS total_items_sold\n" +
                "         FROM product_sales\n" +
                "         INNER JOIN product_price USING (product_id)\n" +
                "         WHERE product_id = ?\n" +
                "         GROUP BY YEAR, MONTH, WEEK)) totals,\n" +
                "     product_price AS price_ref\n" +
                "WHERE product_id = ?\n" +
                "ORDER BY totals.year DESC, totals.month DESC NULLS LAST, totals.week DESC NULLS LAST";
    }
}
