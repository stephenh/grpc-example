package seed;

import org.jdbi.v3.core.Jdbi;

import io.grpc.stub.StreamObserver;
import seed.AccountServiceGrpc.AccountServiceImplBase;

public class AccountService extends AccountServiceImplBase {

  private final Jdbi db;

  public AccountService(Jdbi db) {
    this.db = db;
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> response) {
    Account account = request.getAccount();

    // Potential requirements:
    //
    // We probably only want to allow a subset of Account fields to be set on creation;
    // grpc does have a vague notion of projections, but they are enforced in the application code, e.g.
    // FieldMask.newBuilder().add(...). Which is fine, but not as nice as putting them directly in the
    // proto file, so it'd be clear to clients. I'm surprised that isn't supported better.
    //
    // Also we should probably not let the same person open tons of accounts; more than one
    // is probably okay, but spamming us with 1000s would not. Defer to PM.
    //
    // Also, there is a known "bug" where grpc gives strings a default value of "", so we're sending
    // those default values to the database; they should be stripped to be null, so that they trigger
    // the not-null constraints. Would be nice to sanitize all empty strings to null somewhere inside
    // of jdbi, instead of spreading it all over.

    // Accounts being always-open when created wasn't explicitly mentioned, but makes sense. In
    // real-life would probably start as pending review or something like that.
    if (account.getStatus() != AccountStatus.OPEN) {
      response.onNext(CreateAccountResponse.newBuilder().setSuccess(false).addErrors("Accounts must be created in the OPEN status").build());
      response.onCompleted();
      return;
    }
    // TODO Should have try/catch so that response.onCompleted is always called
    db.useExtension(AccountDao.class, dao -> {
      long id = dao.insert(request.getAccount());
      response.onNext(CreateAccountResponse.newBuilder().setId(id).setSuccess(true).build());
      response.onCompleted();
    });
  }

  @Override
  public void closeAccount(CloseAccountRequest request, StreamObserver<CloseAccountResponse> response) {
    // TODO Should have try/catch so that response.onCompleted is always called
    db.useTransaction(handle -> {
      Account account = handle.attach(AccountDao.class).read(request.getId());
      handle.attach(AccountDao.class).update(account.toBuilder().setStatus(AccountStatus.CLOSED).build());
      response.onNext(CloseAccountResponse.newBuilder().setSuccess(true).build());
      response.onCompleted();
    });
  }

  @Override
  public void getInfo(GetAccountInfoRequest request, StreamObserver<GetAccountInfoResponse> response) {
    GetAccountInfoResponse res = db.withHandle(handle -> {
      // Would need access checks/etc.
      Account account = handle.attach(AccountDao.class).read(request.getAccountId());
      long balance = handle.attach(TransactionDao.class).balance(request.getAccountId());
      return GetAccountInfoResponse.newBuilder().setAccount(account).setBalanceInCents(balance).build();
    });
    // TODO Should have try/catch so that response.onCompleted is always called
    response.onNext(res);
    response.onCompleted();
  }

}
