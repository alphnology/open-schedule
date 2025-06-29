package com.alphnology.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author me@fredpena.dev
 * @created 14/06/2025  - 19:06
 */
@Getter
@Setter
public class Country implements Comparable<Country> {

    private String name;
    private String code;

    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }


    @Override
    public int compareTo(Country other) {
        return this.getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return Objects.equals(code, country.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
