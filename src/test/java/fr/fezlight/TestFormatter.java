package fr.fezlight;

import java.util.function.Function;

public class TestFormatter implements Function<String, Integer> {
    @Override
    public Integer apply(String s) {
        int i = Integer.parseInt(s);
        return i + 40;
    }
}
