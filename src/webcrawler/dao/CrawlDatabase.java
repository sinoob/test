package webcrawler.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CrawlDatabase {

	private static final CrawlDatabase THIS = new CrawlDatabase();
	private Connection conn = null;

	private CrawlDatabase() {
		try {
			// STEP 2: Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			// STEP 3: Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection("localhost", "root", "");
			System.out.println("Database created successfully...");
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		}
	}

	public static Connection getConnection() {
		return THIS.conn;
	}
}
