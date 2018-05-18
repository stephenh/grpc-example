
Run Instructions
================

In one terminal, run `./gradlew server`.

In another terminal, run `./gradlew client`.

Note that currently there is a ton of grpc log output.

Design Thought Process
======================

I really like schema-driven APIs, so considered:

* Swagger, which would provide more REST-native across lots of languages.

  Would be a good choice but I've not used it before, so I don't want to get distracted learning it. Would be fun though.

* GraphQL, like the idea, but seems like more internal API, vs. public API, and don't need the cross-entity graph/etc. for this.

* grpc-java, which is not natively REST friendly, so also more of an internal-only API choice, but going with this as I've used it before.

  (Granted, Seed is Go and I remember [Twirp](https://blog.twitch.tv/twirp-a-sweet-new-rpc-framework-for-go-5f2febbf35f) going by, so apologies if grpc is a red flag.)

For storage, considered:

* Just keeping everything in-memory as objects, given this is a homework assignment, but that seemed overly cute.

* Postgres, I like it, but local config for just running my code seems annoying.

* So sqlite or h2 seemed like a good "zero-setup" choice, while still being "real" storage.

  Using sqlite/h2 also seems I can probably cheat and have unit tests use the real database instead of mocking/stubbing things out.

* For JDBC/ORM/etc., I haven't used [JDBI](http://jdbi.org/) before, but seems simple.

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


