(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [golf.game :as game])
  (:import (java.util UUID)
           (java.io ByteArrayInputStream ByteArrayOutputStream)
           (org.projectodd.wunderboss.web.undertow.async.websocket UndertowWebsocketChannel)))

(s/def ::id (s/and string? (comp not empty?)))
(s/def ::name (s/and string? (comp not empty?)))
(s/def ::channel #(instance? UndertowWebsocketChannel %))
(s/def ::user (s/keys :req-un [::id ::name ::channel]))

(s/def ::users (s/map-of ::id ::user))
(s/def ::games (s/map-of ::id ::game/game))
(s/def ::state (s/keys :req-un [::users ::games]))

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

(defn add-user! [state user]
  (swap! state update-in [:users] conj {(:id user) user}))

(defn remove-user! [state id]
  (swap! state update-in [:users] dissoc id))

(defn set-user-name! [state id name]
  (swap! state assoc-in [:users id :name] name))

(defn get-user-by-id! [state id]
  (get (:users @state) id))

(defn get-user-by-channel! [state channel]
  (let [users (-> @state :users vals)]
    (first (filter #(= channel (:channel %)) users))))

;; games

(defn add-game [games game]
  (let [id (:id game)]
    (conj games {id game})))

(defn create-game! [state]
  (let [game (game/make-game)]
    (swap! state update-in [:games] add-game game)
    game))

(defn remove-game! [state id]
  (swap! state update-in [:games] dissoc id))

(defn get-game-by-id! [state id]
  (get-in @state [:games id]))

(defn connect-user-to-game! [state user-id game-id]
  (let [user (-> (get-user-by-id! state user-id)
                 (assoc :game-id game-id))
        player (game/make-player user-id (:name user))
        game (-> (get-game-by-id! state game-id)
                 (game/add-player player))]
    (swap! state #(-> (assoc-in % [:users user-id] user)
                      (assoc-in [:games game-id] game)))
    game))

;; websocket handlers

(defn on-open [state channel]
  (let [user (make-user channel)]
    (log/info "user created:" user)
    (add-user! state user)))

(defn on-close [state channel {:keys [code reason]}]
  (let [{:keys [id]} (get-user-by-channel! state channel)]
    (log/info "channel closed. code:" code "reason:" reason)
    (log/info "user deleted:" id)
    (remove-user! state id)))

(defn handle-login [state channel payload]
  (let [name (:name payload)
        user (get-user-by-channel! state channel)
        id (:id user)
        response (encode-message {:type :login-ok
                                  :payload {:id id
                                            :name name}})]
    (log/info "logging in:" name)
    (if (nil? user)
      (log/error "user not found. channel:" channel))
    (set-user-name! state id name)
    (async/send! channel response)))

(defn handle-logout [state channel payload]
  (let [id (:id payload)
        response (encode-message {:type :logout-ok
                                  :payload {:id id}})]
    (log/info "logging out:" id)
    (set-user-name! state id "anon")
    (async/send! channel response)))

(defn handle-create-game [state channel]
  (let [user (get-user-by-channel! state channel)
        game (create-game! state)
        game (connect-user-to-game! state (:id user) (:id game))
        response (encode-message {:type :game-created
                                  :payload {:game game}})]
    (log/info "user:" (:id user) "connect to game:" (:id game))
    (async/send! channel response)))

(defn handle-get-games [state channel]
  (let [games (-> @state :games vals)
        response (encode-message {:type :get-games
                                  :payload {:games games}})]
    (log/info "sending games")
    (async/send! channel response)))

;(defn handle-start-game [channel {:keys [id]}]
;  (let [game (get-in @state [:games id])]
;    ))

(defn on-message [state channel raw-message]
  (let [message (decode-message raw-message)
        type (:type message)
        payload (:payload message)]
    (log/info "message received:" message)
    (case type
      :login (handle-login state channel payload)
      :logout (handle-logout state channel payload)
      :create-game (handle-create-game state channel)
      :get-games (handle-get-games state channel)
      (log/warn "no matching message type:" message))))

#_(defn send-game-update [channel game-id]
  (async/send! channel (get @games game-id)))

;(defn notify-clients! [msg]
;  (doseq [chan @channels]
;    (async/send! chan msg)))

(def websocket-callbacks
  {:on-open (partial on-open state)
   :on-close (partial on-close state)
   :on-message (partial on-message state)})

(defn websocket-handler [request]
  (async/as-channel request websocket-callbacks))

(defn websocket-routes []
  [["/ws" websocket-handler]])
