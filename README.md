Leihs Admin
===========


Prerequisite for Building and Development
-----------------------------------------

This service builds with Java OpenJDK 8. We recommend https://www.jenv.be/ to
switch between JAVA Versions. And once set up:

    jenv shell 1.8

There are defaults but you might want to overwrite `LEIHS_MY_HTTP_BASE_URL` or
`LEIHS_DATABASE_URL` before starting any of the following scripts. E.g.

    export LEIHS_MY_HTTP_BASE_URL=http://localhost:3240

    export DATABASE_URL="postgresql://localhost:5432/leihs?max-pool-size=5"
    export LEIHS_DATABASE_URL="jdbc:${DATABASE_URL}"


Building and testing require that `ruby` in the `PATH` resolves to at least
Ruby 2.6. If the `RUBY` environment Variable is set, then `PATH` is
amended with `~/.rubies/$RUBY/bin`.

    export RUBY=ruby-2.6.3


Building
--------

Building uses local and S3 cached artefacts. This can be disabled by setting
`BUILD_CACHE_DISABLED` to `YES`.

    export BUILD_CACHE_DISABLED=YES


### Building

    ./bin/build



Development
-----------

## Run The Application


### Backend


    ./bin/dev-run-backend

alternatively:


    lein do clean, repl

  and once the REPL is running:

    (-main "run")


### Frontend

    ./bin/dev-run-frontend



Testing
-------

1. Building

  see above

1. start the application

  `java -jar target/leihs-admin.jar run -s secret`

3. run specs in a second terminal

  bundle exec rspec spec/




## API Keys

-----BEGIN PGP MESSAGE-----
Comment: GPGTools - http://gpgtools.org

hQIMA8ScbQq5Og78AQ//bJhv/dngXTMP2Z/MpQRNz9jSHjSGTOSrucwi4ufR1upw
M4ndepfhI9/U8H8b9YvajHcRuT2T2RfvLNggpjqtc/8RJrIP5GglRDvu317q/gco
31s9BbqwKa4MbCg9t3uqPsQrFW9D3XZKAkLBhu+vtJE+KzPuaeIUg/SNbJcdN/B+
aUBsnVyleVmCjYwLPH30q5DFBp/ZNb79/XzgPid4b4Z99wmW6561FgTEZOjEZDXb
3ZcrtpVJuhbtOTJ9qL0DQOlwNV7vWuePGUbJPm2aZZdp6sMeArUvzyfAfOoVaIiO
PfUdAgl/tfdhzETs33WOnkDUHexiniNReLW26LmZS1qfOL9DwqhcZpuCm2pCykGW
21uu4RD1gvakQFmb9nmAEbcYviEtUDPdaFv2xInATPVm6G8MsaPrLxoqN0a7JuIW
6OC8/gq7u+NN0x54/gYsIGwaIAc2O8LtVxVv59Q35pGzItvZHFLRrnnFnyMPlIBq
vI0P7KlWWV8ysn0C7M442TX4X8WR00x/DT674R80SYWatmBArHADYchlqrF8msj5
BUzRWScULR5cZYlZtvalfpQPAu4ivCk+p0KqUapGf6y87Jzw0VSRMu9pIlYRFTFI
3TUKMaOF/DRd02w5Td9KUOZD0bvJq80dIET53jxT+PmoM/5tmpsFsayWcFeylrDS
uQEF0qeaBssTtKnfBUB6ZweK6AZUFXBNQo5/Xa8Iwq3yBc8Hs99ntIlzu2kbhaV/
An42oCarp2s5KC4Wqwv81Ih2Qr5oOE2s9RmgtMg0O1/D6tQx+30DNY53BXfJo0jX
GNB8P4tPxD8VEtZ1Hx4sK/RJb7fN5tRtrGjYNv0p6+W1y0IYN21yq+FrbLopxPEp
WIDbg6hFVPYzghyDUi4tH1jk03AIgt1TpuBbsO84ydPPsKoPiFXqMAg/
=Lftp
-----END PGP MESSAGE-----




