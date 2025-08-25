(ns pok.core
  (:require [reagent.dom.client :as rdom]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [pok.state :as state]  ; For Re-frame events/subs
            [pok.flow :as flow]    ; For flow logic
            [pok.reputation :as rep]))  ; For reputation system

;; Dev setup
(defn dev-setup []
  (when goog.DEBUG
    (enable-console-print!)
    (println "dev mode")))

;; Mount root component (stub views for now; implement in pok.views.cljs if needed)
(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (js/document.getElementById "app")
        root (rdom/create-root root-el)]
    (.render root (r/as-element [:div "AP Stats PoK App Mounted - Reputation System Ready"]))))  ; React 18 compatible

;; App init
(defn init []
  (dev-setup)
  (rf/dispatch-sync [:initialize-db])  ; Assumes :initialize-db event in state.cljs
  (mount-root)
  (println "🚀 AP Statistics PoK Blockchain initialized"))

;; Shadow-cljs entry point
(defn ^:export init! []
  (init))