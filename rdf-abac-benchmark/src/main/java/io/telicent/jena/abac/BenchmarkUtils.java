package io.telicent.jena.abac;

import org.apache.commons.io.output.TeeOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class BenchmarkUtils {
    /**
     * Runs a JMH benchmark for a specific benchmark class, primarily intended for use for running classes via an IDE.
     * The log and results of the run are captured to the {@code target/} directory so they can be inspected later if
     * necessary.
     *
     * @param benchmarkClass The benchmark class to run
     */
    public static void run(Class<?> benchmarkClass) {
        PrintStream originalOut = System.out;
        File logFile = new File("target", benchmarkClass.getSimpleName() + "-log.txt");
        System.out.println("Benchmark Log File: " + logFile.getAbsolutePath());
        try (FileOutputStream log = new FileOutputStream(logFile)) {
            // Print the progress output to both our log file and to StdOut so developers can see progress in the IDE
            // console
            try (PrintStream output = new PrintStream(new TeeOutputStream(System.out, log))) {
                System.setOut(output);

                // Prepare a results file where the benchmark results are placed in machine-readable (JSON) format
                File resultsFile = new File("target/", benchmarkClass.getSimpleName() + "-results.json");
                System.out.println("Benchmark Results File: " + resultsFile.getAbsolutePath());
                System.out.print("NB - Won't be populated until benchmark completes!");
                System.out.println();

                // Run a single benchmark class with JMH, requested JSON format results to our results file
                org.openjdk.jmh.Main.main(new String[] {
                        "-rf",
                        "JSON",
                        "-rff",
                        resultsFile.getAbsolutePath(),
                        benchmarkClass.getSimpleName()
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                System.setOut(originalOut);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
