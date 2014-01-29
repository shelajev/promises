package org.shelajev.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.shelajev.promise.Promise;

/**
 *
 * Util class with a default executor service for async actions.
 *
 * Uses promises to seem smarter.
 *
 * @author Oleg Šelajev
 */
public class Async {

  public static ExecutorService defaultExecutor;

  static {
    int cores = Integer.getInteger("async.pool.default", 16);
    defaultExecutor = Executors.newFixedThreadPool(cores);
  }

  public static void shutdown() {
    defaultExecutor.shutdownNow();
  }

  public static <V> Promise<V> submit(Callable<V> callable) {
    return submit(callable, defaultExecutor);
  }

  public static <V> Promise<V> submit(Callable<V> callable, ExecutorService executorService) {
    final Promise<V> promise = new Promise<>();

    Callable<V> smarterCallable = new Callable<V>() {
      public V call() throws Exception {
        try {
          V result = callable.call();
          promise.invoke(result);
          return result;
        }
        catch (Throwable e) {
          promise.invokeWithException(e);
          return null;
        }
      }
    };

    executorService.submit(smarterCallable);
    return promise;
  }
}
