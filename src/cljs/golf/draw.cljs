(ns golf.draw
  (:require ["pixi.js" :as pixi]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [golf.game :as game]
            [golf.websocket :as ws]))

(def width 600)
(def height 600)

(def card-image-width 240)
(def card-image-height 336)
(def card-scale-x (/ width 2000))
(def card-scale-y (/ height 2000))

(def x-spacing (/ width 100))                               ; the space between cards in a hand
(def y-spacing (/ height 100))

(defn- remove-children [id]
  (let [elem (.getElementById js/document id)]
    (doseq [child elem.children]
      (.removeChild elem child))))

(def ^:private rank-texture-names
  (zipmap game/ranks ["A" "2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K"]))

(def ^:private suit-texture-names
  (zipmap game/suits ["C" "D" "H" "S"]))

(defn card->texture-name [card]
  (let [rank (rank-texture-names (:rank card))
        suit (suit-texture-names (:suit card))]
    (str rank suit)))

(defn texture-name->card [texture-name]
  (let [[rank suit] (seq texture-name)
        ranks (clojure.set/map-invert rank-texture-names)
        suits (clojure.set/map-invert suit-texture-names)]
    {:rank (ranks rank)
     :suit (suits suit)}))

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

(defn make-renderer [{:keys [width height background-color]
                      :or   {width 256 height 256 background-color 0xDDDDDD}}]
  (pixi/Renderer. #js{"width"           width
                      "height"          height
                      "backgroundColor" background-color}))

(defn texture-name [filename]
  (last (re-matches #"(.+)*/(.+)\.svg$" filename)))

(defn texture-loaded? [loader name]
  (aget loader "resources" name))

(defn load-card-texture [loader filename]
  (let [name (texture-name filename)]
    (if-not (texture-loaded? loader name)
      (.add loader name filename))))

(defn load-card-textures [loader]
  (doseq [file card-files]
    (load-card-texture loader file))
  (.load loader))

(defn attach-view [id renderer]
  (-> (.getElementById js/document id)
      (.appendChild renderer.view)))

(defn get-texture [loader name]
  (aget loader "resources" name "texture"))

(defn set-pos! [sprite {:keys [x y angle]
                        :or   {x 0 y 0 angle 0}}]
  (doto sprite
    (aset "x" x)
    (aset "y" y)
    (aset "angle" angle)))

(defn on-card-click [texture-name]
  (let [card (texture-name->card texture-name)]
    (re-frame/dispatch [:card/click card])))

(defn make-card-sprite [loader name {:keys [x y angle]
                                     :or   {x 0 y 0 angle 0}
                                     :as   pos}]
  (doto (pixi/Sprite. (get-texture loader name))
    (-> .-scale (.set card-scale-x card-scale-y))
    (set-pos! pos)
    (aset "interactive" true)
    (.on "click" #(on-card-click name))))

(defn card-coord [i]
  (let [x (-> (* card-image-width card-scale-x)
              (+ x-spacing)
              (* (mod i 3)))
        y (if (< i 3)
            0
            (-> (* card-image-height card-scale-y) (+ y-spacing)))]
    {:x x :y y}))

(defn make-hand-container [loader cards {:keys [x y x-spacing y-spacing angle]
                                         :or   {x 0 y 0 x-spacing 5 y-spacing 5 angle 0}
                                         :as   pos}]
  (let [container (pixi/Container.)]
    (doseq [[i card] (map-indexed vector cards)]
      (let [card-sprite (make-card-sprite loader (card->texture-name card) (card-coord i))]
        (.addChild container card-sprite)))
    (doto container
      (aset "pivot" "x" (/ container.width 2))
      (aset "pivot" "y" (/ container.height 2))
      (aset "angle" angle)
      (set-pos! pos))))


(defn deck-coord [started?]
  (let [[x y] [(/ width 2)
               (/ height 2)]
        x (if-not started? x (- x (* 0.5 card-image-width card-scale-x)))]
    {:x x :y y}))

(defn draw-deck [game loader stage]
  (let [sprite (make-card-sprite loader "2B" (deck-coord (:started? game)))]
    (.set sprite.anchor 0.5 0.5)
    (.addChild stage sprite)))

(def table-card-coord
  {:x (+ (/ width 2)
         (* (/ 1 2) card-scale-x card-image-width)
         2)
   :y (/ height 2)})

(defn draw-table-card [game loader stage]
  (if-let [table-card (:table-card game)]
    (let [sprite (make-card-sprite loader (card->texture-name table-card) table-card-coord)]
      (.set sprite.anchor 0.5 0.5)
      (.addChild stage sprite))))

(defn hand-coord [pos]
  (case pos
    :bottom {:x     (/ width 2)
             :y     (- height (* 1.1 card-image-height card-scale-y))
             :angle 0}
    :left {:x     (* 1.5 card-image-width card-scale-x)
           :y     (/ height 2)
           :angle 90}
    :top {:x     (/ width 2)
          :y     (* 1.1 card-image-height card-scale-y)
          :angle 180}
    :right {:x     (- width (* 1.5 card-image-width card-scale-x))
            :y     (/ height 2)
            :angle 270}))

(defn hand-positions [num-players]
  (case num-players
    1 [:bottom]
    2 [:bottom :top]
    3 [:bottom :left :right]
    4 [:bottom :left :top :right]))

(defn draw-player-hand [loader stage cards pos]
  (let [container (make-hand-container loader cards {})
        coord (hand-coord pos)]
    (set-pos! container coord)
    (.addChild stage container)))

(defn draw-player-hands [game loader stage turn]
  (let [hands (game/hands-starting-at-turn game turn)
        positions (hand-positions (count (:players game)))]
    (doseq [[hand pos] (zipmap hands positions)]
      (draw-player-hand loader stage hand pos))))

(defn draw-player-info [game stage]
  (let [positions (hand-positions (-> game :players count))]
    (doseq [[player pos] (zipmap (game/players-by-turn game) positions)]
      (let [score (game/score (:hand player))
            text (str (:name player) ", " score)
            text-elem (doto (pixi/Text. text #js{"fontSize" 18
                                                 "fill"     "limegreen"})
                        (-> .-anchor (.set 0.5 0.5))
                        (set-pos! (hand-coord pos)))]
        (.addChild stage text-elem)))))

(defn draw-game-info [])

(defn draw [id game loader renderer stage turn]
  (remove-children id)
  (draw-deck game loader stage)
  (draw-table-card game loader stage)
  (draw-player-hands game loader stage turn)
  (draw-player-info game stage)
  (.render renderer stage)
  (attach-view id renderer))

(defn init-graphics [id game loader renderer stage turn]
  (load-card-textures loader)
  (.add loader.onComplete #(do (println "textures loaded")
                               (draw id game loader renderer stage turn))))

(defn game-canvas []
  (let [id "game-canvas"
        game (re-frame/subscribe [:game])
        turn (re-frame/subscribe [:player/turn])
        loader (fn [] pixi/Loader.shared)
        renderer #(make-renderer {:width width :height height})
        stage #(pixi/Container.)]
    (reagent/create-class
      {:component-did-mount  (fn []
                               (init-graphics id @game (loader) (renderer) (stage) @turn)
                               (println "game-canvas mounted"))
       :component-did-update (fn []
                               (draw id @game (loader) (renderer) (stage) @turn)
                               (println "game-canvas updated"))
       :reagent-render       (fn []
                               @game                        ; this will force a redraw when game is changed
                               [:div.game-canvas
                                [:div#game-canvas]])})))
