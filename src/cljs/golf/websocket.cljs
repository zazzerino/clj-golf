(ns golf.websocket
  (:require [cognitect.transit :as transit]
            [re-frame.core :as rf]))

(defonce ws-chan (atom nil))

(def json-reader (transit/reader :json))
(def json-writer (transit/writer :json))

(defn receive-transit-message! [update-fn]
  (fn [msg]
    (update-fn (->> msg .-data (transit/read json-reader)))))

(defn send-transit-message! [message]
  (if @ws-chan
    (.send @ws-chan (transit/write json-writer message))
    (throw (js/Error. "Websocket is not available."))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-message! receive-handler))
      (reset! ws-chan chan)
      (println "Websocket connection established with: " url))
    (throw (js/Error. "Websocket connection failed."))))

;; requests

(defn send-login! [name]
  (send-transit-message! {:type :login
                          :name name}))

(defn send-logout! [id]
  (send-transit-message! {:type :logout
                          :id id}))

(defn send-create-game! []
  (send-transit-message! {:type :create-game}))

(defn send-get-games! []
  (send-transit-message! {:type :get-games}))

(defn send-start-game! [id]
  (send-transit-message! {:type :start-game
                          :id id}))

(defn send-connect-to-game! [user-id game-id]
  (send-transit-message! {:type :connect-to-game
                          :user-id user-id
                          :game-id game-id}))

;; response handlers

(defn handle-login [message]
  (let [user (select-keys message [:id :name])]
    (println (str "logging in " user))
    (rf/dispatch [:user/login user])))

(defn handle-logout [{:keys [id]}]
  (println (str "logging out: " id))
  (rf/dispatch [:user/logout id]))

(defn handle-game-created [{:keys [game]}]
  (println (str "game created: " game))
  (rf/dispatch [:set-game game]))

(defn handle-get-games [{:keys [games]}]
  (rf/dispatch [:set-games games]))

(defn handle-game-started [{:keys [game]}]
  (println (str "game started: " game))
  (rf/dispatch [:set-game game]))

(defn handle-connect-to-game [{:keys [game]}]
  (println (str "connected to game: " (:id game)))
  (rf/dispatch [:set-game game]))

(defn handle-response [response]
  (println (str "received: " response))
  (let [type (:type response)]
    (case type
      :login (handle-login response)
      :logout (handle-logout response)
      :game-created (handle-game-created response)
      :get-games (handle-get-games response)
      :game-started (handle-game-started response)
      :connected-to-game (handle-connect-to-game response)
      (println "no matching response type: " type))))

(defn init []
  (send-login! "name0")
  (send-create-game!)
  (send-start-game! @(rf/subscribe [:game/id])))
