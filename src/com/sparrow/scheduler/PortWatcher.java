package com.sparrow.scheduler;

import java.util.Enumeration;

import javax.comm.CommPortIdentifier;

public class PortWatcher implements Runnable {

	@Override
	public void run() {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				System.out.println(portId.getName());
			}
		}
	}

}
