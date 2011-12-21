package dataaccess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class DataAccess {

	private static DataAccess ref;
		
	/**
	 * Private Constructor, necessary for Singleton pattern. 
	 *
	 */
	private DataAccess() {
		
	}
	
	
	public static DataAccess getDataAccessMgr() {
		if (ref == null) {
			ref = new DataAccess();
		}
		return ref;
	}
	
	/**
	 * connect to the database
	 * connection string: jdbc:mysql://140.78.73.87:3306/monitoringDB
	 * user: uece
	 * password: 2011WSceue
	 * @return a Connection object representing the database connection, null, if connection has not been established
	 */
	private Connection getConnection() {
		Connection cn = null;
		System.out.println("Connecting to Database ...");
//		try {
			// insert driver load statement here

//		} catch (ClassNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		try {
			// insert connection request statement here

//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return cn;
	}
	
	/**
	 * Demo method 4 printing table info and content
	 * available table: order
	 * 
	 * @param table
	 */
	public void jdbcDemo(String table) {
		Connection cn = this.getConnection();
		try {
			Statement st = cn.createStatement();
			ResultSet rs = st.executeQuery("SELECT * from "+table);
	        // Get meta data:
	        ResultSetMetaData rsmd = rs.getMetaData();
	        int i, n = rsmd.getColumnCount();
	        // Print table content:
	        for( i=0; i<n; i++ )
	          System.out.print( "+---------------" );
	        System.out.println( "+" );
	        for( i=1; i<=n; i++ )    // Attention: first column with 1 instead of 0
	          System.out.format( "| %-14s", rsmd.getColumnName(i));
	        System.out.println( "|" );
	        for( i=0; i<n; i++ )
	          System.out.print( "+---------------" );
	        System.out.println( "+" );
	        while( rs.next() ) {
	          for( i=1; i<=n; i++ )  // Attention: first column with 1 instead of 0
	            System.out.format( "| %-14s", rs.getString(i));
	          System.out.println( "|" );
	        }
	        
	        for( i=0; i<n; i++ )
	          System.out.print( "+---------------" );
	        System.out.println( "+" );
			rs.close();
			st.close();
			cn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		//....
	}
	
}
