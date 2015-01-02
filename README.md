# What is Gizmo?

Gizmo is an effortless way to create web applications in Clojure.


Gizmo is a set of practices we've accumulated from several Web
applications and APIs developed with Clojure. It's an MVC
microframework, which lets you develop parts of your app completely
independently, which improves composition and allows you to effortlessly
implement things like A/B testing and gradual feature rollouts.

Gizmo uses Enlive under the hood, so you will be able to let your front-end team
work on HTML, CSS and JavaScript without interfering with your server development team,
but also provides means for making sure their accidental changes (e.g. moved or deleted
HTML entries) do not break application code.

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

;; Render HTML response with `index-content` widget as main content
(defn index-html
  [env]
  {:render :html
   :status 200
   :widgets {:main-content gizmo-cloc.widgets.home/index-content}))
```

You can also use function or literals within `:widgets` clause, such as:

```
:widgets {:main-content "some content"}

;; Or

:widgets {:main-content (fn [_] "some content") }
```

That will create a "meta-widget".

JSON rendering in Gizmo is just what you expect it to be: you return a map,
it is serialized into JSON and returned to Jetty.

HTML rendering it's a bit more involved and includes a few concepts that help you
to build modular Web applications.

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

### Layouts

Layout is an outlining template that's shared between several pages on your
website. Usually it's a set of common surroundings of an HTML page.

```clj
;; snippets/layout.clj
(ns gizmo-cloc.snippets.layouts
  (:require [clojurewerkz.gizmo.widget :refer [deflayout]]))

(deflayout application-layout "templates/layouts/application.html"
  [])
```

First defined layout will become a default layout for your application. If you
have more than one layout, we stronly recommend you to specify layout explicitly
at all times in order to avoid cases when, due loading order, wrong layout
gets picked up as a default.

In order to specify layout use `layout` key in your handler return hash:

```clj
(defn index-html
  [env]
  {:render :json
   :layout :application-layout})
```

### Widgets

Widget is a reusable entry that represents any part of your website. Examples
include things like header, login form, user profile, or even a complete page
within a layout. In some other frameworks, widgets are called partials
or nested templates.

Widget consists of two parts: `view` and `fetch`. `fetch` is a
function that receives a complete environment from `handler`.

`fetch` is a function that receives an environment and runs some code, potentially
involving disk or network I/O. Sometimes `fetch` is used just to get a part of
environment that's applicable for a particular view.

We recommend using Enlive for views, but `view` can return a string with HTML
elements generated by any other rendering engine, like Stencil, Hiccup or
your own HTML generation library.

A widget's `fetch` operations and `view` operations done in parallel with other
widgets. `fetch` is where I/O operations go. Both `view` should be
side-effect free, since the result will be cached. It it possible to turn
caching off, too.

```clj
;; handlers/main.clj
(defn my-hander
  [{:keys [route-params]}]
  {:render :html
   :library (:library route-params)
   :namespace (:namespace route-params)
   :main-content  'gizmo-cloc.widgets.main/library-namespace-docs})

;; widgets/main.clj
(ns gizmo-cloc.widgets.main
    (:require [clojurewerkz.gizmo.widget :refer [defwidget]]
              [gizmo-cloc.snippets.main :as snippets]
              [gizmo-cloc.entities :as entities]))

(defwidget library-namespace-docs
  :view snippets/library-namespace-docs-snippet
  :fetch (fn [{:keys [library namespace]}]
           {:library      library
            :namespace    namespace
            :docs         (entities/docs library-name namespace)}))
```

Here, Gizmo passes a hash from `my-handler` straight to the `fetch` function of the `library-namespace-docs` widget,
which performs a query to retrieve all docstrings for namespace of a library (the call to `entieies/docs`).
Once again, `fetch` operations of widgets that are found on the page are done
in parallel. This does not immediatly apply to nested widgets:, in these cases the parent widget
will be rendered first, and after parent widget is rendered it's nested widgets
will be also fetched and rendered in parallel.

### Snippets

A snippet is the `view` part of a widget, or a piece of HTML code that should be rendered
within some other snippet.

For example, here's an HTML snippet for rendering a list of libraries in your
classpath:

```html
<div snippet="libraries-snippet">
  <h2>Libraries</h2>
  <ul snippet="libraries-list" class="list-unstyled">
    <li snippet="libraries-list-item">
      <a href="${library-path}">${library}</a>
    </li>
  </ul>
</div>
```

`snippet` html attribute generates a selector that can be referenced within `defsnippet`.
For example for `libraries-snippet`, the `*libraries-snippet` selector is created.

This is helpful for many reasons:

  * CSS selectors and IDs are flawed as identifiers between front-end and server-side as they're changed frequently and
    are required for other parts of application (front-end, specifically) to function correctly. You don't want
    to change your application code every time someone changes CSS class, ID or even tag of an element.
  * Since templates are loaded during macro expansion, you can catch errors and
    incompatibilities between HTML and application code during compilation time. If
    there was an element with `snippet` attribute is removed, it's `*` selector is
    unavailable, so you'll get a compile time exception.
  * Don't have to take care about positioning of elements and create additional means
    to specify their location. You add `snippet` HTML attribute once, reference it
    within `defsnippet`, and you're good to go, no additional effort involved.

Here is a snippet that will actually render libraries from your classpath
to HTML:

```clj
(ns gizmo-cloc.snippets.main
  (:require [net.cgrand.enlive-html :as html]
            [gizmo-cloc.routes :as routes]
            [clojurewerkz.gizmo.enlive :refer [defsnippet within]]))

(defsnippet index-snippet "templates/main/index.html"
  [*libraries-snippet]
  [libraries]
  (within *libraries-list [*libraries-list-item])
  (html/clone-for [library libraries]
                  [html/any-node] (html/replace-vars {:library-path (routes/library-path :library library)
                                                      :library library})))
```

`replace-vars` will take care of argument interpolation, `clone-for` will take list of
libraries and create a `libraries-list-item` for each one of them.

### Services

Services are used to give you the flexibility of creating a long-running processes within your
application. Typical examples are `jetty` webserver and `nrepl` server that are used in
nearly all Clojure apps.

To create a service, you have to give it `start`, `stop`, `alive` and config functions.

  * `config` is a function that returns configuration for a service or a hardcoded configuration
    value
  * `start` is called in a separate thread, and is responsible for service startup
  * `alive` is used to check wether service is still alive
  * `stop` is responsible for stopping the service

For example, here's a service that manages a `jetty` server:

```clj
(ns gizmo-cloc.services.jetty
  ^{:doc "Jetty service"}
  (:use [clojurewerkz.gizmo.service])
  (:require [ring.adapter.jetty :as jetty]
            [clojurewerkz.gizmo.config :as config]
            [gizmo-cloc.core :as app-core]))

(defservice jetty-service
  :config #(:jetty config/settings)
  :alive (fn [service]
           (and service
               (state service)
               (.isRunning (state service))))
  :stop (fn [service]
          (.stop (state service)))
  :start (fn [service]
           (reset-state service
                        (jetty/run-jetty #'app-core/app (config service)))))
```

You can start it with

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

### Example applications

Reference application that demonstrates core principles of web development with Gizmo can be found
[here](https://github.com/ifesdjeen/gizmo-cloc).



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

Copyright © 2014-2015 Oleksandr Petrov (CodeCentric AG), Michael Klishin

Double licensed under the Eclipse Public License (the same as Clojure) or
the Apache Public License 2.0.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/clojurewerkz/gizmo/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
