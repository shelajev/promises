package org.shelajev.examples;

import java.util.concurrent.ExecutionException;

import org.shelajev.async.Async;
import org.shelajev.promise.Promise;

/**
 *  @author Oleg Šelajev
 */
public class Examples {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    example1();
    example2();
  }

  public static void example1() throws ExecutionException, InterruptedException {
    Promise<String> promise = Async.submit(() -> {
      String helloWorld = "hello world";
      long n = 500;
      System.out.println("Sleeping " + n + " ms example1");
      Thread.sleep(n);
      return helloWorld;
    });

    Promise<Integer> promise2 = promise.bind(string -> Promise.pure(Integer.valueOf(string.hashCode())));

    System.out.println("Main thread example2");
    int hashCode = promise2.get();

    System.out.println("HashCode = " + hashCode);
  }

  public static void example2() throws ExecutionException, InterruptedException {
    Promise<String> promise = Async.submit(() -> {
      String helloWorld = "hello world";
      long n = 500;
      System.out.println("Sleeping " + n + " ms example2");
      Thread.sleep(n);
      return helloWorld;
    });

    Promise<Integer> promise2 = promise.bind(string -> {
      throw new RuntimeException("hurray, an exception: " + string);
    });

    System.out.println("Main thread example2");
    try {
      int hashCode = promise2.get();
      System.out.println("HashCode = " + hashCode);
    }
    catch(Exception e) {
      System.out.println(e);
    }
  }
}
