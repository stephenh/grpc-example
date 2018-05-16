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
    Account account = request.getAccount();
    // Accounts being open when created wasn't explicitly mentioned, but makes sense.
    //
    // Tangentially, we probably only want to allow a subset of Account fields to be set on creation;
    // grpc does have a vague notion of projections, but they are enforced in the application code, e.g.
    // FieldMask.newBuilder().add(...). Which is fine, but not as nice as putting them directly in the
    // proto file, so it'd be clear to clients. I'm surprised that isn't supported better.
    if (account.getStatus() != AccountStatus.OPEN) {
      response.onNext(CreateAccountResponse.newBuilder().setSuccess(false).addErrors("Accounts must be created in the OPEN status").build());
      response.onCompleted();
      return;
    }
    db.useExtension(AccountDao.class, dao -> {
      long id = dao.insert(request.getAccount());
      response.onNext(CreateAccountResponse.newBuilder().setId(id).setSuccess(true).build());
      response.onCompleted();
    });
  }

  @Override
  public void closeAccount(CloseAccountRequest request, StreamObserver<CloseAccountResponse> responseObserver) {
    db.useTransaction(handle -> {
      Account account = handle.attach(AccountDao.class).read(request.getId());
      handle.attach(AccountDao.class).update(account.toBuilder().setStatus(AccountStatus.CLOSED).build());
      responseObserver.onNext(CloseAccountResponse.newBuilder().setSuccess(true).build());
      responseObserver.onCompleted();
    });
  }

}
