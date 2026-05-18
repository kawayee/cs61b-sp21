package tester;

import static org.junit.Assert.*;

import edu.princeton.cs.introcs.StdRandom;
import org.junit.Test;
import student.StudentArrayDeque;

public class TestArrayDequeEC {

    @Test
    public void randomizedTest() {
        StudentArrayDeque<Integer> student = new StudentArrayDeque<>();
        ArrayDequeSolution<Integer> solution = new ArrayDequeSolution<>();

        int numOps = 1000;
        StringBuilder message = new StringBuilder();

        for (int i = 0; i < numOps; i++) {
            int op;
            if (student.size() == 0) {
                op = StdRandom.uniform(2); // only add when empty
            } else {
                op = StdRandom.uniform(4);
            }

            if (op == 0) {
                // addFirst
                int val = StdRandom.uniform(100);
                student.addFirst(val);
                solution.addFirst(val);
                message.append("addFirst(").append(val).append(")\n");
            } else if (op == 1) {
                // addLast
                int val = StdRandom.uniform(100);
                student.addLast(val);
                solution.addLast(val);
                message.append("addLast(").append(val).append(")\n");
            } else if (op == 2) {
                // removeFirst
                Integer studentVal = student.removeFirst();
                Integer solutionVal = solution.removeFirst();
                message.append("removeFirst()\n");
                assertEquals(message.toString(), solutionVal, studentVal);
            } else {
                // removeLast
                Integer studentVal = student.removeLast();
                Integer solutionVal = solution.removeLast();
                message.append("removeLast()\n");
                assertEquals(message.toString(), solutionVal, studentVal);
            }

            // Also check size
            message.append("size()\n");
            assertEquals(message.toString(), solution.size(), student.size());
            // clean message
            message.delete(0, message.length());
        }
    }
}
