(ns golf.draw
  (:require ["pixi.js" :as pixi]
            [clojure.string :as string]
            [reagent.core :as reagent]))

(def game-state (reagent/atom {}))

(def card-width 240)
(def card-height 336)
(def card-scale-x 0.3)
(def card-scale-y 0.3)

(def card-files
  ["img/cards/svg/1B.svg", "img/cards/svg/2J.svg", "img/cards/svg/4C.svg",
   "img/cards/svg/5H.svg", "img/cards/svg/7C.svg", "img/cards/svg/8H.svg",
   "img/cards/svg/AC.svg", "img/cards/svg/JH.svg", "img/cards/svg/QC.svg",
   "img/cards/svg/TH.svg", "img/cards/svg/1J.svg", "img/cards/svg/2S.svg",
   "img/cards/svg/4D.svg", "img/cards/svg/5S.svg", "img/cards/svg/7D.svg",
   "img/cards/svg/8S.svg", "img/cards/svg/AD.svg", "img/cards/svg/JS.svg",
   "img/cards/svg/QD.svg", "img/cards/svg/TS.svg", "img/cards/svg/2B.svg",
   "img/cards/svg/3C.svg", "img/cards/svg/4H.svg", "img/cards/svg/6C.svg",
   "img/cards/svg/7H.svg", "img/cards/svg/9C.svg", "img/cards/svg/AH.svg",
   "img/cards/svg/KC.svg", "img/cards/svg/QH.svg", "img/cards/svg/2C.svg",
   "img/cards/svg/3D.svg", "img/cards/svg/4S.svg", "img/cards/svg/6D.svg",
   "img/cards/svg/7S.svg", "img/cards/svg/9D.svg", "img/cards/svg/AS.svg",
   "img/cards/svg/KD.svg", "img/cards/svg/QS.svg", "img/cards/svg/2D.svg",
   "img/cards/svg/3H.svg", "img/cards/svg/5C.svg", "img/cards/svg/6H.svg",
   "img/cards/svg/8C.svg", "img/cards/svg/9H.svg", "img/cards/svg/JC.svg",
   "img/cards/svg/KH.svg", "img/cards/svg/TC.svg", "img/cards/svg/2H.svg",
   "img/cards/svg/3S.svg", "img/cards/svg/5D.svg", "img/cards/svg/6S.svg",
   "img/cards/svg/8D.svg", "img/cards/svg/9S.svg", "img/cards/svg/JD.svg",
   "img/cards/svg/KS.svg", "img/cards/svg/TD.svg"])

(defn make-renderer
  ([{:keys [width height background-color]
     :or {width 256 height 256 background-color 0xDDDDDD}}]
   (pixi/Renderer. #js{"width" width
                       "height" height
                       "backgroundColor" background-color}))
  ([] (make-renderer {})))

(def renderer (make-renderer {:width 400 :height 400}))
(def stage (pixi/Container.))
(def loader pixi/Loader.shared)

(defn card-name [filename]
  (-> (string/split filename "/")
      last
      (string/split ".")
      first))

(defn texture-loaded? [name]
  (aget loader.resources name))

(defn load-card-texture [filename]
  (let [name (card-name filename)]
    (if-not (texture-loaded? name)
      (.add loader name filename))))

(defn load-card-textures [loader]
  (doseq [file card-files]
    (load-card-texture file))
  (.load loader))

(defn init-graphics [loader]
  (load-card-textures loader)
  (-> loader.onComplete (.add #(println "textures loaded"))))

(defn attach-view [id renderer]
  (-> (.getElementById js/document id)
      (.appendChild renderer.view)))

(defn get-texture [name]
  (-> (aget loader.resources name) .-texture))

(defn make-card-sprite [name]
  (doto (pixi/Sprite. (get-texture name))
    (-> .-scale (.set card-scale-x card-scale-y))))

(defn draw-table-card [stage card-name]
  (let [sprite (make-card-sprite card-name)]
    (.addChild stage sprite)))

(defn draw [id renderer stage]
  (.addChild stage (make-card-sprite "1B"))
  (.render renderer stage)
  (attach-view id renderer))

(init-graphics loader)

(defn game-canvas []
  (let [id "game-canvas"]
    (reagent/create-class
      {:component-did-mount (fn []
                              (draw id renderer stage)
                              (println "game-canvas mounted"))
       :component-did-update (fn []
                               (draw id renderer stage)
                               (println "game-canvas updated"))
       :reagent-render (fn []
                         @game-state
                         [:div#game-canvas])})))
