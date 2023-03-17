package com.sparrow.reporter;

import java.util.Calendar;

import com.nmdm.framework.constants.ConstantLiterals;
import com.nmdm.framework.constants.Literals;
import com.sparrow.nfr.SettingsManager;
import com.sparrow.scheduler.PortAllocator;
import com.sparrow.sms.SmsEngine;
import com.sparrow.soware.DailySalesStats;

public class SmsReporter implements Runnable {

	@Override
	public void run() {
		Calendar c = Calendar.getInstance();
		boolean sendThisHour = false;
		String smsHours[] = SettingsManager.instance().getProperty(Literals.SMS_HOURS.toString()).split(ConstantLiterals.COMMA);
		String currentHour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
		for (String smsHour : smsHours) {
			if (currentHour.equals(smsHour)) {
				sendThisHour = true;
			}
		}

		System.out.println("Current time : " + currentHour + ":" + c.get(Calendar.MINUTE));
		System.out.println("Sending this hour : " + sendThisHour);
		
		
		if (sendThisHour) {
			generateAndSendSms("Timer");
		}
	}

	public void generateAndSendSms(String owner) {
		try {
			System.out.println("SmsReporter invoked...");
			String message = new DailySalesStats().generateDailySalesStats();
			System.out.println(message);
			String[] numbers = SettingsManager.instance().getProperty(Literals.SMS_NUMBERS.toString()).split(ConstantLiterals.COMMA);
			for (String number : numbers) {
				try {
					System.out.println("sending sms to " + number);
					new SmsEngine().sendTextModeSms(number, message + "\n" + owner);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
