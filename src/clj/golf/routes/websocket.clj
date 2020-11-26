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

(defn client-id [ring-req]
  (get-in ring-req [:params :client-id]))

(mount/defstate socket
  :start (sente/make-channel-socket! (get-sch-adapter)
                                     {:user-id-fn client-id}))

#_(defn connected-uids []
  (:connected-uids socket))

(def connected-uids (:connected-uids socket))

(defn send-message! [uid message]
  (log/info "sending message:" message)
  ((:send-fn socket) uid message))

(defmulti handle-message (fn [{:keys [id]}]
                           id))

(defmethod handle-message :default
  [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type: " id)
  {:error (str "Unrecognized websocket event type: " (pr-str id))
   :id    id})

(defmethod handle-message :golf/hello
  [message]
  (log/info "hello" message))

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

;; message utils

;(defn decode-message [message]
;  (let [bytes (.getBytes message)
;        in (ByteArrayInputStream. bytes)
;        reader (transit/reader in :json)]
;    (transit/read reader)))
;
;(defn encode-message [message]
;  (let [out (ByteArrayOutputStream.)
;        writer (transit/writer out :json)]
;    (transit/write writer message)
;    (.toString out)))
;
;(defn send-message! [channel message]
;  (async/send! channel (encode-message message)))
;
;;; websocket handlers
;
;(defn handle-login [context channel message]
;  (let [name (:name message)
;        user (manager/get-user-by-channel context channel)
;        id (:id user)
;        response {:type :login
;                  :id id
;                  :name name}]
;    (if-not (nil? user)
;      (log/info "logging in:" name)
;      (log/error "user not found. channel:" channel))
;    (manager/login! context id name)
;    (send-message! channel response)))
;
;(defn handle-logout [context channel message]
;  (let [id (:id message)
;        response {:type :logout
;                  :id id}]
;    (log/info "logging out:" id)
;    (manager/login! context id "anon")
;    (send-message! channel response)))
;
;(defn handle-create-game [context channel]
;  (let [user (manager/get-user-by-channel context channel)
;        game (manager/make-game)
;        response {:type :game-created
;                  :game game}]
;    (log/info "user:" (:id user) "connected to game:" (:id game))
;    (manager/add-game! context game)
;    (manager/connect-user-to-game! context (:id user) (:id game))
;    (send-message! channel response)))
;
;(defn handle-get-games [context channel]
;  (let [games (manager/get-games context)
;        response {:type :get-games
;                  :games games}]
;    (log/info "sending games to:" channel)
;    (send-message! channel response)))
;
;(defn handle-start-game [context channel {:keys [id]}]
;  (manager/start-game! context id)
;  (let [game (manager/get-game-by-id context id)
;        response {:type :game-started
;                  :game game}]
;    (log/info "game started:" game)
;    (send-message! channel response)))
;
;(defn handle-connect-to-game [context channel {:keys [user-id game-id]}]
;  (log/info "connecting user:" user-id "to game:" game-id)
;  (manager/connect-user-to-game! context user-id game-id)
;  (let [game (manager/get-game-by-id context game-id)
;        response {:type :connected-to-game
;                  :game game}]
;    (send-message! channel response)))
;
;(defn on-open [context channel]
;  (let [user (manager/make-user channel)]
;    (log/info "user created:" user)
;    (manager/add-user! context user)))
;
;(defn on-close [context channel {:keys [code reason]}]
;  (let [{:keys [id]} (manager/get-user-by-channel context channel)]
;    (log/info "channel closed. code:" code "reason:" reason)
;    (manager/remove-user! context id)))
;
;(defn on-message [context channel raw-message]
;  (let [message (decode-message raw-message)
;        type (:type message)]
;    (log/info "message received:" message)
;    (case type
;      :login (handle-login context channel message)
;      :logout (handle-logout context channel message)
;      :create-game (handle-create-game context channel)
;      :get-games (handle-get-games context channel)
;      :start-game (handle-start-game context channel message)
;      :connect-to-game (handle-connect-to-game context channel message)
;      (log/warn "no matching message type:" message))))
;
;#_(defn send-game-update [channel game-id]
;    (async/send! channel (get @games game-id)))
;
;;(defn notify-clients! [msg]
;;  (doseq [chan @channels]
;;    (async/send! chan msg)))
;
;(def websocket-callbacks
;  {:on-open (partial on-open manager/context)
;   :on-close (partial on-close manager/context)
;   :on-message (partial on-message manager/context)})
;
;(defn websocket-handler [request]
;  (async/as-channel request websocket-callbacks))
;
;(defn websocket-routes []
;  [["/ws" websocket-handler]])
