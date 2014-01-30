package org.shelajev.promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Original promise class came from Play! Framework.
 *
 * See https://github.com/playframework/play1
 * Play! framework is distributed under Apache 2 licence.
 *
 *
 *  @author Oleg Šelajev
 */
public class Promise<V> implements Future<V> {

  public static interface Action<T> {
    void invoke(T result);
  }

  protected final CountDownLatch taskLock = new CountDownLatch(1);
  protected List<Action<Promise<V>>> callbacks = new ArrayList<>();

  protected V result = null;
  protected Throwable exception = null;

  protected boolean invoked = false;

  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  public boolean isCancelled() {
    return false;
  }

  public boolean isDone() {
    return invoked;
  }

  public V getOrNull() {
    return result;
  }

  public V get() throws InterruptedException, ExecutionException {
    taskLock.await();
    if (exception != null) {
      // The result of the promise is an exception - throw it
      throw new ExecutionException(exception);
    }
    return result;
  }

  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if(!taskLock.await(timeout, unit)) {
      throw new TimeoutException(String.format("Promise didn't redeem in %s %s", timeout, unit));
    }

    if (exception != null) {
      // The result of the promise is an exception - throw it
      throw new ExecutionException(exception);
    }
    return result;
  }

  public void invoke(V result) {
    invokeWithResultOrException(result, null);
  }

  public void invokeWithException(Throwable t) {
    invokeWithResultOrException(null, t);
  }

  protected void invokeWithResultOrException(V result, Throwable t) {
    synchronized (this) {
      if (!invoked) {
        invoked = true;
        this.result = result;
        this.exception = t;
        taskLock.countDown();
      } else {
        return;
      }
    }
    for (Action<Promise<V>> callback : callbacks) {
      callback.invoke(this);
    }
  }

  public void onRedeem(Action<Promise<V>> callback) {
    synchronized (this) {
      if (!invoked) {
        callbacks.add(callback);
      }
    }
    if (invoked) {
      callback.invoke(this);
    }
  }

  public static <T> Promise<List<T>> waitAll(final Promise<T>... promises) {
    return waitAll(Arrays.<Promise<T>>asList(promises));
  }

  public static <T> Promise<List<T>> waitAll(final Collection<Promise<T>> promises) {
    final CountDownLatch waitAllLock = new CountDownLatch(promises.size());
    final Promise<List<T>> result = new Promise<List<T>>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        boolean r = true;
        for (Promise<T> f : promises) {
          r = r & f.cancel(mayInterruptIfRunning);
        }
        return r;
      }

      @Override
      public boolean isCancelled() {
        boolean r = true;
        for (Promise<T> f : promises) {
          r = r & f.isCancelled();
        }
        return r;
      }

      @Override
      public boolean isDone() {
        boolean r = true;
        for (Promise<T> f : promises) {
          r = r & f.isDone();
        }
        return r;
      }

      @Override
      public List<T> get() throws InterruptedException, ExecutionException {
        waitAllLock.await();
        List<T> r = new ArrayList<T>();
        for (Promise<T> f : promises) {
          r.add(f.get());
        }
        return r;
      }

      @Override
      public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if(!waitAllLock.await(timeout, unit)) {
          throw new TimeoutException(String.format("Promises didn't redeem in %s %s", timeout, unit));
        }

        return get();
      }
    };

    final Action<Promise<T>> action = new Action<Promise<T>>() {

      public void invoke(Promise<T> completed) {
        waitAllLock.countDown();
        if (waitAllLock.getCount() == 0) {
          try {
            result.invoke(result.get());
          } catch (Exception e) {
            result.invokeWithException(e);
          }
        }
      }
    };

    for (Promise<T> f : promises) {
      f.onRedeem(action);
    }
    if(promises.isEmpty()) {
      result.invoke(Collections.<T>emptyList());
    }
    return result;
  }

  public static <T> Promise<T> waitAny(final Promise<T>... promises) {
    return waitAny(Arrays.<Promise<T>>asList(promises));
  }

  public static <T> Promise<T> waitAny(final Collection<Promise<T>> promises) {
    final Promise<T> result = new Promise<>();

    final Action<Promise<T>> action = new Action<Promise<T>>() {
      public void invoke(Promise<T> completed) {
        synchronized (this) {
          if (result.isDone()) {
            return;
          }
        }
        T resultOrNull = completed.getOrNull();
        if(resultOrNull != null) {
          result.invoke(resultOrNull);
        }
        else {
          result.invokeWithException(completed.exception);
        }
      }
    };

    for (Promise<T> f : promises) {
      f.onRedeem(action);
    }

    return result;
  }

  /**
   * aka. return :: a -> m a
   */
  public static <V> Promise<V> pure(final V v) {
    Promise<V> p = new Promise<>();
    p.invoke(v);
    return p;
  }

  public static <V> Promise<V> wrap(final Throwable t) {
    Promise<V> p = new Promise<>();
    p.invokeWithException(t);
    return p;
  }


  /**
   *
   * (>>=) :: m a -> ( a -> m b) -> m b
   *
   * Here is how you can implement it with fmap & join combination
   *
   * public <R> Promise<R> bind(final Function<V, Promise<R>> function) {
   *   Function<Promise<V>, Promise<Promise<R>>> lifted = fmap(function);
   *   Promise<Promise<R>> result = lifted.apply(this);
   *   return join(result);
   * }
   *
   *
   */
  public <R> Promise<R> bind(final Function<V, Promise<R>> function) {
    Promise<R> result = new Promise<>();

    this.onRedeem(callback -> {
      try {
        V v = callback.get();
        Promise<R> applicationResult = function.apply(v);
        applicationResult.onRedeem(applicationCallback -> {
          try {
            R r = applicationCallback.get();
            result.invoke(r);
          }
          catch (Throwable e) {
            result.invokeWithException(e);
          }
        });
      }
      catch (Throwable e) {
        result.invokeWithException(e);
      }
    });

    return result;
  }

  /**
   * aka liftM :: (a -> b) -> (m a -> m b)
   */
  public static <T, R> Function<Promise<T>, Promise<R>> fmap(Function<T, R> function) {
    return p -> {
      Promise<R> result = new Promise<>();

      p.onRedeem(callback -> {
        try {
          T t = callback.get();
          R r = function.apply(t);
          result.invoke(r);
        }
        catch (Throwable e) {
          result.invokeWithException(e);
        }
      });

      return result;
    };
  }

  /**
   * join :: m m a -> m a
   */
  public static <T> Promise<T> join(Promise<Promise<T>> promise) {
    Promise<T> result = new Promise<>();
    promise.onRedeem(callback -> {
      try {
        Promise<T> inner = callback.get();
        inner.onRedeem(innerCallback -> {
          try {
            T t = innerCallback.get();
            result.invoke(t);
          }
          catch (InterruptedException | ExecutionException e) {
            result.invokeWithException(e);
          }
        });
      }
      catch (InterruptedException | ExecutionException e) {
        result.invokeWithException(e);
      }
    });
    return result;
  }

  /**
   * Imagine you have a function:
   * f :: a -> b
   *
   * And you want to bind it into some promise, however you need a:
   * g :: a -> m b
   *
   * function to bind it.
   * lift takes a function and wraps its result into a promise
   *
   * lift :: (a -> b) -> (a -> m b)
   */
  public static <T, R> Function<T, Promise<R>> lift(Function<T, R> function) {
    return function.andThen(r -> Promise.pure(r));
  }

  public static <T> Function<T, Promise<Void>> lift(Consumer<T> consumer) {
    return (t -> { consumer.accept(t); return Promise.pure((Void) null); });
  }
}
