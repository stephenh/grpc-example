package seed;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

public class SeedServerTest {

  private Jdbi db;
  private SeedServer server;

  @Before
  public void setup() {
    db = TestData.newDb();
    server = new SeedServer(db);
  }

  @Test
  public void shouldAllowCreatingAccounts() {
    // given a request to make an account
    CreateAccountRequest req = CreateAccountRequest.newBuilder().setAccount(TestData.newAccount().build()).build();
    // when executed
    CreateAccountResponse res = create(req);
    // then it was successful
    assertThat(res.getSuccess(), is(true));
    // and we see the account in the database
    db.useExtension(AccountDao.class, dao -> {
      assertThat(dao.count(), is(1L));
    });
  }

  private CreateAccountResponse create(CreateAccountRequest req) {
    // this is boilerplate-y, not a fan grpc's approach here
    StubObserver<CreateAccountResponse> res = new StubObserver<>();
    server.createAccount(req, res);
    assertThat(res.values.size(), is(1));
    assertThat(res.completed, is(true));
    return res.values.get(0);
  }

}
