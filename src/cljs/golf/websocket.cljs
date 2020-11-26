(ns golf.websocket
  (:require [cognitect.transit :as transit]
            [taoensso.sente :as sente]
            [mount.core :as mount]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(mount/defstate socket
  :start (sente/make-channel-socket! "/ws" js/csrfToken {:type :auto
                                                         :wrap-recv-evs? false}))

(defn send-message! [& args]
  (if-let [send-fn (:send-fn @socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, channel isn't open!"
                    {:message (first args)}))))

(defmulti handle-message (fn [{:keys [id]} _]
                           id))

(defmethod handle-message :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection established: " (pr-str event)))

(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State changed: " (pr-str event)))

(defmethod handle-message :chsk/ws-ping
  [_ _]
  (.log js/console "pinged"))

(defmethod handle-message :golf/hello
  [a b]
  (js/console.log "a: " (pr-str a))
  (js/console.log "b: " (pr-str b)))

(defmethod handle-message :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message: " (pr-str event)))

(defn receive-message! [{:keys [id event] :as message}]
  (do (.log js/console "Event received: " (pr-str event))
      (handle-message message event)))

(mount/defstate router
  :start (sente/start-chsk-router! (:ch-recv @socket) #'receive-message!)
  :stop (when-let [stop-fn @router]
          (stop-fn)))

;(defonce ws-chan (atom nil))
;
;(def json-reader (transit/reader :json))
;(def json-writer (transit/writer :json))
;
;(defn receive-transit-message! [update-fn]
;  (fn [msg]
;    (update-fn (->> msg .-data (transit/read json-reader)))))
;
;(defn send-transit-message! [message]
;  (if @ws-chan
;    (.send @ws-chan (transit/write json-writer message))
;    (throw (js/Error. "Websocket is not available."))))
;
;(defn make-websocket! [url receive-handler]
;  (println "attempting to connect websocket")
;  (if-let [chan (js/WebSocket. url)]
;    (do
;      (set! (.-onmessage chan) (receive-transit-message! receive-handler))
;      (reset! ws-chan chan)
;      (println "Websocket connection established with: " url))
;    (throw (js/Error. "Websocket connection failed."))))
;
;;; requests
;
;(defn send-login! [name]
;  (send-transit-message! {:type :login
;                          :name name}))
;
;(defn send-logout! [id]
;  (send-transit-message! {:type :logout
;                          :id id}))
;
;(defn send-create-game! []
;  (send-transit-message! {:type :create-game}))
;
;(defn send-get-games! []
;  (send-transit-message! {:type :get-games}))
;
;(defn send-start-game! [id]
;  (send-transit-message! {:type :start-game
;                          :id id}))
;
;(defn send-connect-to-game! [user-id game-id]
;  (send-transit-message! {:type :connect-to-game
;                          :user-id user-id
;                          :game-id game-id}))
;
;;; response handlers
;
;(defn handle-login [message]
;  (let [user (select-keys message [:id :name])]
;    (println (str "logging in " user))
;    (rf/dispatch [:user/login user])))
;
;(defn handle-logout [{:keys [id]}]
;  (println (str "logging out: " id))
;  (rf/dispatch [:user/logout id]))
;
;(defn handle-game-created [{:keys [game]}]
;  (println (str "game created: " game))
;  (rf/dispatch [:set-game game]))
;
;(defn handle-get-games [{:keys [games]}]
;  (rf/dispatch [:set-games games]))
;
;(defn handle-game-started [{:keys [game]}]
;  (println (str "game started: " game))
;  (rf/dispatch [:set-game game]))
;
;(defn handle-connect-to-game [{:keys [game]}]
;  (println (str "connected to game: " (:id game)))
;  (rf/dispatch [:set-game game]))
;
;(defn handle-response [response]
;  (println (str "received: " response))
;  (let [type (:type response)]
;    (case type
;      :login (handle-login response)
;      :logout (handle-logout response)
;      :game-created (handle-game-created response)
;      :get-games (handle-get-games response)
;      :game-started (handle-game-started response)
;      :connected-to-game (handle-connect-to-game response)
;      (println "no matching response type: " type))))
