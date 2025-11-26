package net.lerariemann.infinity.util.registry;

public class RuleUtils {
    public static int normalize(int x, int size) {
        int a = Math.abs(x < 0 ? x+1 : x) % size;
        return (x < 0) ? size - 1 - a : a;
    }

}
