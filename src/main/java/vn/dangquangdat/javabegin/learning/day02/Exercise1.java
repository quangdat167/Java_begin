package vn.dangquangdat.javabegin.learning.day02;

public class Exercise1 {

    public static void main(String[] arg) {
        for (int i = 1; i <= 100; i++) {
            if (i % 5 == 0 && i % 3 == 0) {
                System.out.println("FizzBuzz");
            } else if (i % 5 == 0) {
                System.out.println("Buzz");
            } else if (i % 3 == 0) {
                System.out.println("Fizz");
            } else {
                System.out.println(i);
            }
        }

        System.out.println(classify(0));     // INACTIVE
        System.out.println(classify(1));     // NORMAL
        System.out.println(classify(999));   // NORMAL
        System.out.println(classify(1000));  // HEAVY
        System.out.println(classify(5000));  // HEAVY

        int[] normal = {7, -2, 15, 4, 9};
        // min=-2, max=15, sum=33

        int[] negatives = {-8, -3, -12};
        // min=-12, max=-3, sum=-23

        int[] single = {5};
        // min=5, max=5, sum=5

        int[] large = {
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        };
        System.out.println("min: " + findMin(large));
        System.out.println("max: " + findMax(large));
        System.out.println("sum: " + calculateSum(large));
    }

    static String classify(long requestCount) {
        if (requestCount < 0) {
            throw new IllegalArgumentException("request must >= 0");
        }
        if (requestCount == 0) {
            return "INACTIVE";
        }
        if (requestCount < 1000) {
            return "NORMAL";
        }
        return "HEAVY";
    }

    static int findMin(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Array not null!");
        }
        int min = numbers[0];
        for (int i : numbers) {
            if (i < min) {
                min = i;
            }
        }
        return min;
    }

    static int findMax(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Array not null!");
        }
        int max = numbers[0];
        for (int i : numbers) {
            if (max < i) {
                max = i;
            }
        }
        return max;
    }

    static long calculateSum(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Array not null!");
        }
        long sum = 0L;
        for (int i : numbers) {
            sum += i;
        }
        return sum;
    }

}
