(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit])
  (:import (java.util UUID)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def channels (atom #{}))
(def users (atom {}))

(defn uuid []
  (str (UUID/randomUUID)))

(defn decode-message [msg]
  (let [bytes (.getBytes msg)
        in (ByteArrayInputStream. bytes)
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn encode-message [msg]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer msg)
    (.toString out)))

(defn on-open [channel]
  (swap! channels conj channel))

(defn on-close [channel {:keys [code reason]}]
  (swap! channels #(into (empty %) (remove #{channel} %))))

(defn on-message [channel msg]
  (let [body (decode-message msg)]
    (log/info "message received:" body)
    (case (:type body)
      (log/warn "no matching message type:" body))))

;(defn on-message [channel raw-msg]
;  (let [msg (convert-message raw-msg)]
;    (log/info (str "message received: " msg))
;    (case (:type msg)
;      :login (handle-login channel msg))
;    ;(async/send! channel (encode-message (assoc msg :type :ok)))
;    ))

;(defn notify-clients! [msg]
;  (doseq [chan @channels]
;    (async/send! chan msg)))

(def websocket-callbacks
  {:on-open on-open
   :on-close on-close
   :on-message on-message})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defn websocket-routes []
  [["/ws" ws-handler]])
