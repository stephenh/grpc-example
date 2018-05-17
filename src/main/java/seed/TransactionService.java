package seed;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import io.grpc.stub.StreamObserver;
import seed.TransactionServiceGrpc.TransactionServiceImplBase;

public class TransactionService extends TransactionServiceImplBase {

  private final Jdbi db;

  public TransactionService(Jdbi db) {
    this.db = db;
  }

  @Override
  public void transfer(TransferRequest req, StreamObserver<TransferResponse> res) {
    TransferResponse response = db.inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
      TransactionDao dao = handle.attach(TransactionDao.class);
      // TODO Add some sanity checks to sourceBalance, e.g. should be positive, should probably not be $1 billion
      long sourceBalance = dao.balance(req.getSourceAccountId());
      if (sourceBalance >= req.getAmountInCents()) {
        // TODO use a clock
        long now = System.currentTimeMillis();
        // make two new transactions; currently we don't tie them back to the same source event
        dao.insert(
          Transaction
            .newBuilder()
            .setAccountId(req.getSourceAccountId())
            .setAmountInCents(req.getAmountInCents() * -1)
            .setTimestampInMillis(now)
            // TODO: I think "" is allowed, which we should probably reject
            .setDescription(req.getDescription())
            .build());
        dao.insert(
          Transaction
            .newBuilder()
            .setAccountId(req.getDestinationAccountId())
            .setAmountInCents(req.getAmountInCents())
            .setTimestampInMillis(now)
            .setDescription(req.getDescription())
            .build());

        // We're in a transaction but technically should throb something on the
        // account to keep concurrent transfers from over-drawing the account.
        //
        // AFAICT not even SERIALIZABLE transaction isolation would prevent over-
        // draws with the current "read balance + insert two rows" approach. This
        // is because this timeline is possible:
        //
        // txn1: queries transaction where accountId = 1, gets $10
        // txn2: queries transaction where accountId = 1, gets $10
        // txn1: adds two new transaction rows, one for accountId = -10
        // txn2: adds two new transaction rows, one for accountId = -10
        //
        // None of the usual suspects (dirty read, non-repeatable read, or phantom read)
        // have been violated here because we have not re-issued a read.
        //
        // I suppose that would be a way to do it, instead of bumping an op-lock
        // on the account row (which would cause a conflict), we could re-read the
        // balance and ensure it matches what we expect. This
        // would trigger phantom read detection.
        //
        // Sorry, I don't have a unit test for this scenario. :-)

        long newSourceBalance = dao.balance(req.getSourceAccountId());
        if (newSourceBalance != sourceBalance - req.getAmountInCents()) {
          // In theory we shouldn't get here, because it means we had a phantom read,
          // which the database should have already detected/failed on
          throw new RuntimeException("Potential overdraw detected");
        }

        return TransferResponse.newBuilder().setSuccess(true).build();
      } else {
        return TransferResponse.newBuilder().setSuccess(false).addErrors("Insufficient funds").build();
      }
    });
    // TODO Add try/catch in case withTransaction throws
    res.onNext(response);
    res.onCompleted();
  }

  @Override
  public void getByAccount(GetByAccountRequest request, StreamObserver<GetByAccountResponse> response) {
    List<Transaction> transactions = db.withExtension(TransactionDao.class, dao -> {
      return dao.readForAccount(request.getAccountId());
    });
    // TODO Add try/catch in case withExtension throws
    response.onNext(GetByAccountResponse.newBuilder().addAllTransactions(transactions).build());
    response.onCompleted();
  }

}
