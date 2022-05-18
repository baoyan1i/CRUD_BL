package io.simpleframework.crud.domain;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;


@Getter
public abstract class BaseDomain {

    private final Set<String> updatedValueObjectNames = new HashSet<>();

    
    protected void markValueObjectUpdated(String name) {
        this.updatedValueObjectNames.add(name);
    }

}
