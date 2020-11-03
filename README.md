# leihs-procure





## shared/globals

### Error Codes

##### `NOT_AUTHORIZED_FOR_APP`

User is logged into leihs, but has no permission to use the application.

##### `UNKNOWN_SERVER_ERROR`

#### generic server errors

those are used in generic exception handlers and could be improved:

- `API_ERROR`
- `DATABASE_ERROR`

#### generic client errors

those are used/infered by the client only

##### `UNKNOWN_NETWORK_ERROR`

apollo client "networkError", but without ErrorCode.
Could also mean "unparseable response", e.g. some plain text error from a proxy.

##### `NO_CONNECTION_TO_SERVER`

no HTTP connection to server. Likely the user is offline, or the server is down.
