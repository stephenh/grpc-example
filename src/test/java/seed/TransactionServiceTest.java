package seed;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static seed.TestData.balance;
import static seed.TestData.deposit;
import static seed.TestData.save;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

public class TransactionServiceTest {

  private Jdbi db;
  private TransactionService service;

  @Before
  public void setup() {
    db = TestData.newDb();
    service = new TransactionService(db);
  }

  // I almost went down the rabbit hole of previewing JDK10 which finally finally
  // (finally) brings `var` to Java. I have a mostly-love/slightly-hate relationship
  // with Scala. Could have tried Kotlin, what's another experimental technology in
  // the mix for a graded assignment?

  @Test
  public void shouldCreateATransfer() {
    // given two existing accounts
    Account a1 = save(db, TestData.newAccount());
    Account a2 = save(db, TestData.newAccount());
    // and a1 has $100 in it
    deposit(db, a1, 100.00);
    // when we transfer $100 from a1 to a2
    TransferResponse rep = transfer(a1, a2, 100.00, "transfer 1");
    // then it's a success
    assertThat(rep.getSuccess(), is(true));
    // and the balances have changed
    assertThat(balance(db, a1), is(0.00));
    assertThat(balance(db, a2), is(100.00));
    // and the source transaction looks good
    Transaction t1 = db.withExtension(TransactionDao.class, dao -> dao.read(2L));
    assertThat(t1.getAccountId(), is(a1.getId()));
    assertThat(t1.getAmountInCents(), is(-10000L));
    assertThat(t1.getDescription(), is("transfer 1"));
    // and the destination transaction looks good
    Transaction t2 = db.withExtension(TransactionDao.class, dao -> dao.read(3L));
    assertThat(t2.getAccountId(), is(a2.getId()));
    assertThat(t2.getAmountInCents(), is(10000L));
    assertThat(t2.getDescription(), is("transfer 1"));
  }

  @Test
  public void shouldNotAllowAnOverride() {
    // given two existing accounts
    Account a1 = save(db, TestData.newAccount());
    Account a2 = save(db, TestData.newAccount());
    // and a1 has $10 in it
    deposit(db, a1, 10.00);
    // when we transfer $100 from a1 to a2
    TransferResponse rep = transfer(a1, a2, 100.00, "transfer 1");
    // then it fails
    assertThat(rep.getSuccess(), is(false));
    assertThat(rep.getErrorsList(), hasItems("Insufficient funds"));
    // and the balances have not changed
    assertThat(balance(db, a1), is(10.00));
    assertThat(balance(db, a2), is(0.00));
  }

  private TransferResponse transfer(Account from, Account dest, double dollars, String description) {
    TransferRequest req = TransferRequest
      .newBuilder()
      .setSourceAccountId(from.getId())
      .setDestinationAccountId(dest.getId())
      .setAmountInCents((int) (dollars * 100))
      .setDescription(description)
      .build();
    return StubObserver.<TransferResponse> getSync(o -> service.transfer(req, o));
  }

}