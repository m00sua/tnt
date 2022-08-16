package org.example.tnt.classes;

import lombok.Getter;
import org.springframework.util.Assert;


public class Bi<K, V> {

    @Getter
    private K key;

    @Getter
    private V value;


    public Bi(K key, V value) {
        Assert.notNull(key, "Key cannot be null");
        Assert.notNull(value, "Value cannot be null");
        this.key = key;
        this.value = value;
    }

}
