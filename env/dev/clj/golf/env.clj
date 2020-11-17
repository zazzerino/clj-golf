(ns golf.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [golf.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[golf started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[golf has shut down successfully]=-"))
   :middleware wrap-dev})
