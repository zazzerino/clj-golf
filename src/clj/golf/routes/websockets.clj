(ns golf.routes.websockets
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]))

(defonce users (atom #{}))

(defn make-user [channel]
  {:id (str (java.util.UUID/randomUUID))
   :channel channel
   :name nil
   :game nil})

(defn connect! [channel]
  (let [user (make-user channel)]
    (swap! users conj user)
    (log/info (str "user connected: " user))))

(defn disconnect! [channel {:keys [code reason]}]
  (swap! users (fn [users]
                 (set (remove #(= (:channel %) channel)
                              users))))
  (log/info "close code:" code "reason:" reason))

(defn notify-clients! [channel msg]
  (doseq [user @users]
    (async/send! (:channel user) msg)))

(def websocket-callbacks
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(def websocket-routes
  [["/ws" ws-handler]])
