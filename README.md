> This repository was archived during handover to a new maintainer
# liflig-http4k-utils

Kotlin library that provides a `ServiceRouter` for setting up the recommended http4k filters         
https://confluence.capraconsulting.no/display/CALS/http4k+i+Liflig

Also sets up an optional /health endpoint.


This library is currently only distributed in Liflig
internal repositories.

## Contributing

This project follows
https://confluence.capraconsulting.no/x/fckBC

To check build before pushing:

```bash
mvn verify
```

The CI server will automatically release new version for builds on master.