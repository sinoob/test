package com.nmdm.soware.reader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

import com.nmdm.framework.constants.BillType;
import com.nmdm.framework.constants.ConstantLiterals;
import com.nmdm.framework.constants.Literals;
import com.nmdm.framework.pool.SystemParameter;
import com.nmdm.soware.helper.DateHelper;
import com.nmdm.soware.vo.DailySalesVO;
import com.sparrow.dbf.DBFLiterals;
import com.sparrow.nfr.SettingsManager;

public class DailySalesReader {

	private final static String FILE_NAME = "DSALES.DAT";
	private final static String FIELD_BILLNO = "ORDERNO";
	private final static String FIELD_USER = "USERID";
	private final static String FIELD_DATE = "DATE";
	private final static String FIELD_AMOUNT = "BILLAMT";
	private final static String FIELD_BILL_TYPE = "BILLTYPE";
	private final static String FIELD_CUST_CODE = "CUSTCODE";
	private final static String FIELD_CC_NUMBER = "CREDCARD";

	public List<DailySalesVO> read(String userId, Date date) throws IOException, ParseException {
		return parse(userId, date);
	}

	public List<DailySalesVO> read(Date date) throws IOException, ParseException {
		return parse(null, date);
	}

	private List<DailySalesVO> parse(String userId, Date date) throws IOException, ParseException {
		Charset stringCharset = Charset.forName("Cp866");
		File file = new File(SettingsManager.instance().getProperty(DBFLiterals.DBF_PARENT_PATH) + FILE_NAME);
		DbfReader reader = new DbfReader(file);
		DbfRecord rec = null;
		Date dbDate = null;
		Date sdate = null;
		Date edate = null;
		String user = null;
		List<DailySalesVO> list = new ArrayList<DailySalesVO>();
		DailySalesVO vo = null;
		String cancelCode = "C";
		if (date != null) {
			sdate = DateHelper.getStartOfDay(date);
			edate = DateHelper.getEndOfDay(date);
		}
		while ((rec = reader.read()) != null) {
			rec.setStringCharset(stringCharset);
			user = rec.getString(FIELD_USER);
			if (!rec.isDeleted() && !cancelCode.equals(rec.getString(FIELD_CUST_CODE))) {
				if (date != null) {
					try{
						
					dbDate = rec.getDate(FIELD_DATE);
					if (dbDate.before(sdate) || dbDate.after(edate)) {
						continue;
					}
					}catch(Exception e) {
						continue;
					}
				}
				if (user != null && userId != null && !user.endsWith(userId)) {
					continue;
				}
				vo = new DailySalesVO();
				vo.setUser(user.substring(user.lastIndexOf(ConstantLiterals.SPACE) + 1));
				vo.setAmount(rec.getDouble(FIELD_AMOUNT));
				vo.setRoundedAmount(Math.floor(rec.getDouble(FIELD_AMOUNT) + 0.5));
				vo.setBillType(BillType.values()[Integer.valueOf(rec.getString(FIELD_BILL_TYPE)) - 1]);
				vo.setCreditCardPayment(rec.getString(FIELD_CC_NUMBER) != null);
				list.add(vo);
			}
		}
		reader.close();
		return list;
	}
}
