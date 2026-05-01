# euvat-filing-frontend

Allows businesses and their agents to claim refunds on VAT paid in other EU member states. Traders or their agents have
the ability to complete amendments to claims submitted.

## Developer setup

[Developer setup](https://confluence.tools.tax.service.gov.uk/display/RBD/Local+Machine+Setup+to+run+and+connect+to+Oracle+database)

## Running the service locally

Service Manager for EUVAT: `sm2 --start EUVAT_ALL`

To check libraries update, run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run` or `sbt run 18501`

To execute the scala formatter: `./run_fmt_checks.sh`

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").