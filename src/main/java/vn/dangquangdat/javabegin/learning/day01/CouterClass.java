package vn.dangquangdat.javabegin.learning.day01;

public class CouterClass {

    static void increment(Counter counter) {
        counter.i = 100;
    }

    static void replace(Counter counter) {
        counter = new Counter();
        counter.i = 5;
    }

    static class Counter {

        protected int i = 0;
    }

    public static void main(String[] args) {
        Counter ct = new Counter();
        System.out.println(ct.i);

        replace(ct);
        System.out.println(ct.i);

    }
}
