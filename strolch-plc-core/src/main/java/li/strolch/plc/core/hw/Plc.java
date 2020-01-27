package li.strolch.plc.core.hw;

public interface Plc {

	void start();

	void stop();

	void registerListener(PlcAddress address, PlcListener listener);

	void unregisterListener(PlcAddress address, PlcListener listener);

	void notify(String address, Object value);

	void send(PlcAddress address);

	void send(PlcAddress address, Object value);

	void addConnection(PlcConnection connection);

	PlcConnection getConnection(String id);

	PlcConnection getConnection(PlcAddress address);

	void registerNotificationMapping(PlcAddress address);

	void notifyConnectionStateChanged(PlcConnection connection);

	void setConnectionStateChangeListener(PlcConnectionStateChangeListener listener);
}