# What is Gizmo

Gizmo is an effortless way to create web applications in Clojure.

Gizmo is a set of practices we've accumulated from several Web
applications and API's developed with Clojure. It's a MVC
microframework, which lets you develop parts of your app in completely
independently, which improves composition and allows you effortlessly
implement things like A/B testing, gradual feature rollouts.

Gizmo uses Enlive under the hood, so you will be able to let your front-end team to
work on HTML, CSS and JavaScript without interferring with Web development team, but
also provides means for making sure their accidental changes (e.g. moved or deleted
HTML entries) do not break application code.

## Project Goals

Gizmo is not a replacement for Ring or Compojure. It's based on them, and doesn't
re-implement their features.

  * Provide a convenient, idiomatic way of develoing Clojure Web apps
  * Give you set of building blocks to bring you up to speed as fast as possible
  * Leave infinite flexibility in terms of all configuration and composition
    decisions
  * Help you to establish reasonable convention on where to put what (handlers,
    services, routes, HTML, CSS, and so on)
  * Be well documented
  * Be well tested

## Project Maturity

Principles that are represented in Gizmo are battle-tested and proven to work very well
on large Clojure web applications, however Gizmo as a library is very young and there
may be breaking API changes until stable release.

## Community

Gizmo does not yet have it's own mailing list. This will be resolved as soon as first
artifacts are pushed to Clojars.

To subscribe for announcements of releases, important changes and so on, please follow
[@ClojureWerkz](https://twitter.com/clojurewerkz) on Twitter.

## Documentation

### Request Lifecycle

When HTTP request is coming to your application, it's received by Jetty webserver and
pushed into middleware stack. Middlewares add required entries (such as session,
cookies, route params, authentication tokens) to request hash and hand it over to
routing function, which figures out which handler the request should be routed to.

Handler is preparing the response and gives all required information about HTTP
response code, response body and type and hands it to responder. Depending on
response type chosen, one of renderers is called (for exmaple, HTML or JSON).

Rendrerer renders a complete response body and gives resulting hash back to Jetty,
so that it could be returned to requester.

### Request, Response and Environment

Even though Request, Response and Environment are pretty much same thing, just
on different steps of processing, we decided to make the separation of these
concepts to be able to communicate it in a better way.

`request` is an initial request from the browser (or API client, if you wish),
which contains information about Referrer, User Agent, called URI, Path. Everything
that's related to the normal HTTP request.

`environment` is request, processed and refined by middleware stack and handler.

It becomes `response` after it went through middlewares, handler and renderer
and is ready to be returned back to the client.

These terms are more or less interchangeable, but you can use them to specify
which part of processing you're referring to.

In all parts of your application, you can always refer to current (immutable)
state of request by calling `clojurewerkz.gizmo.request/request` function.
We strongly advise not to overuse availability of a complete request and always
pass required parts of request to all functions explicitly. Although it's
hard to draw a boundary where it is acceptable, just keep in mind that it will
make your code less explicit and testable.

### Middleware

Middleware is a function that receives a request and modifies it in some way.
Middleware can either stop execution itself and return result or pass it
to the next middleware.

Here's what middleware looks like:

```clj
(defn wrap-authenticated-only
  [handler]
  (fn [env]
    (if (user-authenticated? env)
      (handler (assoc env :new-key :new-value))
      {:status 401 :body "Unauthorized"})))
```

You can see that there're two execution paths here: if user is authenticated,
the underlying handler is called, so middleware stack execution is continued,
otherwise middleware returns 401 Unauthorized response and prevents further
execution of the stack.

In order to create a middleware stack, you thread the handler through set of
middlewares, wrapping handler into the middleware, then wrapping resulting
stack into another middleware function and so on.

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

Routing recognizes URLs and dispatches them to particular handler. It also
generates helper functions for creating Paths and URLs so that you wouldn't
need to hardcode them and could specify them once for both parsing and
generation purposes.

Following code defines routes for a simple application that's showing you
docstrings of all the libraries in your Clojure classpath.

Root path "/" is handled by `main/index` handler function.
Library path "/libs/:library" is handled by `main/library-show` handler function,
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

Handler is reponsible for the incoming request for particular URL. Handler
is called if routing found that request URL matched route for it. Handler
receives an Environment, that's been processed by middleware stack
and returns a hash that's passed to responder.

You can have full control over response params in `response`. For example,
you can specify `status`, `headers` and so on. In order to specify
type of your response, you add `render` with value of `html` or `json`
(which are built-in renderers), for example:

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
  {:render :json
   :status 200
   :widgets {:main-content gizmo-cloc.widgets.home/index-content}))
```

Since most of things related to `json` responses are more or less straightforward
(you specify response-hash and it's returned straight to caller), with `html`
it's a bit different, since we provide you with several concepts that help you
to build modular web applications, and widgets are a big part of it.

### Layouts

Layout is a high-level template that's shared between several pages on your
website. Usually it's a set of common surroundings of an HTML page.

```clj
;; snippets/layout.clj
(ns gizmo-cloc.snippets.layouts
  (:require [clojurewerkz.gizmo.widget :refer [deflayout]]))

(deflayout application-layout "templates/layouts/application.html"
  [])
```

You can have any valid HTML code in your `application.html`. This part is totally
up to you.

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
within some layout.

Widget consists of `view` and `fetch` parts. `fetch` receives a complete environment
from `handler`.

We recommend using Enlive for views, but `view` can return a string with HTML
elements generated by any other rendering engine, like Clostache, Hiccup or
your own HTML-generation library.

Widgets `fetch` operations and `view` operations done in parallel with other
widgets. `fetch` is advised for I/O operations. Both `view` and `fetch` should be
side-effect free, since their results will be cached. It it possible to turn
of caching alltogether, but it's not an excuse to utilize functions for what's
not their intention.


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

Here, hash from `my-handler` is passed straight to `fetch` function of the widget,
and it's performing a query to retrieve all docstrings for namespace of some library.
Once again, `fetch` operations of widgets that are found on the page are done
in parallel. It does not apply for nested widgets, since in that case parent widget
should be rendered first, but after parent widget is rendered, it's nested widgets
will be also fetched and rendered in parallel.

### Snippets

Snippet usually `view` part of widget, or a part of HTML code that should be rendered
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
For example for `libraries-snippet`, `*libraries-snippet` selector is created.

This is helpful due to many reasons:

  * CSS selectors and IDs are flawed, they're changed frequently and required for other
    parts of application (front-end, specifically) to function correctly. You don't want
    to change your application code every time someone changes CSS class, ID or even
    tag of an element.
  * Since templates are loaded during macro expansion, you can catch errors and
    incompatibilities between HTML and application code during compilation time. If
    there was an element with `snippet` attribute is removed, it's `*` selector is
    unavailable, so you'll get a compile time exception.
  * Don't have to take care about positioning of elements and create additional means
    to specify their location. You add `snippet` HTML attribute once, reference it
    within `defsnippet`, and you're good to go, no additional effort involved.

Now, let's create a snippet that will actually render libraries from your classpath
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

Services are used to give you flexibility of creating a long-running processes within your
application. Typical examples are `jetty` webserver and `nrepl` server that are used in
nearly all Clojure apps.

To create a service, you have to give it `start`, `stop`, `alive` and config functions.

  * `config` is a function that returnc configuration for a service or hardcoded configuration
    value.
  * `start` is called in a separate thread, and is responsible for service startup.
  * `alive` is used to check wether service is still alive
  * `stop` is responsible for stopping the service

For example, here's a service that manages `jetty` server:

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
and more complex example of cooperative [UDP socket listener here](https://github.com/clojurewerkz/gizmo/blob/master/examples/services/udp_service.clj).

### Configuration

Configuration file is loaded by `clojurewerkz.gizmo.config/load-config!`, which receives
a path to configuration file and loads it to `clojurewerkz.gizmo.config/settings` variable,
that's available at all times.

### Leiningen project templates

You can use [Gizmo Leiningen project template](https://github.com/ifesdjeen/gizmo-web-template)
to generate Gizmo application skeletons.

### Example applications

Reference applicaiton that demonstrates core priniples of web development with Gizmo can be found
[here](https://github.com/ifesdjeen/gizmo-cloc).

## Development

Gizmo uses [Leiningen 2](http://leiningen.org). Make sure you have it installed and then run
tests against supported Clojure versions using

```
lein all test
```

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.

## License

Copyright © 2013 Oleksandr Petrov (CodeCentric AG), Michael Klishin

Double licensed under the Eclipse Public License (the same as Clojure) or
the Apache Public License 2.0.
