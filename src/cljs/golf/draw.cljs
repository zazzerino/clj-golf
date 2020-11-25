(ns golf.draw
  (:require ["pixi.js" :as pixi]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [golf.game :as game]
            [golf.websocket :as ws]))

(def width 500)
(def height 500)

(def card-width 240)
(def card-height 336)
(def card-scale-x 0.2)
(def card-scale-y 0.2)

(defn- remove-children [elem]
  (doseq [child elem.children]
    (.removeChild elem child)))

(def ^:private rank-texture-names
  (zipmap game/ranks
          ["A" "2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K"]))

(def ^:private suit-texture-names
  (zipmap game/suits ["C" "D" "H" "S"]))

(defn- texture-name [card]
  (let [rank (rank-texture-names (:rank card))
        suit (suit-texture-names (:suit card))]
    (str rank suit)))

(def card-files
  ; downloaded from www.me.uk/cards, copyright Adrian Kennard
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

(defn make-renderer  [{:keys [width height background-color]
                       :or {width 256 height 256 background-color 0xDDDDDD}}]
  (pixi/Renderer. #js{"width" width
                      "height" height
                      "backgroundColor" background-color}))

(defn card-name [filename]
  (-> (string/split filename "/")
      last
      (string/split ".")
      first))

(defn texture-loaded? [loader name]
  (aget loader.resources name))

(defn load-card-texture [loader filename]
  (let [name (card-name filename)]
    (if-not (texture-loaded? loader name)
      (.add loader name filename))))

(defn load-card-textures [loader]
  (doseq [file card-files]
    (load-card-texture loader file))
  (.load loader))

(defn attach-view [id renderer]
  (-> (js/document.getElementById id)
      (.appendChild renderer.view)))

(defn get-texture [loader name]
  (-> (aget loader.resources name) .-texture))

(defn set-pos [sprite {:keys [x y angle]
                       :or {x 0 y 0 angle 0}}]
  (set! sprite.x x)
  (set! sprite.y y)
  (set! sprite.angle angle)
  sprite)

(defn make-card-sprite [loader name {:keys [x y angle]
                                     :or {x 0 y 0 angle 0}}]
  (doto (pixi/Sprite. (get-texture loader name))
    (set-pos {:x x :y y :angle angle})
    (-> .-scale (.set card-scale-x card-scale-y))))

(defn make-hand-container [loader cards {:keys [x y x-spacing y-spacing angle]
                                         :or {x 0 y 0 x-spacing 5 y-spacing 5 angle 0}}]
  (let [container (pixi/Container.)]
    (doseq [[i card] (map-indexed vector cards)]
      (let [x (-> (* card-width card-scale-x) (+ x-spacing) (* (mod i 3)))
            y (if (< i 3)
                0
                (-> (* card-height card-scale-y) (+ y-spacing)))
            card-sprite (make-card-sprite loader (texture-name card) {:x x :y y})]
        (.addChild container card-sprite)))
    (set-pos container {:x x :y y})
    (set! container.pivot.x (/ container.width 2))
    (set! container.pivot.y (/ container.height 2))
    (set! container.angle angle)
    container))

(defn player-hand-coord [pos]
  (case pos
    :bottom {:x (/ width 2)
             :y (- height (* 1.1 card-height card-scale-y))
             :angle 0}
    :left {:x (* 1.5 card-width card-scale-x)
           :y (/ height 2)
           :angle 90}
    :top {:x (/ width 2)
          :y (* 1.1 card-height card-scale-y)
          :angle 180}
    :right {:x (- width (* 1.5 card-width card-scale-x))
            :y (/ height 2)
            :angle 270}))

(defn draw-player-hand [loader stage cards pos]
  (let [container (make-hand-container loader cards {})
        coord (player-hand-coord pos)]
    (set-pos container coord)
    (.addChild stage container)))

(defn draw-deck [loader stage]
  (let [sprite (make-card-sprite loader "2B" {:x (/ width 2)
                                              :y (/ height 2)})]
    (.set sprite.anchor 0.5 0.5)
    (.addChild stage sprite)))

(defn draw-table-card [loader stage game]
  (if-let [table-card (:table-card game)]
    (let [sprite (make-card-sprite loader (texture-name table-card)
                                   {:x (+ (/ width 2) (* card-scale-x card-width))
                                    :y (/ height 2)})]
      (.set sprite.anchor 0.5 0.5)
      (.addChild stage sprite))))

(def cs [{:rank :ace, :suit :clubs}
         {:rank :ace, :suit :diamonds}
         {:rank :ace, :suit :hearts}
         {:rank :ace, :suit :spades}
         {:rank :2, :suit :clubs}
         {:rank :2, :suit :diamonds}])

(defn draw [id game loader renderer stage]
  (remove-children (js/document.getElementById id))
  (draw-deck loader stage)
  (draw-table-card loader stage game)
  ;(draw-player-hand loader stage cs :bottom)
  ;(draw-player-hand loader stage cs :left)
  ;(draw-player-hand loader stage cs :top)
  ;(draw-player-hand loader stage cs :right)
  (.render renderer stage)
  (attach-view id renderer))

(defn init-graphics [id game loader renderer stage]
  (load-card-textures loader)
  (-> loader.onComplete (.add #(do (println "textures loaded")
                                   (draw id game loader renderer stage)))))

(defn game-canvas []
  (let [id "game-canvas"
        game @(rf/subscribe [:game])
        loader (fn [] pixi/Loader.shared)
        renderer #(make-renderer {:width width :height height})
        stage #(pixi/Container.)]
    (reagent/create-class
      {:component-did-mount (fn []
                              (init-graphics id game (loader) (renderer) (stage))
                              (println "game-canvas mounted"))
       :component-did-update (fn []
                               (draw id game (loader) (renderer) (stage))
                               (println "game-canvas updated"))
       :reagent-render (fn []
                         game
                         [:div#game-canvas])})))
