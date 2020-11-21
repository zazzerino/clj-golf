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

(defn send-login! [{:keys [name]}]
  (send-transit-message! {:type :login
                          :name name}))

(defn send-logout! [id]
  (send-transit-message! {:type :logout
                          :id id}))

(defn handle-login [{:keys [id name] :as user}]
  (println (str "logging in " name))
  (re-frame/dispatch [:user/login user]))

(defn handle-logout [id]
  (println (str "user logged out: " id)))

(defn handle-response [response]
  (println (str "received: " response))
  (case (:type response)
    :login-ok (handle-login (select-keys response [:id :name]))
    :logout-ok (handle-logout (:id response))
    (println "no matching response type")))
