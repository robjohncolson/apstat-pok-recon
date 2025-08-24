(ns pok.state
  "Profile Management and Re-frame State for PoK Blockchain
   Phase 5 implementation with archetype system and performance optimization"
  (:require [re-frame.core :as rf]
            [cljs.test :refer-macros [deftest is testing]]))

;; Profile record definition
(defrecord Profile [username archetype pubkey reputation-score])

;; Archetype system constants
(def ^:const ARCHETYPES
  {:aces {:emoji "🏆" :description "High accuracy, fast responses"}
   :strategists {:emoji "🧠" :description "Thoughtful, deliberate responses"}
   :explorers {:emoji "🔍" :description "Learning and discovering"}
   :learners {:emoji "📚" :description "Steady progress and improvement"}
   :socials {:emoji "🤝" :description "Collaborative and helpful"}})

;; Calculate archetype based on performance metrics
(defn calculate-archetype
  "Calculates user archetype based on performance metrics"
  [accuracy response-time questions-answered social-score]
  (cond
    ;; Aces: High accuracy (>90%) with fast responses (<3s)
    (and (>= accuracy 0.9) (< response-time 3000) (>= questions-answered 50))
    :aces
    
    ;; Strategists: Good accuracy (>85%) with thoughtful responses (5-8s)
    (and (>= accuracy 0.85) (>= response-time 5000) (<= response-time 8000) (>= questions-answered 30))
    :strategists
    
    ;; Socials: Good collaboration score (>80%) regardless of other metrics
    (>= social-score 0.8)
    :socials
    
    ;; Learners: Steady progress (60-80% accuracy) with moderate engagement
    (and (>= accuracy 0.6) (<= accuracy 0.8) (>= questions-answered 20))
    :learners
    
    ;; Explorers: New users or those still discovering the system
    :else
    :explorers))

;; Re-frame event handlers
(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:profile nil
    :current-question nil
    :blockchain {:blocks [] :mempool []}
    :reputation {:leaderboard [] :attestations {}}
    :ui {:modals {} :current-view :question}}))

(rf/reg-event-db
 :create-profile
 (fn [db [_ username]]
   (let [new-profile (map->Profile {:username username
                                   :archetype :explorers
                                   :pubkey (str "pk_" (.toString (js/Math.random) 36))
                                   :reputation-score 100.0})]
     (assoc db :profile new-profile))))

(rf/reg-event-db
 :update-archetype
 (fn [db [_ accuracy response-time questions-answered social-score]]
   (let [new-archetype (calculate-archetype accuracy response-time questions-answered social-score)]
     (assoc-in db [:profile :archetype] new-archetype))))

(rf/reg-event-db
 :update-reputation
 (fn [db [_ new-reputation]]
   (assoc-in db [:profile :reputation-score] new-reputation)))

;; Re-frame subscriptions
(rf/reg-sub
 :profile-visible
 (fn [db _]
   (when-let [profile (:profile db)]
     (dissoc profile :pubkey)))) ; Hide pubkey for UI

(rf/reg-sub
 :profile-archetype-data
 (fn [db _]
   (when-let [profile (:profile db)]
     (let [archetype (:archetype profile)
           archetype-data (get ARCHETYPES archetype)]
       (merge archetype-data {:archetype archetype})))))

(rf/reg-sub
 :reputation-score
 (fn [db _]
   (get-in db [:profile :reputation-score] 0.0)))

(rf/reg-sub
 :blockchain-height
 (fn [db _]
   (count (get-in db [:blockchain :blocks] []))))

(rf/reg-sub
 :transaction-mempool
 (fn [db _]
   (get-in db [:blockchain :mempool] [])))

;; Profile persistence helpers
(defn save-profile-to-storage!
  "Saves profile to browser localStorage"
  [profile]
  (let [profile-data (dissoc profile :pubkey)] ; Don't persist pubkey
    (.setItem js/localStorage "pok-profile" (.stringify js/JSON (clj->js profile-data)))))

(defn load-profile-from-storage
  "Loads profile from browser localStorage"
  []
  (when-let [stored (.getItem js/localStorage "pok-profile")]
    (try
      (js->clj (.parse js/JSON stored) :keywordize-keys true)
      (catch js/Error e nil))))

;; Archetype validation and description
(defn get-archetype-description
  "Gets full description for archetype"
  [archetype]
  (get ARCHETYPES archetype {:emoji "❓" :description "Unknown archetype"}))

(defn validate-profile
  "Validates profile structure"
  [profile]
  (and (map? profile)
       (string? (:username profile))
       (keyword? (:archetype profile))
       (contains? ARCHETYPES (:archetype profile))
       (string? (:pubkey profile))
       (number? (:reputation-score profile))))