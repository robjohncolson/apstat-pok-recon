(ns pok.core
  (:require [reagent.dom.client :as rdom]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.db :as rfdb]
            [pok.state :as state]  ; For Re-frame events/subs
            [pok.flow :as flow]    ; For flow logic
            [pok.reputation :as rep]  ; For reputation system
            [pok.views :as views]))  ; UI components

;; Dev setup
(defn dev-setup []
  (when goog.DEBUG
    (enable-console-print!)
    (println "dev mode")
    ;; Expose app-db for console debugging (dev-only)
    (set! (.-appDb js/window) rfdb/app-db)
    (set! (.-getAppDb js/window) #(deref rfdb/app-db))
    (println "💡 Debug helpers: window.appDb (atom) | window.getAppDb() (current state)")))

;; Mount root component with main UI panel
(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (js/document.getElementById "app")
        root (rdom/create-root root-el)]
    (.render root (r/as-element [views/main-panel]))))  ; React 18 compatible

;; Load curriculum from JSON file
(defn load-curriculum []
  (-> (js/fetch "/curriculum.json")
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. "Failed to load curriculum.json")))))
      (.then (fn [json-data]
               (let [curriculum (js->clj json-data :keywordize-keys true)]
                 (js/console.log "Loaded curriculum:" (count curriculum) "questions")
                 (js/console.log "First 3 questions:" (clj->js (take 3 curriculum)))
                 (rf/dispatch [:load-curriculum curriculum])
                 (rf/dispatch [:initialize-with-curriculum]))))
      (.catch (fn [error]
                (js/console.log "Error loading curriculum:" error)
                (rf/dispatch [:load-curriculum []])
                (rf/dispatch [:initialize-with-curriculum])))))

;; App init
(defn init []
  (dev-setup)
  (rf/dispatch-sync [:initialize-db])  ; Initialize basic DB structure
  (load-curriculum)  ; Load curriculum and check for existing profile
  (state/expose-debug-helpers!)  ; Dev-only console helpers
  (mount-root)
  (println "🚀 AP Statistics PoK Blockchain initialized with persistence"))

;; Shadow-cljs entry point
(defn ^:export init! []
  (init))