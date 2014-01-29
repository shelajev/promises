package org.shelajev.promise;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.shelajev.async.Async;

import static org.junit.Assert.assertEquals;

public class PromiseTests {

  @Test
  public void waitAny() throws Exception {
    String s = Promise.waitAny(sleep(300), sleep(200)).get();
    assertEquals("-> 200", s);
  }

  @Test
  public void waitAll() throws Exception {
    List<String> s = Promise.waitAll(sleep(200), sleep(10)).get();
    assertEquals(2, s.size());
    assertEquals("-> 200", s.get(0));
    assertEquals("-> 10", s.get(1));
  }

  @Test(expected = TimeoutException.class)
  public void waitAllTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    Promise.waitAll(sleep(200), sleep(200)).get(100, TimeUnit.MILLISECONDS);
  }

  @Test()
  public void waitAllNoTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    Promise.waitAll(sleep(200), sleep(200)).get(400, TimeUnit.MILLISECONDS);
  }

  @Test(expected = TimeoutException.class)
  public void waitForTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    sleep(200).get(100, TimeUnit.MILLISECONDS);
  }

  private Promise<String> sleep(long ms) throws InterruptedException {
    return Async.submit(() -> {
      Thread.sleep(ms);
      return "-> " + ms;
    });
  }

}
