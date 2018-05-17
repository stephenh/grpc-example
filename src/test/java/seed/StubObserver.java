package seed;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;

/**
 * Stub observer for tests.
 *
 * All of our grpc endpoints are synchronous, this could be made into a future
 * if we really needed to wait until onCompleted was called.
 */
public class StubObserver<T> implements StreamObserver<T> {

  // this is boilerplate-y, not a fan grpc's approach here
  public static <T> T getSync(Consumer<StreamObserver<T>> f) {
    StubObserver<T> res = new StubObserver<>();
    f.accept(res);
    assertThat(res.values.size(), is(1));
    assertThat(res.completed, is(true));
    return res.values.get(0);
  }

  public final List<T> values = new ArrayList<>();
  public boolean completed;

  @Override
  public void onNext(T value) {
    if (completed) {
      throw new IllegalStateException();
    }
    values.add(value);
  }

  @Override
  public void onError(Throwable t) {
    if (completed) {
      throw new IllegalStateException();
    }
    completed = true;
  }

  @Override
  public void onCompleted() {
    completed = true;
  }
}
