(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [golf.manager :as manager])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

;; websocket utils

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

;; websocket handlers

(defn handle-login [context channel message]
  (let [name (:name message)
        user (manager/get-user-by-channel context channel)
        id (:id user)
        response (encode-message {:type :login
                                  :id id
                                  :name name})]
    (if-not (nil? user)
      (log/info "logging in:" name)
      (log/error "user not found. channel:" channel))
    (manager/login! context id name)
    (async/send! channel response)))

(defn handle-logout [context channel message]
  (let [id (:id message)
        response (encode-message {:type :logout
                                  :id id})]
    (log/info "logging out:" id)
    (manager/login! context id "anon")
    (async/send! channel response)))

(defn handle-create-game [context channel]
  (let [user (manager/get-user-by-channel context channel)
        game (manager/make-game)
        response (encode-message {:type :game-created
                                  :game game})]
    (log/info "user:" (:id user) "connected to game:" (:id game))
    (manager/add-game! context game)
    (manager/connect-user-to-game! context (:id user) (:id game))
    (async/send! channel response)))

(defn handle-get-games [context channel]
  (let [games (manager/get-games context)
        response (encode-message {:type :get-games
                                  :games games})]
    (log/info "sending games")
    (async/send! channel response)))

(defn handle-start-game [context channel {:keys [id]}]
  (let [game (manager/start-game! context id)
        response (encode-message {:type :game-started
                                  :game game})]
    (log/info "game started:" game)
    (async/send! channel response)))

(defn on-open [context channel]
  (let [user (manager/make-user channel)]
    (log/info "user created:" user)
    (manager/add-user! context user)))

(defn on-close [context channel {:keys [code reason]}]
  (let [{:keys [id]} (manager/get-user-by-channel context channel)]
    (log/info "channel closed. code:" code "reason:" reason)
    (log/info "user deleted:" id)
    (manager/remove-user! context id)))

(defn on-message [context channel raw-message]
  (let [message (decode-message raw-message)
        type (:type message)]
    (log/info "message received:" message)
    (case type
      :login (handle-login context channel message)
      :logout (handle-logout context channel message)
      :create-game (handle-create-game context channel)
      :get-games (handle-get-games context channel)
      :start-game (handle-start-game context channel message)
      (log/warn "no matching message type:" message))))

#_(defn send-game-update [channel game-id]
  (async/send! channel (get @games game-id)))

;(defn notify-clients! [msg]
;  (doseq [chan @channels]
;    (async/send! chan msg)))

(def websocket-callbacks
  {:on-open (partial on-open manager/context)
   :on-close (partial on-close manager/context)
   :on-message (partial on-message manager/context)})

(defn websocket-handler [request]
  (async/as-channel request websocket-callbacks))

(defn websocket-routes []
  [["/ws" websocket-handler]])
