(ns golf.websocket
  (:require [cognitect.transit :as transit]))

(defonce ws-chan (atom nil))
(def json-reader (transit/reader :json))
(def json-writer (transit/writer :json))

(defn receive-transit-msg! [update-fn]
  (fn [msg]
    (update-fn (->> msg .-data (transit/read json-reader)))))

(defn send-transit-msg! [msg]
  (if @ws-chan
    (.send @ws-chan (transit/write json-writer msg))
    (throw (js/Error. "Websocket is not available."))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (reset! ws-chan chan)
      (println "Websocket connection established with: " url))
    (throw (js/Error. "Websocket connection failed."))))

(defn send-login! [{:keys [name]}]
  (send-transit-msg! {:type :login
                      :name name}))

(defn handle-response! [response]
  (println (str response)))
