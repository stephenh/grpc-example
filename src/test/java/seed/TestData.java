package seed;

import java.util.function.Consumer;

import org.jdbi.v3.core.Jdbi;

/** Helper methods to give our tests low-level access to the database for setup/assertions. */
public class TestData {

  // Kind of ugly, but add some helpers to make our tests prettier to read
  public static Account save(Jdbi db, Account.Builder account) {
    if (account.getId() == 0) {
      long id = db.withExtension(AccountDao.class, dao -> dao.insert(account.build()));
      return account.setId(id).build();
    } else {
      db.useExtension(AccountDao.class, dao -> dao.update(account.build()));
      return account.build();
    }
  }

  public static Account read(Jdbi db, Account account) {
    return db.withExtension(AccountDao.class, dao -> dao.read(account.getId()));
  }

  public static double balance(Jdbi db, Account account) {
    return ((Long) db.withExtension(TransactionDao.class, dao -> dao.balance(account.getId()))).longValue() / 100.00;
  }

  public static Transaction deposit(Jdbi db, Account account, double dollars) {
    return deposit(db, account, dollars, t -> {
    });
  }

  public static Transaction deposit(Jdbi db, Account account, double dollars, Consumer<Transaction.Builder> f) {
    return db.withExtension(TransactionDao.class, dao -> {
      Transaction.Builder t = Transaction
        .newBuilder()
        .setAccountId(account.getId())
        .setTimestampInMillis(System.currentTimeMillis()) // should use a clock
        .setAmountInCents((long) (dollars * 100.00))
        .setDescription("deposit");
      f.accept(t);
      long id = dao.insert(t.build());
      return dao.read(id);
    });
  }

  public static Account.Builder newAccount() {
    return Account.newBuilder().setName("bob").setAddress("123 Road").setSsn("111-111-1111");
  }

}
