(ns golf.draw
  (:require ["pixi.js" :as pixi]
            [reagent.core :as reagent]))

(def game-state (reagent/atom {}))

(defn make-renderer
  ([{:keys [width height background-color]
     :or {width 256 height 256 background-color 0xDDDDDD}}]
   (pixi/Renderer. #js{"width" width
                       "height" height
                       "backgroundColor" background-color}))
  ([] (make-renderer {})))

(def renderer (make-renderer {:width 400 :height 400}))
(def stage (pixi/Container.))

(defn draw [renderer stage]
  (.render renderer stage))

(defn attach-view [id renderer]
  (-> (.getElementById js/document id)
      (.appendChild renderer.view)))

(defn game-canvas []
  (let [id "game-canvas"]
    (reagent/create-class
      {:component-did-mount (fn []
                              (draw renderer stage)
                              (attach-view id renderer)
                              (println "game-canvas mounted"))
       :component-did-update (fn []
                               (draw renderer stage)
                               (attach-view id renderer)
                               (println "game-canvas updated"))
       :reagent-render (fn []
                         @game-state
                         [:div#game-canvas])})))
