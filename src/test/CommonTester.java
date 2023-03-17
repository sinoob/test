package test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.inventeon.framework.utils.InventeonDateUtils;
import com.inventeon.framework.utils.InventeonFormatterUtils;

public class CommonTester {

	public static void main1(String[] args) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMM");
		Calendar c = Calendar.getInstance();
		System.out.println(df.format(c.getTime()));
		int start = Integer.valueOf(df.format(c.getTime()));
		c.set(Calendar.MONTH, 0);
		System.out.println(df.format(c.getTime()));
		int end = Integer.valueOf(df.format(c.getTime()));
		System.out.println(end - start);
	}

	public static void main3(String[] args) {
		Calendar c = Calendar.getInstance();
		Date d1 = c.getTime();
		c.add(Calendar.MONTH, -5);
		Date d2 = c.getTime();
		System.out.println(InventeonDateUtils.getMonthsBetween(d1, d2));
	}
	
	public static void mai3n(String[] args) {

		Date d = new Date();
		int monthCount = 5;
		Map<String, Integer> monthMap = new HashMap<>();
		SimpleDateFormat df = new SimpleDateFormat("MMM-yyyy");
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.set(Calendar.DAY_OF_MONTH, 1);
		for(int i=0; i<monthCount; i++) {
			monthMap.put(df.format(c.getTime()), i);
			c.add(Calendar.MONTH, 1);
		}
		
		System.out.println(monthMap);
	}

	
	public static void main4(String[] args) throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String s = "2020-04-06";
		Date dt = df.parse(s);
		System.out.println(dt);
	}
	
	public static void main(String[] args) {
		System.out.println(InventeonFormatterUtils.indianFormatNoDecimals(333333333));
	}
}
