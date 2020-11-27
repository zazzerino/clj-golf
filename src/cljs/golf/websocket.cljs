(ns golf.websocket
  (:require [cognitect.transit :as transit]
            [taoensso.sente :as sente]
            [mount.core :as mount]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(def config {:type :auto
             :wrap-recv-evs? false})

(mount/defstate socket
  :start (sente/make-channel-socket! "/ws" js/csrfToken config))

(defn get-uid []
  (-> @socket :state deref :uid))

(defn send-message! [& args]
  (if-let [send-fn (:send-fn @socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, channel isn't open!"
                    {:message (first args)}))))

(defmulti handle-message :id)

(defmethod handle-message :chsk/handshake
  [{:keys [event ?data]} _]
  (if-let [uid (first ?data)]
    (rf/dispatch [:user/login {:id uid :name "anon"}]))
  (.log js/console "Connection established: " (pr-str event)))

(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State changed: " (pr-str event)))

(defmethod handle-message :chsk/ws-ping
  [{:keys [event]} _]
  #_(.log js/console "pinged"))

(defmethod handle-message :golf/hello
  [_ _]
  (println "hello golf"))

(defmethod handle-message :golf/game-update
  [{:keys [?data]} _]
  (println "updating game")
  (if-let [game (:game ?data)]
    (rf/dispatch [:set-game game])
    (println "There was a problem updating game.")))

(defmethod handle-message :golf/games-updated
  [{:keys [?data]} _]
  (println "games updated")
  (if-let [games (:games ?data)]
    (rf/dispatch [:set-games games])))

(defmethod handle-message :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message: " (pr-str event)))

(defn receive-message! [{:keys [event] :as message}]
  (do (.log js/console "Event received: " (pr-str event))
      (handle-message message event)))

(mount/defstate router
  :start (sente/start-chsk-router! (:ch-recv @socket) #'receive-message!)
  :stop (when-let [stop-fn @router]
          (stop-fn)))

(defn send-login! [name]
  (send-message! [:golf/login {:name name}]
    4000
    (fn [reply]
      (if (sente/cb-success? reply)
        (rf/dispatch [:user/login (:user reply)])
        (println "error: " (pr-str reply))))))

(defn send-logout! []
  (send-message! [:golf/logout]
    4000
    (fn [reply]
      (if (sente/cb-success? reply)
        (rf/dispatch [:user/logout])
        (println "error: " (pr-str reply))))))

(defn send-new-game! []
  (send-message! [:golf/new-game]
    4000
    (fn [reply]
      (if (sente/cb-success? reply)
        (rf/dispatch [:set-game (:game reply)])
        (println "error: " (pr-str reply))))))

(defn send-get-games! []
  (send-message! [:golf/get-all-games]
    4000
    (fn [reply]
      (if (sente/cb-success? reply)
        (rf/dispatch [:set-games (:games reply)])
        (println "error: " (pr-str reply))))))

(defn send-join-game! [game-id]
  (send-message! [:golf/join-game {:game-id game-id}]
    4000
    (fn [reply]
      (if (sente/cb-success? reply)
        (rf/dispatch [:set-game (:game reply)])
        (println "error: " (pr-str reply))))))

(defn send-start-game! []
  (let [game-id @(rf/subscribe [:game/id])]
    (send-message! [:golf/start-game {:game-id game-id}])))
