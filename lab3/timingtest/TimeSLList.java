package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    public static void timeGetLast() {
        // TODO: YOUR CODE HERE
        AList<Integer> Ns = new AList<>();
        AList<Double> times = new AList<>();
        AList<Integer> opCounts = new AList<>();

        int[] sizes = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
        int M = 10000;

        for (int n : sizes) {
            // Step 1 & 2: Create an SLList and add N items
            SLList<Integer> testList = new SLList<>();
            for (int i = 0; i < n; i++) {
                testList.addFirst(i);
            }

            // Step 3: Start the timer
            Stopwatch sw = new Stopwatch();

            // Step 4: Perform M getLast operations
            for (int i = 0; i < M; i++) {
                testList.getLast();
            }

            // Step 5: Check the timer
            double timeInSeconds = sw.elapsedTime();

            Ns.addLast(n);
            times.addLast(timeInSeconds);
            opCounts.addLast(M);
        }

        System.out.println("Timing table for getLast");
        printTimingTable(Ns, times, opCounts);
    }

}
