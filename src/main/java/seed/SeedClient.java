package seed;

import java.util.concurrent.CountDownLatch;

import io.grpc.Channel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import seed.AccountServiceGrpc.AccountServiceStub;

public class SeedClient {

  public static void main(String[] args) throws Exception {
    Channel channel = NettyChannelBuilder.forAddress("localhost", 3000).negotiationType(NegotiationType.PLAINTEXT).build();
    AccountServiceStub stub = AccountServiceGrpc.newStub(channel);

    CountDownLatch c = new CountDownLatch(1);
    Account account = Account.newBuilder().setName("a").setAddress("asdf").setSsn("ssn").build();

    stub.createAccount(//
      CreateAccountRequest.newBuilder().setAccount(account).build(),
      new StreamObserver<CreateAccountResponse>() {
        public void onNext(CreateAccountResponse value) {
          System.out.println("Response " + value.toString());
        }

        @Override
        public void onError(Throwable t) {
          t.printStackTrace();
        }

        @Override
        public void onCompleted() {
          c.countDown();
        }
      });
    c.await();
  }

}
