# COSV Frontend

### Building
* For IR usage see https://github.com/JetBrains/kotlin-wrappers#experimental-ir-backend

To run frontend locally, use `./gradlew :cosv-frontend:browserDevelopmentRun --continuous` or `./gradlew :cosv-frontend:browserProductionRun --continuous`.

To pack distribution, use `./gradlew :cosv-frontend:browserDevelopmentWebpack` and `./gradlew :cosv-frontend:browserProductionWebpack`.

save-backend consumes frontend distribution as a dependency. Frontend distribution is copied and included in spring boot resources.

### `nginx` [configuration](../frontend-common/README.md)

### `webpack-dev-server` [configuration for no `api-gateway` run](../frontend-common/README.md)

### [Using OAuth with a local deployment (`api-gateway` on)](../frontend-common/README.md)
