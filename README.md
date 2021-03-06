Helse-E2E
===================

End to End testing


## Oversikt

Oppsett av komponentene som kjøres for E2E tests kan sees [her](../blob/master/docker-compose.png)

For å oppdatere figuren bruk denne kommandoen:

```
docker run --rm -it --name dcv -v $(pwd):/input pmsipilot/docker-compose-viz render --no-ports --no-networks -m image docker-compose.yml
```

#### Nøkler:

For at sparkel skal kunne kommunisere med vtpmock over SSL samt for å signere JWT´er med mock-STS må det genereres noen nøkler.
Dette kan gjøres ved å kjøre `./makekeystore.sh`

#### Kom i gang

```
mvn clean install
export SPA_IMAGE=navikt/spa:$(curl -s https://registry.hub.docker.com/v2/repositories/navikt/spa/tags/ | jq -r '."results"[]["name"]' | sed -n 1p)
export SPARKEL_IMAGE=navikt/sparkel:$(curl -s https://registry.hub.docker.com/v2/repositories/navikt/sparkel/tags/ | jq -r '."results"[]["name"]' | sed -n 1p)
export VTPMOCK_IMAGE=navikt/spvtpmock:$(curl -s https://registry.hub.docker.com/v2/repositories/navikt/spvtpmock/tags/ | jq -r '."results"[]["name"]' | sed -n 1p)
docker-compose up --build
```

#### Manuell test av sparkel vs. vtpmock:

Lag et accessToken for "srvspa" systembruker (NB: vtpmock.local speiler per nå Issuer fra url.
Issuer er konfigurert til å måtte være https://vtpmock.local..., så url må være https://vtpmock.local, altså må 127.0.0.1 vtpmock.local inn i etc/hosts)

`curl -k --data "code=srvspa" https://vtpmock.local:8063/isso/oauth2/access_token`

Last f.eks. scenario #50 inne i mocken + returner disse scenario-dataene (http på 8060, https på 8063):

`curl --request POST http://localhost:8060/api/testscenario/50`

Bruk id-token (som access-token) og aktørId fra scenarioet til å kjøre et kall mot sparkel:

`curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:8080/api/person/<søkerAktørIdent>` 


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
