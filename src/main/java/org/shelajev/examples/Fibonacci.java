package org.shelajev.examples;

import java.util.concurrent.ExecutionException;

import org.shelajev.async.Async;
import org.shelajev.promise.Promise;

import static org.shelajev.promise.Promise.lift;

/**
 * Example class that shows basic use of promises.
 */
public class Fibonacci {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    Fibonacci fn = new Fibonacci(40);
    Promise<Void> result = fn.compute();
    System.out.println("Started computing!");
    result.get();
    System.out.println("Done computing!");
  }

  private final int n;

  public Fibonacci(int n) {
    this.n = n;
  }

  private Promise<Void> compute() {
    Promise<Long> computing = Async.submit(() -> fib(n));
    return computing.bind(lift(Fibonacci::printout));
  }

  private static void printout(Long number) {
    System.out.println("Computed " + number);
  }

  private long fib(int n) {
    if(n < 0) {
      throw new IllegalArgumentException("I can compute n-th fibonacci number, where n >= 0, n = " + n);
    }
    if(n <= 1) {
      return 1;
    }

    return fib(n - 2) + fib(n - 1);
  }
}
