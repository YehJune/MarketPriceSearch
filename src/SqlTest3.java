import java.sql.*;
import java.io.*;
import java.lang.reflect.Executable;
import java.util.Scanner;
 
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import java.awt.geom.Point2D;

public class SqlTest3{
    private Connection connect = null;
    private Statement stmt = null;
    private ResultSet rs = null;
    private ResultSetMetaData rsMetaData = null;

    private final String url = "jdbc:postgresql://localhost:5433/postgres";
    private final String user = "postgres";
    private final String password = "2964";
    /**
     * @param args
     * @throws SQLException
     */
    public static void main(String[] args) throws SQLException {
        try {
            Scanner scan = new Scanner(System.in);
            SqlTest3 app = new SqlTest3();

            System.out.println("신선식품 가격정보 제공 서비스\n");

            //connect
            app.connection();

            //조회하고자 하는 위치정보 입력받는다.
            System.out.println("\n마트 정보를 조회하고자하는 위치의 도로명 주소를 입력하세요. ");
            String inputRN = scan.nextLine();

            app.stmt.executeUpdate("create table search_range as select* from address where rn = '"+inputRN+"';");

            boolean loop=true;
            //범위 내 점포 정보 조회
            while (loop){
                loop=false;

                //조회하고자 하는 범위(맨해튼 거리) 입력받는다.
                System.out.println("\n해당 위치에서몇 m 이내 마트를 조회할지를 입력하세요. (맨해튼 거리로 계산됩니다. 단위는 제외하고 입력해주세요.");
                String inputRange = scan.nextLine();

                app.stmt.executeUpdate("update search_range "+
                        "set x_min = x_min-"+inputRange+","+
                        " y_min=y_min-"+inputRange+", "+
                        "x_max=x_max+"+inputRange+","+
                        " y_max=y_max+"+inputRange+";");
                app.stmt.executeUpdate("create table market_in_range "+
                        "as select * from market, search_range "+
                        "WHERE market.\"DTLSTATEGBN\"= 1 and "+
                        "search_range.x_min <= market.\"X\" and "+
                        "search_range.y_min <= market.\"Y\" "+
                        "and market.\"X\" <= search_range.x_max and "+
                        "market.\"Y\" <= search_range.y_max;");
                app.rs = app.stmt.executeQuery("select count (*) from market_in_range;");
                while (app.rs.next()) {
                    if (app.rs.getString("count").equals("0"))
                    {System.out.println("\n범위 내에 마트정보가 없습니다. 거리를 더 길게 설정해주세요.");
                    loop = true;
                     app.stmt.executeUpdate("drop table market_in_range;");
                     break;
                    }
                }
            }
            app.rs = app.stmt.executeQuery("select * from market_in_range; ");
            app.printQ(8);

            //조회하고자 하는 품목명을 입력받는다.
            System.out.println("\n조회하고자하는 품목의 이름을 입력하세요.");
            String inputA_Name = scan.nextLine();

            //범위 내 점포에서 판매하는 품목의 실판매 규격 정보 출력
            String task2 = "select distinct A_NAME, A_UNIT, BPLCNM\n" +
                    "from market_in_range, Item\n" +
                    "where \n" +
                    "A_NAME similar to '%" + inputA_Name + "%' and\n" +
                    "BPLCNM similar to M_NAME;";
            app.rs = app.stmt.executeQuery(task2);
            while (app.rs.next()) {
                System.out.print(app.rs.getString("A_NAME"));
                System.out.print("\t");
                System.out.print(app.rs.getString("A_UNIT"));
                System.out.print("\t");
                System.out.print(app.rs.getString("BPLCNM"));
                System.out.print("\n\n");
            }

            //조회하고자 하는 품목명과 실판매 규격을 입력받는다.
            System.out.println("\n조회하고자하는 품목의 실판매 규격을 입력하세요.");
            String inputA_Unit = scan.nextLine();

            //해당 항목의 마트명, 가격, 품목명, 실판매규격, 전화번호, 도로명주소, 업태구분명을 오름차순으로 정렬
            String task3 = "select M_NAME, A_PRICE, A_NAME, A_UNIT,\n" +
                    "    SITETEL, RDNWHLADDR, UPTAENM\n" +
                    "from Item, market_in_range\n" +
                    "where M_NAME similar to BPLCNM and \n" +
                    "    A_NAME similar to '%" + inputA_Name + "%' and \n" +
                    "    A_UNIT = '"+ inputA_Unit +"'\n" +
                    "    order by A_PRICE asc;";
            app.rs = app.stmt.executeQuery(task3);
            while (app.rs.next()) {
                System.out.print(app.rs.getString("M_NAME"));
                System.out.print("\t");
                System.out.print(app.rs.getString("A_PRICE"));
                System.out.print("\t");
                System.out.print(app.rs.getString("A_NAME"));
                System.out.print("\t");
                System.out.print(app.rs.getString("A_UNIT"));
                System.out.print("\t");
                System.out.print(app.rs.getString("SITETEL"));
                System.out.print("\t");
                System.out.print(app.rs.getString("RDNWHLADDR"));
                System.out.print("\t");
                System.out.print(app.rs.getString("UPTAENM"));
                System.out.print("\n\n");
            }

            //user입력에 따라 새로 만들어진 table drop
            app.stmt.executeQuery("drop table search_range, market_in_range");

            //close
            app.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    //connect
    public void connection() throws SQLException {
        try {
            connect = DriverManager.getConnection(url, user, password);
            stmt = connect.createStatement();
        } catch (SQLException ex) {
            throw ex;
        }
    }
//connection close
    public void close() throws SQLException {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (connect != null) {
                connect.close();
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }

    public void executeQ(String q) throws SQLException {
        try {
            rs = stmt.executeQuery(q);
        } catch (SQLException ex) {
            throw ex;
        }
    }
    public void printQ(int colNum) throws SQLException {
        try {
            rsMetaData = rs.getMetaData();
            // display Column name
            System.out.printf("\t");
            for (int i = 1; i < colNum + 1; i++) {
                System.out.printf(rsMetaData.getColumnName(i) + "\t");
            }
            System.out.println("");
            // display result
            int tupleNum = 1;
            while (rs.next()) {
                System.out.printf(tupleNum + "\t");
                for (int i = 1; i < colNum + 1; i++) {
                    String columnValue = rs.getString(i);
                    System.out.printf(columnValue + "\t");
                }
                System.out.println("");
                tupleNum++;
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }
}