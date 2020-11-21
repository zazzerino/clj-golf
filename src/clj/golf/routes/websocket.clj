(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit])
  (:import (java.util UUID)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def channels (atom #{}))
(def users (atom {}))

(defn generate-id []
  (str (UUID/randomUUID)))

(defn decode-message [message]
  (let [bytes (.getBytes message)
        in (ByteArrayInputStream. bytes)
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn encode-message [message]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer message)
    (.toString out)))

(defn make-user [channel name]
  {:id (generate-id)
   :channel channel
   :name name})

(defn get-user-id-by-channel [users channel]
  (-> (filter #(= channel (:channel %)) (vals users))
      first
      :id))

(defn on-open [channel]
  (log/info "channel open:" channel)
  (swap! channels conj channel))

(defn on-close [channel {:keys [code reason]}]
  (log/info "channel closed. code:" code "reason:" reason)
  (swap! channels #(into (empty %) (remove #{channel} %)))
  (if-let [user-id (get-user-id-by-channel @users channel)]
    (swap! users dissoc user-id)))

(defn handle-login [channel message]
  (let [name (:name message)
        user (make-user channel name)
        response (encode-message {:type :login
                                  :id (:id user)
                                  :name name})]
    (swap! users assoc (:id user) user)
    (async/send! channel response)))

(defn on-message [channel raw-message]
  (let [message (decode-message raw-message)]
    (log/info "message received:" message)
    (case (:type message)
      :login (handle-login channel message)
      (log/warn "no matching message type:" message))))

;(defn notify-clients! [msg]
;  (doseq [chan @channels]
;    (async/send! chan msg)))

(def websocket-callbacks
  {:on-open on-open
   :on-close on-close
   :on-message on-message})

(defn websocket-handler [request]
  (async/as-channel request websocket-callbacks))

(defn websocket-routes []
  [["/ws" websocket-handler]])
