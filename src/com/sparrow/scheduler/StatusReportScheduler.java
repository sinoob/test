package com.sparrow.scheduler;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;

import com.nmdm.framework.constants.Literals;
import com.sparrow.nfr.SettingsManager;
import com.sparrow.reporter.SmsReporter;
import com.sparrow.webhost.WebSynchronizer;

public class StatusReportScheduler {

	private Enumeration portList;
	private CommPortIdentifier portId;
	private SerialPort serialPort;

	public static void main(String[] args) {

		StatusReportScheduler car = new StatusReportScheduler();
		car.initPort();
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		int delay = car.getDelayForNextHour();
		System.out.println(delay);
		MissCallHandler mt = new MissCallHandler();
		new Thread(mt).start();
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownHandler()));
		car.startMemoryUpdateSchedule(scheduler);

	}

	private int getDelayForNextHour() {
		Calendar now = Calendar.getInstance();
		return 60 - now.get(Calendar.MINUTE) + 15;
		// return 2;
	}

	private void startMemoryUpdateSchedule(ScheduledExecutorService service) {
		ScheduledFuture<?> future = service.scheduleWithFixedDelay(new SmsReporter(), getDelayForNextHour(), 60, TimeUnit.MINUTES);
		service.scheduleWithFixedDelay(new PortWatcher(), getDelayForNextHour() - 1, 60, TimeUnit.MINUTES);
		service.scheduleWithFixedDelay(new WebSynchronizer(), 5, 10, TimeUnit.SECONDS);
		
		try {
			future.get();
		} catch (ExecutionException e) {
			e.printStackTrace();
			future.cancel(true);
			System.out.println("phoenixxxxxxx");
			this.startMemoryUpdateSchedule(service);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initPort() {
		portList = CommPortIdentifier.getPortIdentifiers();
		String port = SettingsManager.instance().getProperty(Literals.SMS_PORT.toString());
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL && portId.getName().equals(port)) {
				try {
					serialPort = (SerialPort) portId.open("StatusReportScheduler", 2000);
					PortAllocator.getInstance().saveSerialPort(serialPort);
				} catch (PortInUseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
