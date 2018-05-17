package seed;

import java.io.IOException;

import org.jdbi.v3.core.Jdbi;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

public class SeedServer {
  public static void main(String[] args) {
    Jdbi db = Database.newDb();
    Server rpc = NettyServerBuilder //
      .forPort(3000)
      .addService(new AccountService(db))
      .addService(new TransactionService(db))
      .build();
    try {
      rpc.start();
      System.out.println("Listening");
      rpc.awaitTermination();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
