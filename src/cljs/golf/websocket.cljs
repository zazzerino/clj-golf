(ns golf.websocket
  (:require [cognitect.transit :as transit]
            [re-frame.core :as re-frame]))

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

(defn send-login! [name]
  (send-transit-message! {:type :login
                          :payload {:name name}}))

(defn send-logout! [id]
  (send-transit-message! {:type :logout
                          :payload {:id id}}))

(defn send-create-game! []
  (send-transit-message! {:type :create-game}))

(defn handle-login [{:keys [id name] :as user}]
  (println (str "logging in " name))
  (re-frame/dispatch [:user/login user]))

(defn handle-logout [{:keys [id]}]
  (println (str "user logged out: " id)))

(defn handle-game-created [{:keys [game]}]
  (println (str "game created: " game))
  (re-frame/dispatch [:set-game game]))

(defn handle-response [response]
  (println (str "received: " response))
  (let [type (:type response)
        payload (:payload response)]
    (case type
      :login-ok (handle-login payload)
      :logout-ok (handle-logout payload)
      :game-created (handle-game-created payload)
      (println "no matching response type"))))
