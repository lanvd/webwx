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

		   // ����������
        String driver = "com.mysql.jdbc.Driver";

        // URLָ��Ҫ���ʵ����ݿ���scutcs
        String url = "jdbc:mysql://42.51.216.62:3308/webwx";

        // MySQL����ʱ���û���
        String user = "webwx";

        // MySQL����ʱ������
        String password = "webwx";

        try {
         // ������������
         Class.forName(driver);

         // �������ݿ�
         Connection conn = DriverManager.getConnection(url, user, password);

         if(!conn.isClosed())
          System.out.println("Succeeded connecting to the Database!");

         // statement����ִ��SQL���
         Statement statement = conn.createStatement();

         // Ҫִ�е�SQL���
         String sql = "select * from uuid_tab";

         // �����
         ResultSet rs = statement.executeQuery(sql);

         System.out.println("-----------------");
        

         String name = null;

         while(rs.next()) {

          // ѡ��sname��������
          name = rs.getString("uuid");

          // ����ʹ��ISO-8859-1�ַ�����name����Ϊ�ֽ����в�������洢�µ��ֽ������С�
          // Ȼ��ʹ��GB2312�ַ�������ָ�����ֽ�����
          name = new String(name.getBytes("ISO-8859-1"),"GB2312");

          // ������
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
