package li.strolch.plc.core.hw;

import java.util.Objects;

public class PlcAddress {

	public final PlcAddressType type;
	public final boolean virtual;

	public final String resource;
	public final String action;

	public final String address;
	public final PlcValueType valueType;
	public final Object defaultValue;
	public final boolean inverted;

	public PlcAddress(PlcAddressType type, boolean virtual, String resource, String action, String address,
			PlcValueType valueType, Object defaultValue, boolean inverted) {
		this.type = type;
		this.virtual = virtual;
		this.resource = resource.intern();
		this.action = action.intern();
		this.address = address.intern();
		this.valueType = valueType;
		this.defaultValue = defaultValue;
		this.inverted = inverted;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		PlcAddress that = (PlcAddress) o;

		if (!Objects.equals(resource, that.resource))
			return false;
		return Objects.equals(action, that.action);
	}

	@Override
	public int hashCode() {
		int result = resource != null ? resource.hashCode() : 0;
		result = 31 * result + (action != null ? action.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "PlcAddress [" + "type='" + type + '\'' + ", resource='" + resource + '\'' + ", action='" + action + '\''
				+ ", hwAddress='" + address + '\'' + ", valueType=" + valueType + ']';
	}
}
