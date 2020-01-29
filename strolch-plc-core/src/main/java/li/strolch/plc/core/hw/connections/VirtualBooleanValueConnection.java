package li.strolch.plc.core.hw.connections;

import li.strolch.plc.core.hw.Plc;

public class VirtualBooleanValueConnection extends SimplePlcConnection {

	public VirtualBooleanValueConnection(Plc plc, String id) {
		super(plc, id);
	}

	@Override
	public void send(String address, Object value) {
		boolean bool = (boolean) value;
		logger.info("Setting address " + this.id + " to " + bool);
		this.plc.notify(this.id, bool);
	}
}