(ns golf.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[golf started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[golf has shut down successfully]=-"))
   :middleware identity})
