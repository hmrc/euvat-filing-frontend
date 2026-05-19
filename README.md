
# euvat-filing-frontend

Allows businesses and their agents to claim refunds on VAT paid in other EU member states. Traders or their agents have
the ability to complete amendments to claims submitted.

## Developer setup
[Developer setup](https://confluence.tools.tax.service.gov.uk/display/RBD/Local+Machine+Setup+to+run+and+connect+to+Oracle+database)

## Running the services locally

- Service Manager for EUVAT: `sm2 --start EUVAT_ALL`

- To check libraries update, run all tests and coverage: `./run_tests.sh`

- To start the server locally: `sbt run` or `sbt run 18501`

- To execute the scala formatter: `./run_fmt.sh`
- 

### Running locally through Auth wizard: (http://localhost:9949/auth-login-stub/gg-sign-in)

- Redirect URL: http://localhost:18500/manage-eu-vat
  - **Affinity Group    CredId              Key                   Identifier Name     Identifier value    Status**
    - Ind/ Org                              HMRC-EU-REF-ORG       VATRegNo            <non-empty>         Activated
    - Agent             <non-empty>         HMCE-VAT-AGNT         AgentRefNo          <non-empty>         Activated
    - Agent             <non-empty>         HMRC-NOVRN-AGNT       VATAgentRefNo       <non-empty>         Activated

or refer to TO-BE login details: https://confluence.tools.tax.service.gov.uk/spaces/RBD/pages/1274447541/EUVAT+-+Testing#EUVATTesting-TO-BElogindetails%3A


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").