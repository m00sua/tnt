package org.example.tnt.clients;

import lombok.Getter;
import lombok.Setter;


public class Wrapper<T> {

    @Getter
    @Setter
    private T object;

    public Wrapper() {
        object = null;
    }

}
