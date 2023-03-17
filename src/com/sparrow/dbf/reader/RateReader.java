package com.sparrow.dbf.reader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

import org.apache.commons.lang.StringUtils;

import com.nmdm.framework.constants.Literals;
import com.nmdm.framework.pool.SystemParameter;
import com.nmdm.soware.helper.DateHelper;
import com.nmdm.soware.vo.DailySalesVO;
import com.sparrow.dbf.DBFLiterals;
import com.sparrow.dbf.dto.ItemData;
import com.sparrow.nfr.SettingsManager;

public class RateReader {

	private final static String FILE_NAME = "STOCK.DAT";
	private final static String FIELD_CODE = "ITEMCODE";
	private final static String FIELD_NAME = "ITEMNAME";
	private final static String FIELD_MRP = "MRP";
	private final static String FIELD_RATE = "PRICE1";
	private final static String FIELD_SUPPLIER = "SUPPCODE";

	private String software, server;

	public RateReader(String software, String server) {
		this.software = software;
		this.server = server;
	}

	public Collection<ItemData> read(Collection<String> codes) throws IOException, ParseException {
		if ("GREENBOOK".equals(software)) {
			return readGreenBook(codes);
		}
		return readSoware(codes);
	}

	public Collection<ItemData> readSoware(Collection<String> codes) throws IOException, ParseException {
		Charset stringCharset = Charset.forName("Cp866");
		File file = new File(SettingsManager.instance().getProperty(DBFLiterals.DBF_PARENT_PATH) + FILE_NAME);
		DbfReader reader = new DbfReader(file);
		DbfRecord rec = null;
		List<ItemData> list = new ArrayList();
		while ((rec = reader.read()) != null) {
			rec.setStringCharset(stringCharset);
			String code = rec.getString(FIELD_CODE);
			if (!StringUtils.isEmpty(rec.getString(FIELD_SUPPLIER))) {
				if (codes.contains(code) && !rec.isDeleted()) {
					ItemData data = new ItemData("", "");
					data.setCode(code);
					data.setName(rec.getString(FIELD_NAME));
					data.setMrp(rec.getString(FIELD_MRP));
					data.setRate(rec.getString(FIELD_RATE));
					list.add(data);
				}
			}
		}
		reader.close();
		return list;
	}

	public Collection<ItemData> readGreenBook(Collection<String> codes) throws IOException, ParseException {

		Connection conn = null;
		Statement stmt = null;
		List<ItemData> list = new ArrayList<ItemData>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(server, "root", "");
			stmt = conn.createStatement();
			String sql;
			sql = "select itmcod, itmnam, mrp, slgprc from itmmst";
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery(sql);
			while (rs.next()) {
				String code = rs.getString("itmcod");
				if (codes.contains(code)) {
					ItemData data = new ItemData("", "");
					data.setCode(code);
					data.setName(rs.getString("itmnam"));
					data.setMrp(rs.getString("mrp"));
					data.setRate(rs.getString("slgprc"));
					list.add(data);
				}
			}
			rs.close();
			stmt.close();
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}// end finally try
		}// end try

		return list;
	}

}
