package li.strolch.plc.core.hw.i2c;

import static li.strolch.utils.helper.ByteHelper.*;
import static li.strolch.utils.helper.ExceptionHelper.getExceptionMessageWithCauses;
import static li.strolch.utils.helper.StringHelper.toHexString;
import static li.strolch.utils.helper.StringHelper.toPrettyHexString;

import java.util.*;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import li.strolch.plc.core.hw.Plc;
import li.strolch.plc.core.hw.connections.SimplePlcConnection;
import li.strolch.plc.model.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCF8574OutputConnection extends SimplePlcConnection {

	private static final Logger logger = LoggerFactory.getLogger(PCF8574OutputConnection.class);

	private int i2cBusNr;
	private boolean inverted;

	private byte[] addresses;
	private I2CDevice[] outputDevices;
	private byte[] states;

	private Map<String, int[]> positionsByAddress;

	public PCF8574OutputConnection(Plc plc, String id) {
		super(plc, id);
	}

	@Override
	public void initialize(Map<String, Object> parameters) {

		if (!parameters.containsKey("i2cBus"))
			throw new IllegalArgumentException("Missing param i2cBus");
		if (!parameters.containsKey("addresses"))
			throw new IllegalArgumentException("Missing param addresses");

		this.i2cBusNr = (int) parameters.get("i2cBus");
		this.inverted = parameters.containsKey("inverted") && (boolean) parameters.get("inverted");

		@SuppressWarnings("unchecked")
		List<Integer> addressList = (List<Integer>) parameters.get("addresses");
		this.addresses = new byte[addressList.size()];
		for (int i = 0; i < addressList.size(); i++) {
			this.addresses[i] = addressList.get(i).byteValue();
		}

		Map<String, int[]> positionsByAddress = new HashMap<>();
		for (int i = 0; i < this.addresses.length; i++) {
			for (int j = 0; j < 8; j++)
				positionsByAddress.put(this.id + "." + i + "." + j, new int[] { i, j });
		}
		this.positionsByAddress = Collections.unmodifiableMap(positionsByAddress);

		logger.info("Configured PCF8574 Output on I2C addresses 0x " + toPrettyHexString(this.addresses));
	}

	@Override
	public boolean connect() {
		if (isConnected()) {
			logger.warn(this.id + ": Already connected");
			return true;
		}

		logger.info(this.id + ": Connecting...");

		// initialize
		try {
			I2CBus i2cBus = I2CFactory.getInstance(i2cBusNr);

			this.outputDevices = new I2CDevice[this.addresses.length];
			this.states = new byte[this.addresses.length];
			byte[] bytes = this.addresses;
			boolean ok = true;
			for (int i = 0; i < bytes.length; i++) {
				byte address = bytes[i];

				this.outputDevices[i] = i2cBus.getDevice(address);

				// default is all outputs off, i.e. 1
				this.states[i] = (byte) 0xff;
				try {
					this.outputDevices[i].write(this.states[i]);
					logger.info(
							"Set initial value to " + asBinary((byte) 0xff) + " for address 0x" + toHexString(address));
				} catch (Exception e) {
					ok = false;
					logger.error(
							"Failed to set initial value to " + asBinary((byte) 0xff) + " on I2C Bus " + this.i2cBusNr
									+ " and address 0x" + toHexString(address), e);
				}

				logger.info("Connected to I2C Device at address 0x" + toHexString(address) + " on I2C Bus "
						+ this.i2cBusNr);
			}

			if (ok)
				return super.connect();

			handleBrokenConnection(
					"Failed to set initial values to " + asBinary((byte) 0xff) + " on I2C Bus " + this.i2cBusNr
							+ " and addresses 0x " + toPrettyHexString(this.addresses), null);
			return false;

		} catch (Throwable e) {
			handleBrokenConnection(
					"Failed to connect to I2C Bus " + this.i2cBusNr + " and addresses 0x " + toPrettyHexString(
							this.addresses) + ": " + getExceptionMessageWithCauses(e), e);

			return false;
		}
	}

	@Override
	public void disconnect() {
		this.outputDevices = null;
		this.states = null;

		this.connectionState = ConnectionState.Disconnected;
		this.connectionStateMsg = "-";
		this.plc.notifyConnectionStateChanged(this);
	}

	@Override
	public Set<String> getAddresses() {
		return new TreeSet<>(this.positionsByAddress.keySet());
	}

	@Override
	public void send(String address, Object value) {
		assertConnected();

		int[] pos = this.positionsByAddress.get(address);
		if (pos == null)
			throw new IllegalStateException("Address is illegal " + address);

		int device = pos[0];
		int pin = pos[1];

		boolean high = (boolean) value;

		// see if we need to invert
		if (this.inverted)
			high = !high;

		try {
			byte newState;
			if (high)
				newState = clearBit(this.states[device], pin);
			else
				newState = setBit(this.states[device], pin);

			this.outputDevices[device].write(newState);
			this.states[device] = newState;
		} catch (Exception e) {
			handleBrokenConnection(
					"Failed to write to I2C address: " + address + ": " + getExceptionMessageWithCauses(e), e);
			throw new IllegalStateException("Failed to write to I2C address " + address, e);
		}
	}

	@Override
	public String toString() {
		return this.id;
	}
}
