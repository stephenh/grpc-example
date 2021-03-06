
Run Instructions
================

In one terminal, run `./gradlew server`.

In another terminal, run `./gradlew client`.

Note that currently there is a ton of grpc log output.

Code Pointers
=============

* The API is defined in [account.proto](src/main/proto/account.proto) and [transaction.proto](src/main/proto/transaction.proto).

* Services are in [AccountService](src/main/java/seed/AccountService.java) and [TransactionService](src/main/java/seed/TransactionService.java).

* Tests are in [AccountServiceTest](src/test/java/seed/AccountServiceTest.java) and [TransactionServiceTest](src/test/java/seed/TransactionServiceTest.java)

Design Thought Process
======================

I really like schema-driven APIs, so considered:

* Swagger, which would provide more REST-native across lots of languages.

  Would be a good choice but I've not used it before, so I don't want to get distracted learning it. Would be fun though.

* GraphQL, like the idea, but seems like more internal API, vs. public API, and don't need the cross-entity graph/etc. for this.

* grpc-java, which is not natively REST friendly, so also more of an internal-only API choice, but going with this as I've used it before (just for a small hobby project, [mirror](https://github.com/stephenh/mirror)).

  (Granted, Seed is Go and I remember [Twirp](https://blog.twitch.tv/twirp-a-sweet-new-rpc-framework-for-go-5f2febbf35f) going by, so apologies if grpc is a red flag.)

  (Also, in retrospect there are definitely boilerplate-y aspects to grpc, e.g. builders everywhere. Also, I do generally prefer more noun-/entity-based modeling, as then the CRUD/verb/plumbing is implicit/100% standardized vs. being slightly bespoke for each "RPC that is trying to be an entity".)

For storage, considered:

* Just keeping everything in-memory as objects, given this is a homework assignment, but that seemed overly cute.

* Postgres, I like it, but local config for just running my code seems annoying.

* So sqlite or h2 seemed like a good "zero-setup" choice, while still being "real" storage.

  Using sqlite/h2 also seems I can probably cheat and have unit tests use the real database instead of mocking/stubbing things out.

  I ended up going with in-memory [H2](http://www.h2database.com/html/main.html) since it's native JVM.

* For JDBC/ORM/etc., I haven't used [JDBI](http://jdbi.org/) before, but it seems simple, so I'm using that.

  (After having implemented this example with JDBI, it does seem fine for what it is, just a nicer way to do SQL, but I'm not completely anti-ORM yet either. Re-typing out CRUD DAOs in JDBI for every table would get old. Granted, could code generate those.)

Other Misc Notes
================

* I'm deferring all latency/throughput/etc. reporting to infrastructure magic, e.g. combination of grpc + whatever glue.

* I'm purposefully not doing retries.

* I've not thought about timeout scenarios. Or connection pooling.

* I purposefully used millis for dates as I assume this is not truly "the user" facing, and dealing with string-formatted dates and time zones is a PITA. Granted, it makes debugging harder as I cannot yet do millis -> date in my head. That said, this vim macro will do it:

```
command Timestamps %s/\d\@<!\(\d\{10}\)\d\{3}\d\@!/\=strftime('%c', submatch(1))/g
```

* I also purposefully used cents for storage b/c floating point is evil. I do use `100.00`-style doubles in the tests though, which I allow for better test readability. I also purposefully included "InCents" suffixes in the RPC types to be obvious what the primitive value is. Granted, some sort of user-type like `Cents` or `MoneyAmount` would be cool too, but not pursuing that tangent.

* I'm assuming single currency/USD.

* I would not have this many TODOs in production code.


