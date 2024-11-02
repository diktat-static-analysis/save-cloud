# SaveOurTool gateway

## Overview
Here is a brief scheme of routing in cluster:
1. The Ingress proxies requests to `api-gateway` (configured in `ingress.yml`);
2. `api-gateway` forwards requests to Spring services as well as `save-frontend` (configured in `application.yml` and `WebSecurityConfig.kt`);
3. `save-frontend` is a pod that has `nginx` running in it (configured in `nginx.conf`);
4. All further routing is configured in `basicRouting` functional component, defined in `save-frontend` module.

### 1. Ingress:
The Ingress proxies requests to the API Gateway (configured in `ingress.yml`).

In Kubernetes, an Ingress is a resource that manages external access to services within the cluster.
It acts as a layer of traffic routing and load balancing.
Existed ingress configuration defines two routing rules for the Ingress resource.
 - paths that start with `/grafana/**` are forwarded to `grafana` service.
 - all the other paths `/**` are forwarded to `api-gateway` spring service.

### 2. `api-gateway`
`api-gateway` uses Spring Cloud Gateway (configured in `application.yml` and `WebSecurityConfig.kt`).

`api-gateway` acts as a request router and filter for `SaveOurTool`.
It forwards requests to specific backend services based on their paths
and performs some necessary filtering and header manipulation to ensure proper communication between the gateway and downstream services.
The routes are defined based on specific URI paths:
 - `/sec/**` is not forwarded anywhere but `api-gateway`'s `SecurityInfoController`,
    which is responsible for `/sec/oauth-providers` endpoint
 - `/api/demo` is forwarded to `save-demo`
 - `/api/cpg` is forwarded to `save-cpg-demo`
 - `/api/**` is forwarded to `save-backend`
 - `/neo4j/browser/**` is forwarded to `neo4j-browser`
 - all the remaining requests (including resources requests) are forwarded to `save-frontend`

### 3. `save-frontend`
`save-frontend` is a pod that has `nginx` running in it (configured in `nginx.conf`).

An entry point to scripts built from `save-frontend` module is `index.html`,
so it is required to return it whenever user requests something but not resource. 

After `save-frontend` pod serves `index.html`, scripts are fetched and the remaining routing is done with `basicRouting` functional component.

For more info take a look into `save-frontend/README.md`

## Local deployment case
The main difference between `dev` and `prod` cases is that `prod` has `kubernetes` `ingress`, while `dev` uses `webpack-dev-server`.

`webpack-dev-server` starts dev server, that does some routing (for more see `dev-server.js` and `save-deploy/README.md`).
**If none of proxies match**, `webpack-dev-server` serves the `index.html` (`historyApiFallback` parameter is used).

`webpack-dev-server` is configured in `dev-server.js`.
