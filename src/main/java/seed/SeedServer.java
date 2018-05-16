package seed;

import org.jdbi.v3.core.Jdbi;

import io.grpc.stub.StreamObserver;
import seed.SeedGrpc.SeedImplBase;

public class SeedServer extends SeedImplBase {

  private final Jdbi db;

  public SeedServer(Jdbi db) {
    this.db = db;
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> response) {
    db.useExtension(AccountDao.class, dao -> {
      dao.insert(request.getAccount());
      response.onNext(CreateAccountResponse.newBuilder().setSuccess(true).build());
      response.onCompleted();
    });
  }

}
