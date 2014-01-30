Promises
========

### Continuous Integration
[![Build Status](https://travis-ci.org/shelajev/promises.png)](https://travis-ci.org/shelajev/promises)

### Quick intro

A small java8 implementation of Promise monad. Promise represents a result of async action.

Consider an example below:

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

By using promises one can easily chain execution of async tasks propagating their results through the whole computation.
Additionally, any exceptions will also be propagated without any fiddling with callbacks.


Monads
======

Monad is a type that represents a context of computation.

    class Monad m where
      return :: a -> m a
      (>>=)  :: m a -> (a -> m b) -> m b -- this is called bind

Essentially, it means that we can wrap a computation into a monad instance and pass it around.

Let's look at those methods closer, and I definitely recommend you to check
out [Typeclassopedia](http://www.haskell.org/haskellwiki/Typeclassopedia) for a better and more rigorous explanation.

### Return

    return :: a -> m a

This is just a constructor, it takes a type *a* and returns a monad of type *m a*.
Type *a* here is a result of computation in the monad.
Intuitively, if we don't have it, monads are less interesting, because we cannot instantiate them.

So the only interesting part of a monad definition is a *bind* function.

### Bind

    (>>=)  :: m a -> (a -> m b) -> m b

If you're not good at Haskell notation, here's a somewhat corresponding Java definition:

    public static <A, B> Monad<B> bind(Monad<A> ma, Function<A, Monad<B>> function);

Or, if we accept that the first parameter is represented by *this*, we get an instance method:

    abstract class Monad<A> {
      public <B> Monad<B> bind(Function<A, Monad<B>> function);
    }

Bind takes an instance of monad and a function from its result type and produces another monad.
How does it help us in any way? Well, if monads featured an *unwrap* function, everything would be easy.
We could chain computations like:

    (a -> m b)
    (b -> m c)

just by getting *m b* and unwrapping it to get the actual result. However, remember, that a monad is a context, and ripping values out of context is not always correct.

With the help of *bind* that every monad type implements according to its own internal understanding, we can chain computations without leaving a context that this monad represents.
Essentially, we supply a computation with result type *a* and a function over that result type to obtain a computation that results in *b*.

See, it isn't that complicated.

There's also an alternative definition of monads that uses *fmap* and *join* functions:

    fmap :: (a -> b) -> (m a -> m b)
    join :: m m a -> m a

which can be used to define bind. Figure out how come? It is useful.



Third party code
================

Original code for Promise implementation is taken from [Play! Framework](https://github.com/playframework/play1), which is distributed under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0.html). 

