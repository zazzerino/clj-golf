(ns golf.routes.websocket
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [mount.core :as mount]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [get-sch-adapter]]
            [golf.middleware :as middleware]
            [golf.manager :as manager]))

(mount/defstate context
  :start (manager/make-context)
  :stop (reset! context (manager/make-context)))

(defn client-id [ring-req]
  (get-in ring-req [:params :client-id]))

(mount/defstate socket
  :start (sente/make-channel-socket! (get-sch-adapter)
                                     {:user-id-fn client-id}))

(def connected-uids (:connected-uids socket))

(defn send-message! [uid message]
  (log/info "sending message:" message)
  ((:send-fn socket) uid message))

(defn send-to-all! [message]
  (doseq [uid (:any @connected-uids)]
    (send-message! uid message)))

(defmulti handle-message (fn [{:keys [id]}]
                           id))

(defmethod handle-message :default
  [{:keys [id]}]
  (when-not (= id :chsk/ws-ping)
    (log/debug "Received unrecognized websocket event type: " id))
  {:error (str "Unrecognized websocket event type: " (pr-str id))
   :id    id})

;(defmethod handle-message :chsk/ws-ping
;  [message]
;  (log/info "pinged:" (pr-str message)))

(defmethod handle-message :chsk/uidport-open
  [{:keys [uid]}]
  (if (manager/get-user-by-id context uid)
    (manager/remove-user context uid))
  (manager/add-user context uid)
  (log/info "uidport open:" uid))

(defmethod handle-message :golf/login
  [{:keys [uid ?reply-fn ?data]}]
  (let [name (:name ?data)]
    (if-let [user (manager/get-user-by-id context uid)]
      (do (log/info "user found:" (pr-str user))
          (manager/login context uid name)
          (if ?reply-fn
            (?reply-fn {:user (assoc user :name name)})
            (log/error "no reply-fn in :golf/login msg")))
      (log/error "user not found:" (pr-str uid)))))

(defmethod handle-message :golf/logout
  [{:keys [uid ?reply-fn]}]
  (if-let [user (manager/get-user-by-id context uid)]
    (do (log/info "logging out user:" uid)
        (manager/logout context uid)
        (if ?reply-fn
          (?reply-fn :logged-out)))
    (log/error "user not found: " (pr-str uid))))

(defmethod handle-message :golf/new-game
  [{:keys [uid ?reply-fn]}]
  (let [game (manager/new-game context uid)]
    (log/info "game created:" (pr-str game))
    (when ?reply-fn
      (?reply-fn {:game game}))))

(defmethod handle-message :golf/get-all-games
  [{:keys [?reply-fn]}]
  (let [games (manager/get-all-games context)]
    (if ?reply-fn
      (?reply-fn {:games games})
      (log/error "no reply-fn provided to :golf/get-all-games"))))

(defmethod handle-message :golf/join-game
  [{:keys [uid ?data ?reply-fn]}]
  (if-let [game (manager/join-game context (:game-id ?data) uid)]
    (if ?reply-fn
      (?reply-fn {:game game}))))

(defn receive-message! [{:keys [id] :as message}]
  (log/debug "Received message with id: " id)
  (handle-message message))

(mount/defstate router
  :start (sente/start-chsk-router! (:ch-recv socket) #'receive-message!)
  :stop (when-let [stop-fn router]
          (stop-fn)))

(defn websocket-routes []
  ["/ws"
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]
    :get  (:ajax-get-or-ws-handshake-fn socket)
    :post (:ajax-post-fn socket)}])
