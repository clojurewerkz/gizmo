# What is Gizmo?

Gizmo is an effortless way to create web applications in Clojure.


Gizmo is a set of practices we've accumulated from several Web
applications and APIs developed with Clojure. It's an MVC
microframework, which lets you develop parts of your app completely
independently, which improves composition and allows you to effortlessly
implement things like A/B testing and gradual feature rollouts.

## Project Goals

Gizmo is not a replacement for Ring or Compojure. It's based on them, and doesn't
re-implement their features.

  * Provide a convenient, idiomatic way of developing Clojure web apps
  * Give you a set of building blocks to bring you up to speed as fast as possible
  * Leave infinite flexibility in terms of all configuration and composition
    decisions
  * Help you to establish reasonable convention on where to put what (handlers,
    services, routes, HTML, CSS, and so on)
  * Be well documented
  * Be well tested

## Project Maturity

Principles that are represented in Gizmo are battle-tested and proven
to work very well for large Clojure Web applications. Gizmo as a
library is very young and breaking API changes currently can happen at
any point without prior notice.

## Maven Artifacts

### Most Recent Release

With Leiningen:

    [clojurewerkz/gizmo "1.0.0-alpha2"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>eep</artifactId>
      <version>1.0.0-alpha2</version>
    </dependency>

## Documentation

### Intro

Gizmo is a collection of good practices that ties multiple Clojure Web
development libraries and a few concepts together (similar to
DropWizard in Java, although slightly more opinionated).

With Gizmo, you build HTTP apps as one or more *services*, each of which can
be started, stopped and performed a health check on. Request handling
is implemented as a pipeline, which starts with a Ring request, includes
a number of middlewares, a handler (picked by the router) and a responder.

Gizmo separates UI elements from HTTP request handling, and request
handling logic from serving the response.

### HTTP Request Lifecycle

Incoming HTTP requests are handled by Jetty and processed through a
middleware stack. Middleware implements session handling, cookies,
route parameters extraction, authentication, etc. A middleware takes a
request hash hands it over to the routing function, which figures out
which handler the request should be routed to.

Handler prepares the response and returns HTTP response code, response
body and content type, and hands this hash over to responder. Depending on
response content type, an appropriate renderer is invoked (for
exmaple HTML or JSON).

Renderer renders a complete response body and returns the result
back to Jetty, which sends it back to the client.

### Request, Response and Environment

Even though Request, Response and Environment are closely related to each other,
Gizmo separates these concepts. These (plus middleware) concepts are taken directly
from [Ring](https://github.com/ring-clojure/ring).

`request` is an initial request from a HTTP client,
which contains information about the referrer, user agent, path and so on.

`environment` is a request that has been processed and refined by
the middleware stack and request handler.

`environment` becomes `response` after it has been through the
middleware, handler and renderer and is ready to be returned back to
the client.

With this separation, you can refer to a specific part of request processing pipeline.

In all parts of your application, you can always refer to current (immutable)
state of request by calling `clojurewerkz.gizmo.request/request` function.
We strongly advise not to overuse availability of a complete request and always
pass required parts of request to all functions explicitly. Although it's
hard to draw a boundary where it is acceptable, just keep in mind that it will
make your code less explicit and testable.

### Middleware

A middleware is a function that receives a request and modifies it.
Middleware can terminate execution of request processing or return a result, or pass the
request on to the next middleware.

Here's what middleware looks like:

```clj
(defn wrap-authenticated-only
  [handler]
  (fn [env]
    (if (user-authenticated? env)
      (handler (assoc env :new-key :new-value))
      {:status 401 :body "Unauthorized"})))
```

There are two execution paths here: if the user is authenticated,
a request handler is called, so request processing is continued,
otherwise middleware returns `401 Unauthorized` response and halt further
request processing.

In order to create a middleware stack, you thread the handler through
set of middlewares, wrapping handler into the middleware, then
wrapping resulting stack into another middleware function and so on.

```clj
(ns my-app.core
  (:require [compojure.handler :refer [api]]
            [ring.middleware.params :refer [wrap-params]]
            [my-app.routes :as routes]))
(def app
  (-> (api routes/main-routes)
      wrap-params))
```

## Routing

Routing in Gizmo is built upon Compojure and [Route One](https://github.com/clojurewerkz/route-one).

Routing recognizes URLs and dispatches them to a suitable handler. It also
generates helper functions for creating Paths and URLs so that you wouldn't
need to hardcode them and could specify them once for both parsing and
generation purposes.

Following code defines routes for a simple application that's showing you
docstrings of all the libraries in your Clojure classpath.

Root path "/" is handled by `main/index` handler function.
Library path "/libs/:library" is handled by `main/library-show`,
and so on.

```clj
(ns gizmo-cloc.routes
    (:use [clojurewerkz.route-one.compojure])
    (:require [compojure.core :as compojure]
              [compojure.route :as route]))

(compojure/defroutes main-routes
  (GET root      "/"                             request (gizmo-cloc.handlers.main/index request))
  (GET library   "/libs/:library"                request (gizmo-cloc.handlers.main/library-show request))
  (GET namespace "/libs/:library/nss/:namespace" request (gizmo-cloc.handlers.main/namespace-show request))
  (GET favicon   "/favicon.ico"                  _       (fn [_] {:render :nothing}))
  (route/not-found "Page not found"))
```

You can use generated routes by adding `-path` postfix for paths and `-url`
postfix for URLs. You can find in-depth documentation for route parsing
and generation in [Route One](https://github.com/clojurewerkz/route-one).

### Handlers

A handler is a function responsible for requests matching a particular
URL pattern. Handler take an *environment*, a request that's been
processed by middleware stack, and returns a hash that's passed to
a responder.

You can have full control over response params in `response`. For example,
you can specify `status`, `headers` and so on. In order to specify
type of your response, set `:render` key to either `"html"` or `"json"`
(two built-in renderers), for example:

```clj
;; Render :response-hash and return it as JSON response
(defn index-json
  [env]
  {:render :json
   :status 200
   :response-hash {:response :hash}))
```

### Responders

In order to implement a custom response MIME type, use multimethods extending `respond-with`. For example,
if you want to add an XML responder, you can write:

```clj
(ns my-app
  (:require [clojurewerkz.gizmo.responder :refer [respond-with]])

(defmethod respond-with :xml
  [env]
  {:status 200
   :body (xml/serialize (:response-hash env))})
```

```clj
(start jetty-service)
;; or you can start all services together
(start-all!)
```

You can check [nrepl service example here](https://github.com/clojurewerkz/gizmo/blob/master/examples/services/nrepl_service.clj)
and a more complex example of cooperative [UDP socket listener here](https://github.com/clojurewerkz/gizmo/blob/master/examples/services/udp_service.clj).

### Configuration

Configuration is a file loaded by `clojurewerkz.gizmo.config/load-config!`, which takes
a path to configuration file and loads it to `clojurewerkz.gizmo.config/settings` variable,
that's available at all times.

### Leiningen project templates

You can use [Gizmo Leiningen project template](https://github.com/ifesdjeen/gizmo-web-template)
to generate Gizmo application skeletons.

You can get up and running with it by creating a new template and running it:
```
lein new gizmo-web my-app
cd my-app
lein run --config config/development.clj
```

## Community

Gizmo does not yet have it's own mailing list. This will be resolved as soon as first
artifacts are pushed to Clojars.

To subscribe for announcements of releases, important changes and so on, please follow
[@ClojureWerkz](https://twitter.com/clojurewerkz) on Twitter.

## Development

Gizmo uses [Leiningen 2](http://leiningen.org). Make sure you have it installed and then run
tests against supported Clojure versions using

```
lein all test
```

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.

## License

Copyright © 2014-2015 Alex Petrov, Michael Klishin

Double licensed under the Eclipse Public License (the same as Clojure) or
the Apache Public License 2.0.
