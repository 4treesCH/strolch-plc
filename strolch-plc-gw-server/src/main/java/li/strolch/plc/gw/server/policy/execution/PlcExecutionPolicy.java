package li.strolch.plc.gw.server.policy.execution;

import static li.strolch.plc.gw.server.PlcServerContants.BUNDLE_STROLCH_PLC_GW_SERVER;
import static li.strolch.runtime.StrolchConstants.SYSTEM_USER_AGENT;

import java.util.HashSet;
import java.util.Set;

import li.strolch.execution.policy.SimpleExecution;
import li.strolch.model.log.LogMessage;
import li.strolch.model.log.LogMessageState;
import li.strolch.model.log.LogSeverity;
import li.strolch.model.activity.Action;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.plc.gw.server.PlcAddressResponseListener;
import li.strolch.plc.gw.server.PlcGwServerHandler;
import li.strolch.plc.model.PlcAddressKey;
import li.strolch.plc.model.PlcAddressResponse;
import li.strolch.plc.gw.server.PlcNotificationListener;

public abstract class PlcExecutionPolicy extends SimpleExecution
		implements PlcNotificationListener, PlcAddressResponseListener {

	protected PlcGwServerHandler plcHandler;
	protected Set<PlcAddressKey> registeredKeys;

	public PlcExecutionPolicy(StrolchTransaction tx) {
		super(tx);
		this.registeredKeys = new HashSet<>();
	}

	protected abstract String getPlcId();

	@Override
	public void initialize(Action action) {
		super.initialize(action);
		this.plcHandler = getComponent(PlcGwServerHandler.class);
	}

	public String getActionType() {
		return this.actionType;
	}

	protected void register(PlcAddressKey key) {
		this.plcHandler.register(getPlcId(), key, this);
		this.registeredKeys.add(key);
	}

	protected void unregisterAll() {
		this.registeredKeys.forEach(k -> this.plcHandler.unregister(getPlcId(), k, this));
	}

	@Override
	protected void handleStopped() {
		unregisterAll();
		super.handleStopped();
	}

	protected void toExecuted() throws Exception {
		stop();
		getController().toExecuted(this.actionLoc);
	}

	protected boolean isPlcConnected() {
		return this.plcHandler.isPlcConnected(getPlcId());
	}

	protected boolean assertPlcConnected() {
		if (this.plcHandler.isPlcConnected(getPlcId()))
			return true;

		toError(msgPlcNotConnected());
		return false;
	}

	protected boolean assertResponse(PlcAddressResponse response) {
		if (response.getState().isSent())
			return true;

		toError(msgFailedToSendMessage(response));
		return false;
	}

	protected void send(PlcAddressKey key, boolean value) {
		this.plcHandler.sendMessage(key, getPlcId(), value, this);
	}

	protected void send(PlcAddressKey key, int value) {
		this.plcHandler.sendMessage(key, getPlcId(), value, this);
	}

	protected void send(PlcAddressKey key, double value) {
		this.plcHandler.sendMessage(key, getPlcId(), value, this);
	}

	protected void send(PlcAddressKey key, String value) {
		this.plcHandler.sendMessage(key, getPlcId(), value, this);
	}

	protected void send(PlcAddressKey key) {
		this.plcHandler.sendMessage(key, getPlcId(), this);
	}

	@Override
	public void handleResponse(PlcAddressResponse response) throws Exception {
		assertResponse(response);
	}

	@Override
	public abstract void handleNotification(PlcAddressKey key, Object value) throws Exception;

	@Override
	public void handleConnectionLost() {
		toError(msgConnectionLostToPlc());
	}

	protected LogMessage msgPlcNotConnected() {
		return new LogMessage(this.realm, SYSTEM_USER_AGENT, this.actionLoc, LogSeverity.Error,
				LogMessageState.Information, BUNDLE_STROLCH_PLC_GW_SERVER, "execution.plc.notConnected").value("plc", getPlcId());
	}

	protected LogMessage msgFailedToSendMessage(PlcAddressResponse response) {
		PlcAddressKey key = response.getPlcAddressKey();
		return new LogMessage(this.realm, SYSTEM_USER_AGENT, this.actionLoc, LogSeverity.Error,
				LogMessageState.Information, BUNDLE_STROLCH_PLC_GW_SERVER, "execution.plc.sendMessage.failed") //
				.value("plc", getPlcId()) //
				.value("key", key) //
				.value("msg", response.getStateMsg());
	}

	protected LogMessage msgConnectionLostToPlc() {
		return new LogMessage(this.realm, SYSTEM_USER_AGENT, this.actionLoc, LogSeverity.Error,
				LogMessageState.Information, BUNDLE_STROLCH_PLC_GW_SERVER, "execution.plc.connectionLost").value("plc", getPlcId());
	}
}
