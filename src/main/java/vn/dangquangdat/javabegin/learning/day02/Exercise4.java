package vn.dangquangdat.javabegin.learning.day02;

public class Exercise4 {

    public static void main(String[] arg) {
        int[] array1 = {-4, 0, 1, 4, 5, 7, 10, 12, 15};
        int target = 0;

        System.out.println("Index of " + target + " is: " + binarySearch(target, array1));

    }

    static int linearSearch(int target, int... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Array not null!");
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == target) {
                return i;
            }
        }
        return -1;
    }

    static int binarySearch(int target, int... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Array not null!");
        }

        int left = 0;
        int right = args.length - 1;
        int repeatTimes = 0;

        while (left <= right) {
            repeatTimes++;
            int mid = left + (right - left) / 2;
            if (args[mid] == target) {
                System.err.println("Repeat: " + repeatTimes);
                return mid;
            }

            if (args[mid] < target) {
                left = mid + 1;

            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

}
