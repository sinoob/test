package greenbook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * This class can be used only after the mdf file of xenia pos is attached to
 * the MS SQLSERVER Management studio.
 * 
 * @author root
 *
 */
public class AjwaDataMigrator {

	private Connection ajwaConn;
	private Connection gbConn;

	public AjwaDataMigrator() {
		String url = "jdbc:sqlserver://INVMACONE\\XENIASQL;databaseName=AJWA20182019;integratedSecurity=true";
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			ajwaConn = DriverManager.getConnection(url);

			Class.forName("com.mysql.jdbc.Driver");
			gbConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ajwa", "root", "");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<String, String> findItemDistributor() {
		StringBuilder sb = new StringBuilder();
		sb.append(" select  p.lid as suppliercode, i.itemid from  trspurchase p ");
		sb.append(" left outer join ");
		sb.append(" (select distinct itemid, max(invid) as invid from trsPurchasedetail d group by itemid) as i ");
		sb.append(" on i.invid = p.invid ");
		Map<String, String> map = new HashMap<String, String>();
		try {
			PreparedStatement stmt = ajwaConn.prepareStatement(sb.toString());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				map.put(rs.getString("itemid"), rs.getString("suppliercode"));
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public void test() {

		Map<String, String> map = findItemDistributor();
		StringBuilder select = new StringBuilder().append("select ");
		select.append(" m.prodId, m.prodName, m.Batch, m.mrp as masterMrp, m.sgstper, m.cgstper, ");
		select.append(" m.cessper, m.ptaxincl, m.staxincl, ");
		select.append(" m.prate as masterPRate, m.srate as masterSRate,");
		select.append(" m.mainUnit,");
		select.append(" s.Prate as childPRate, s.SRate as childSRate, s.mrp as childMrp, s.cost as childCost ");
		select.append(" from mtrProduct m left outer join (select * from trsStockQty where mrp > 0 and cost > 0) s on m.prodid = s.itemid  where m.prodid = 2214 order by prodid");
		StringBuilder sbinsert = new StringBuilder().append("insert into itmmst( ");
		sbinsert.append(" ITMCOD, ITMNAM, BARCOD, BARCODFLG, MRP, TAXPER, ");
		sbinsert.append(" CESPER, PURPRC, SLGPRC, WHLPRC, OWNPRC, ");
		sbinsert.append(" DSRCOD, ACTFLG, STKQTY, ");
		sbinsert.append(" RECSRC, UNT, PURRAT, SLGRAT, ");
		// sbinsert.append(" CRTTIM, LSTUPDTIM, CRTUSRCOD, LSTUPDUSRCOD, ");
		sbinsert.append(" CRTUSRCOD, LSTUPDUSRCOD, ");
		sbinsert.append(" SLGPRCPER, GRPIDR, ");
		// sbinsert.append(" HSNCOD, BLKSALFLG, BSTBFRDYS, ");
		sbinsert.append(" MIGITMCOD ) ");
		sbinsert.append("  values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		try {
			PreparedStatement stmt = ajwaConn.prepareStatement(select.toString());
			ResultSet rs = stmt.executeQuery();
			PreparedStatement toStmt = gbConn.prepareStatement(sbinsert.toString());
			gbConn.setAutoCommit(false);
			int count = 0;
			double srate, prate, mrp, cost;
			while (rs.next()) {

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}

	}

	private double getNonZero(double a, double b) {
		return (a == 0) ? b : a;
	}

	public void migrateMasterItems() {
		Map<String, String> map = findItemDistributor();
		StringBuilder select = new StringBuilder().append("select ");
		select.append(" m.prodId, m.prodName, m.Batch, m.mrp as masterMrp, m.sgstper, m.cgstper, ");
		select.append(" m.cessper, m.ptaxincl, m.staxincl, ");
		select.append(" m.prate as masterPRate, m.srate as masterSRate,");
		select.append(" m.mainUnit,");
		select.append(" s.Prate as childPRate, s.SRate as childSRate, s.mrp as childMrp, s.cost as childCost ");
		select.append(" from mtrProduct m left outer join (select * from trsStockQty where mrp > 0 and cost > 0) s on m.prodid = s.itemid order by prodid");
		StringBuilder sbinsert = new StringBuilder().append("insert into itmmst( ");
		sbinsert.append(" ITMCOD, ITMNAM, BARCOD, BARCODFLG, MRP, TAXPER, ");
		sbinsert.append(" CESPER, PURPRC, SLGPRC, WHLPRC, OWNPRC, ");
		sbinsert.append(" DSRCOD, ACTFLG, STKQTY, ");
		sbinsert.append(" RECSRC, UNT, PURRAT, SLGRAT, ");
		// sbinsert.append(" CRTTIM, LSTUPDTIM, CRTUSRCOD, LSTUPDUSRCOD, ");
		sbinsert.append(" CRTUSRCOD, LSTUPDUSRCOD, ");
		sbinsert.append(" SLGPRCPER, GRPIDR, ");
		// sbinsert.append(" HSNCOD, BLKSALFLG, BSTBFRDYS, ");
		sbinsert.append(" MIGITMCOD ) ");
		sbinsert.append("  values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		try {
			PreparedStatement stmt = ajwaConn.prepareStatement(select.toString());
			ResultSet rs = stmt.executeQuery();
			PreparedStatement toStmt = gbConn.prepareStatement(sbinsert.toString());
			gbConn.setAutoCommit(false);
			int count = 0;
			double srate, prate, mrp, cost;
			String prevProdId = "";
			int append = 0;
			while (rs.next()) {
				mrp = getNonZero(rs.getDouble("childMrp"), rs.getDouble("masterMrp"));
				srate = getNonZero(rs.getDouble("childSRate"), rs.getDouble("masterSRate"));
				prate = getNonZero(rs.getDouble("childPRate"), rs.getDouble("masterPRate"));
				cost = rs.getDouble("childCost");

				int i = 0;
				String prodid = rs.getString("prodId");
				append = prodid.equals(prevProdId) ? ++append : 0;
				prevProdId = prodid;
				if(append == 0){
					toStmt.setString(++i, 'A' + prodid);
				}else{
					toStmt.setString(++i, 'A' + prodid + '+' + append);
				}
				toStmt.setString(++i, rs.getString("prodName"));
				toStmt.setString(++i, rs.getString("Batch"));
				toStmt.setString(++i, "Y");
				toStmt.setDouble(++i, mrp);
				double taxper = rs.getDouble("sgstper") + rs.getDouble("cgstper");
				toStmt.setDouble(++i, taxper);
				toStmt.setDouble(++i, rs.getDouble("cessper"));
				boolean pincl = rs.getBoolean("ptaxincl");
				boolean sincl = rs.getBoolean("staxincl");
				if (cost == 0) {
					toStmt.setDouble(++i, pincl ? prate : prate + (prate * taxper / 100));
				} else {
					toStmt.setDouble(++i, cost);
				}
				double slgprc = sincl ? srate : srate + (srate * taxper / 100);
				toStmt.setDouble(++i, slgprc);
				toStmt.setDouble(++i, slgprc);
				toStmt.setDouble(++i, slgprc);
				toStmt.setString(++i, map.get(prodid));
				toStmt.setString(++i, "Y");
				toStmt.setDouble(++i, 0);
				toStmt.setString(++i, "MIG");
				String unit = rs.getString("mainUnit");
				toStmt.setString(++i, "KG".equalsIgnoreCase(unit) ? "KG" : "PKT");
				toStmt.setDouble(++i, 0);
				toStmt.setDouble(++i, 0);
				toStmt.setString(++i, "SYSTEM");
				toStmt.setString(++i, "SYSTEM");
				toStmt.setDouble(++i, 0);
				toStmt.setDouble(++i, 0);
				toStmt.setString(++i, prodid);
				toStmt.addBatch();
				count++;
				if (count % 300 == 0) {
					toStmt.executeBatch();
					System.out.println("Batch execution " + count / 300);
				}
			}
			toStmt.executeBatch();
			gbConn.commit();
			System.out.println("Completed migrateMasterItems.");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
	}

	public void migrateAccountMaster() {
		String select = " SELECT lid, ledgername, contactno, phoneno, address, tinno FROM mtrledger  where accid = 10 ";
		StringBuilder sb = new StringBuilder();
		sb.append(" INSERT INTO  actmst  ( ");
		sb.append(" ACTCOD ,  ACTNAM ,  ADR ,  PHNNUM ,  ");
		sb.append(" GSTNUM , CDTDYS,  ");
		sb.append(" ACTFLG ,   ACTTYP ,  RECSRC ,  CRTUSRCOD ,  LSTUPDUSRCOD )");
		sb.append(" values (?,?,?,?,?,?,?,?,?,?,?)");

		try {
			gbConn.setAutoCommit(false);
			PreparedStatement stmt = ajwaConn.prepareStatement(select);
			ResultSet rs = stmt.executeQuery();
			PreparedStatement toStmt = gbConn.prepareStatement(sb.toString());
			int count = 0;
			while (rs.next()) {
				int i = 0;
				toStmt.setString(++i, rs.getString("lid"));
				toStmt.setString(++i, rs.getString("ledgername"));
				String add = rs.getString("address");
				if (add != null && add.length() > 100) {
					add = add.substring(0, 100);
				}
				toStmt.setString(++i, add);
				String phone = rs.getString("contactno");
				if (StringUtils.isEmpty(phone)) {
					toStmt.setString(++i, rs.getString("phoneno"));
				} else {
					toStmt.setString(++i, phone);
				}
				toStmt.setString(++i, rs.getString("tinno"));
				toStmt.setInt(++i, 0);
				toStmt.setString(++i, "Y");
				toStmt.setString(++i, "SUPPLIER");
				toStmt.setString(++i, "MIG");
				toStmt.setString(++i, "SYSTEM");
				toStmt.setString(++i, "SYSTEM");
				toStmt.addBatch();
				count++;
				if (count % 300 == 0) {
					toStmt.executeBatch();
					System.out.println("Batch execution " + count / 300);
				}
			}
			toStmt.executeBatch();
			gbConn.commit();
			System.out.println("Completed migrateAccountMaster.");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
	}

	private List<String> findItemCodes(String query, Connection conn) {
		List<String> list = new ArrayList<>();
		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				list.add(rs.getString(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public List<String> findNewlyAddedItems() {
		String selectAjwa = "select prodid from mtrProduct ";
		String selectGb = "select MIGITMCOD from itmmst ";
		List<String> ajwalist = findItemCodes(selectAjwa, ajwaConn);
		List<String> gblist = findItemCodes(selectGb, gbConn);
		ajwalist.removeAll(gblist);
		return ajwalist;

	}

	public List<String> findNewlyAddedMrps() {
		String selectAjwa = "select batch + '_' +ltrim(str(mrp)) from trsStockQty ";
		String selectGb = "select concat(barcod, '_', convert(truncate(mrp,0), char(50))) from itmmst   ";
		List<String> ajwalist = findItemCodes(selectAjwa, ajwaConn);
		List<String> gblist = findItemCodes(selectGb, gbConn);
		ajwalist.removeAll(gblist);
		return ajwalist;
		
	}
	
	public void findItemsSoldAfterStockTaking() {

	}
	

}
