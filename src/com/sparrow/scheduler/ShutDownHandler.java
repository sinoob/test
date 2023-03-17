package com.sparrow.scheduler;

import com.sparrow.reporter.SmsReporter;

public class ShutDownHandler implements Runnable {

	@Override
	public void run() {
		System.out.println("Running Shutdown Handler");
		new SmsReporter().generateAndSendSms("Shutdown");
	}
}
