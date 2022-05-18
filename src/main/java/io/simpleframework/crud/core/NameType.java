package io.simpleframework.crud.core;

import io.simpleframework.crud.util.SimpleCrudUtils;

import java.util.Locale;


public enum NameType {
    
    CLASS_DEFINED {
        @Override
        public String trans(String name) {
            throw new IllegalArgumentException("NameType is not support " + CLASS_DEFINED);
        }
    },
    
    NOOP {
        @Override
        public String trans(String name) {
            return name;
        }
    },
    
    UNDERLINE_UPPER_CASE {
        @Override
        public String trans(String name) {
            return SimpleCrudUtils.camelToUnderline(name).toUpperCase(Locale.ENGLISH);
        }
    },
    
    UNDERLINE_LOWER_CASE {
        @Override
        public String trans(String name) {
            return SimpleCrudUtils.camelToUnderline(name).toLowerCase(Locale.ENGLISH);
        }
    };

    public abstract String trans(String name);

}
