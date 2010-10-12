package org.meandre.client.utils;

/**
 *
 * @author Boris Capitanu
 *
 * @param <K> Key type
 * @param <V> Value type
 */

public class KeyValuePair<K,V> {
    private final K _key;
    private final V _value;

    public KeyValuePair(K key, V value) {
        _key = key;
        _value = value;
    }

    public K getKey() {
        return _key;
    }

    public V getValue() {
        return _value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof KeyValuePair<?, ?>))
            return false;

        KeyValuePair<?, ?> other = (KeyValuePair<?, ?>)obj;
        return (_key.equals(other._key) && _value.equals(other._value));
    }

    @Override
    public int hashCode() {
        return _key.hashCode() + 92821 * _value.hashCode();
    }

    @Override
    public String toString() {
        return String.format("key: %s\tvalue: %s", _key.toString(), _value.toString());
    }
}