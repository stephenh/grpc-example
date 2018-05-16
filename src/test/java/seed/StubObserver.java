package seed;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;

/**
 * Stub observer for tests.
 *
 * All of our grpc endpoints are synchronous, this could be made into a future
 * if we really needed to wait until onCompleted was called.
 */
public class StubObserver<T> implements StreamObserver<T> {

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