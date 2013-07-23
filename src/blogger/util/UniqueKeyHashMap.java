package blogger.util;

import java.util.HashMap;

public final class UniqueKeyHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 3876720881586637165L;

	/**
	 * key must be unique.
	 * <p>
	 * {@inheritDoc}
	 * 
	 * @throws RuntimeException
	 *           if key already exists
	 */
	@Override
	public synchronized V put(K key, V value) {
		if (super.containsKey(key)) {
			throw new RuntimeException(String.format("key [%s] already exists", key));
		}
		else {
			super.put(key, value);
			return null;
		}
	}

}
