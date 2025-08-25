(ns pok.state
  "Profile Management and Re-frame State for PoK Blockchain
   Phase 5 implementation with archetype system and performance optimization"
  (:require [re-frame.core :as rf]
            [pok.reputation :as reputation]))

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
 (fn [db [_ {:keys [accuracy attestations question-stats streak-count time-windows] 
             :or {accuracy 1.0 attestations [] question-stats {} streak-count 0 time-windows 0}}]]
   (let [current-profile (:profile db)]
     (if current-profile
       ;; Use the complete reputation calculation from reputation.cljs
       (let [updated-profile (reputation/update-reputation-score 
                             current-profile accuracy attestations question-stats streak-count)
             ;; Apply time decay if time-windows is provided
             final-profile (if (> time-windows 0)
                            (update updated-profile :reputation-score 
                                   #(reputation/reputation-decay % (* time-windows 24)))
                            updated-profile)]
         (assoc db :profile final-profile))
       db))))

;; Submit answer event handler - simple version to avoid circular deps
(rf/reg-event-fx
 :submit-answer
 (fn [{:keys [db]} [_ question-id answer]]
   (let [current-profile (:profile db)
         ;; For testing, assume correct answer varies by question
         correct-answer (case question-id
                         "U1-L1-Q01" "A"
                         "U1-L1-Q02" "B" 
                         "A") ; default
         is-correct (= answer correct-answer)]
     
     (println (str "Processing answer: Q=" question-id " A=" answer " Correct=" correct-answer " Result=" is-correct))
     
     ;; Create profile if it doesn't exist  
     (when-not current-profile
       (rf/dispatch [:create-profile "test-user"]))
     
     ;; Update reputation based on correctness
     (rf/dispatch [:update-reputation {:accuracy (if is-correct 1.0 0.0)
                                      :attestations []
                                      :question-stats {answer 0.25}
                                      :streak-count (if is-correct 1 0)}])
     
     {:db db})))

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

;; Debug subscription to access full db state from console
(rf/reg-sub
 :debug/app-db
 (fn [db _]
   db))

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
      (catch js/Error _ nil))))

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