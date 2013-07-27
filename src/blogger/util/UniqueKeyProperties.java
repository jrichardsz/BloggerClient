package blogger.util;

import java.util.Properties;

public final class UniqueKeyProperties extends Properties {
	private static final long serialVersionUID = -4131138357558217634L;

	public UniqueKeyProperties() {
	}

	public UniqueKeyProperties(Properties defaults) {
		super(defaults);
	}

	/**
	 * key must be unique.
	 * <p>
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException
	 *           if key already exists
	 */
	@Override
	public synchronized String setProperty(String key, String value) {
		return (String) this.put(key, value);
	}

	/**
	 * key must be unique.
	 * <p>
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException
	 *           if (1) key/value is not String type, (2) key already exists
	 */
	@Override
	public synchronized Object put(Object key, Object value) {
		if (!(key instanceof String && value instanceof String)) {
			throw new IllegalArgumentException("key and value must be String type");
		}
		if (super.containsKey(key)) {
			throw new IllegalArgumentException(String.format("key [%s] already exists", key));
		}
		else {
			super.put(key, value);
			return null;
		}
	}

}
