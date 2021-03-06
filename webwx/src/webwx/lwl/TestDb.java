package webwx.lwl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDb {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		   // 驱动程序名
        String driver = "com.mysql.jdbc.Driver";

        // URL指向要访问的数据库名scutcs
        String url = "jdbc:mysql://42.51.216.62:3308/webwx";

        // MySQL配置时的用户名
        String user = "webwx";

        // MySQL配置时的密码
        String password = "webwx";

        try {
         // 加载驱动程序
         Class.forName(driver);

         // 连续数据库
         Connection conn = DriverManager.getConnection(url, user, password);

         if(!conn.isClosed())
          System.out.println("Succeeded connecting to the Database!");

         // statement用来执行SQL语句
         Statement statement = conn.createStatement();

         // 要执行的SQL语句
         String sql = "select * from uuid_tab";

         // 结果集
         ResultSet rs = statement.executeQuery(sql);

         System.out.println("-----------------");
        

         String name = null;

         while(rs.next()) {

          // 选择sname这列数据
          name = rs.getString("uuid");

          // 首先使用ISO-8859-1字符集将name解码为字节序列并将结果存储新的字节数组中。
          // 然后使用GB2312字符集解码指定的字节数组
          name = new String(name.getBytes("ISO-8859-1"),"GB2312");

          // 输出结果
          System.out.println( "\t" + name);
         }

         rs.close();
         conn.close();

        } catch(ClassNotFoundException e) {


         System.out.println("Sorry,can`t find the Driver!");
         e.printStackTrace();


        } catch(SQLException e) {


         e.printStackTrace();


        } catch(Exception e) {


         e.printStackTrace();


        }
}

}
