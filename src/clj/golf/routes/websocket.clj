(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [golf.game :as game])
  (:import (java.util UUID)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def state (atom {:users {}
                  :games {}}))

;; websocket utils

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

;; users

(defn make-user
  ([channel name]
   {:id (uuid)
    :channel channel
    :name name})
  ([channel]
   (make-user channel "anon")))

(defn create-user! [user]
  (swap! state update-in [:users] conj {(:id user) user})
  user)

(defn delete-user! [id]
  (swap! state update-in [:users] dissoc id))

(defn set-user-name! [user name]
  (swap! state update-in [:users (:id user)] assoc :name name))

(defn get-user-by-id [id]
  (get-in @state [:users id]))

(defn get-user-by-channel [channel]
  (let [users (-> @state :users vals)]
    (first (filter #(= channel (:channel %)) users))))

;; games

(defn create-game! []
  (let [game (game/make-game)]
    (swap! state update-in [:games] conj {(:id game) game})
    game))

(defn delete-game! [id]
  (swap! state update-in [:games] dissoc id))

(defn get-game-by-id [id]
  (get-in @state [:games id]))

(defn connect-user-to-game! [user-id game-id]
  (let [user (-> (get-user-by-id user-id)
                 (assoc :game-id game-id))
        player (game/make-player user-id (:name user))
        game (-> (get-game-by-id game-id)
                 (game/add-player player))]
    (swap! state (fn [state]
                   (-> state
                       (assoc-in [:users user-id] user)
                       (assoc-in [:games game-id] game))))
    game))

;; websocket messages

(defn on-open [channel]
  (let [user (make-user channel)]
    (log/info "user created:" user)
    (create-user! user)))

(defn on-close [channel {:keys [code reason]}]
  (let [user (get-user-by-channel channel)]
    (log/info "user deleted:" user)
    (delete-user! (:id user))))

(defn handle-login [channel payload]
  (let [name (:name payload)
        user (get-user-by-channel channel)
        response (encode-message {:type :login-ok
                                  :payload {:id (:id user)
                                            :name name}})]
    (log/info "logging in:" name)
    (set-user-name! user name)
    (async/send! channel response)))

(defn handle-logout [channel payload]
  (let [id (:id payload)
        user (get-user-by-id id)
        response (encode-message {:type :logout-ok
                                  :payload {:id id}})]
    (log/info "logging out:" id)
    (set-user-name! user "anon")
    (async/send! channel response)))

(defn handle-create-game [channel]
  (let [user (get-user-by-channel channel)
        game (create-game!)
        game (connect-user-to-game! (:id user) (:id game))
        response (encode-message {:type :game-created
                                  :payload {:game game}})]
    (log/info "user:" (:id user) "connect to game:" (:id game))
    (async/send! channel response)))

(defn on-message [channel raw-message]
  (let [message (decode-message raw-message)
        type (:type message)
        payload (:payload message)]
    (log/info "message received:" message)
    (case type
      :login (handle-login channel payload)
      :logout (handle-logout channel payload)
      :create-game (handle-create-game channel)
      (log/warn "no matching message type:" message))))

#_(defn send-game-update [channel game-id]
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
