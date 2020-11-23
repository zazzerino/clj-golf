(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [golf.game :as game])
  (:import (java.util UUID)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def channels (atom #{}))
(def users (atom {}))
(def games (atom {}))

(defn- reset-state! []
  (reset! users {})
  (reset! games {}))

(defn uuid []
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
  {:id (uuid)
   :channel channel
   :name name})

(defn get-user-id-by-channel [users channel]
  (-> (filter #(= channel (:channel %)) (vals users))
      first
      :id))

(defn logout-user! [id]
  (swap! users dissoc id))

(defn create-game! []
  (let [game (game/make-game)
        id (:id game)]
    (swap! games assoc id game)
    id))

(defn connect-user-to-game! [user-id game-id]
  (let [user (-> (get @users user-id)
                 (assoc :game-id game-id))
        player (game/make-player user-id (:name user))
        game (-> (get @games game-id)
                 (game/add-player player))]
    (swap! users assoc user-id user)
    (swap! games assoc game-id game)))

(defn delete-game! [id]
  (swap! games dissoc id))

(defn on-open [channel]
  (log/info "channel open:" channel)
  (swap! channels conj channel))

(defn on-close [channel {:keys [code reason]}]
  (log/info "channel closed. code:" code "reason:" reason)
  (swap! channels #(into (empty %) (remove #{channel} %)))
  (if-let [user-id (get-user-id-by-channel @users channel)]
    (logout-user! user-id)))

(defn handle-login [channel message]
  (let [name (:name message)
        user (make-user channel name)
        response (encode-message {:type :login-ok
                                  :id (:id user)
                                  :name name})]
    (swap! users assoc (:id user) user)
    (async/send! channel response)))

(defn handle-logout [channel message]
  (let [id (:id message)
        response (encode-message {:type :logout-ok
                                  :id id})]
    (log/info "logging out:" id)
    (logout-user! id)
    (async/send! channel response)))

(defn handle-create-game [channel message]
  (let [user-id (:user-id message)
        game-id (create-game!)]
    (connect-user-to-game! user-id game-id)
    (async/send! channel
                 (encode-message {:type :game-created
                                  :game (get @games game-id)}))))

(defn on-message [channel raw-message]
  (let [message (decode-message raw-message)]
    (log/info "message received:" message)
    (case (:type message)
      :login (handle-login channel message)
      :logout (handle-logout channel message)
      :create-game (handle-create-game channel message)
      (log/warn "no matching message type:" message))))

(defn send-game-update [channel game-id]
  (async/send! channel (get @games game-id)))

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
