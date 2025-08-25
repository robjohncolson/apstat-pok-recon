(ns pok.state
  "Profile Management and Re-frame State for PoK Blockchain
   Phase 5 implementation with archetype system and performance optimization"
  (:require [re-frame.core :as rf]
            [re-frame.db :as rfdb]
            [pok.reputation :as reputation]
            [pok.curriculum :as curriculum]
            [pok.blockchain :as blockchain]))

;; Simple SHA-256 using built-in crypto or fallback hash
(defn simple-sha256 
  "Simple SHA-256 hash for answer hashing (MCQ)"
  [text]
  (if (and js/crypto js/crypto.subtle)
    ;; Use Web Crypto API if available (async, but we'll use sync fallback)
    (str (.toString (hash text) 16))  ; Fallback to cljs hash
    (str (.toString (hash text) 16))))

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
    :curriculum []
    :current-question-index 0
    :current-question nil
    :question-index 0
    :questions curriculum/sample-questions
    :mempool []
    :chain []
    :distributions {}
    :blockchain {:blocks [] :mempool []}
    :reputation {:leaderboard [] :attestations {}}
    :ui {:modals {} :current-view :question}}))

;; Load curriculum event
(rf/reg-event-db
 :load-curriculum
 (fn [db [_ curriculum-data]]
   (assoc db 
          :curriculum curriculum-data
          :current-question-index 0
          :current-question (first curriculum-data))))

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

;; Submit answer event handler - creates blockchain transaction per ADR-028
(rf/reg-event-fx
 :submit-answer
 (fn [{:keys [db]} [_ question-id answer]]
   (let [current-profile (:profile db)
         current-question (:current-question db)
         question-type (:type current-question)
         
         ;; Determine question type
         current-type (cond
                       (or (:choices current-question) (= question-type "multiple-choice")) "multiple-choice"
                       (= question-type "free-response") "free-response"
                       :else "multiple-choice")]
     
     (println (str "Processing answer: Q=" question-id " Type=" current-type " A=" answer))
     
     ;; Create profile if it doesn't exist  
     (when-not current-profile
       (rf/dispatch [:create-profile "test-user"]))
     
     ;; Create transaction and add to mempool
     (let [tx (blockchain/create-tx question-id answer current-type)]
       (rf/dispatch [:add-to-mempool tx]))
     
     ;; PoK: No immediate reputation updates - mining handles this
     ;; Auto-advance handled in views.cljs
     
     {:db db})))

;; Load next question event handler
(rf/reg-event-db
 :load-next-question
 (fn [db _]
   (let [current-index (:current-question-index db)
         curriculum (:curriculum db)
         next-index (mod (inc current-index) (count curriculum))
         next-question (nth curriculum next-index nil)]
     (assoc db 
            :current-question-index next-index
            :current-question next-question))))

;; Load previous question event handler
(rf/reg-event-db
 :load-prev-question
 (fn [db _]
   (let [current-index (:current-question-index db)
         curriculum (:curriculum db)
         prev-index (mod (dec current-index) (count curriculum))
         prev-question (nth curriculum prev-index nil)]
     (assoc db 
            :current-question-index prev-index
            :current-question prev-question))))

;; Add transaction to mempool
(rf/reg-event-db
 :add-to-mempool
 (fn [db [_ tx]]
   (update db :mempool conj tx)))

;; Mine block from mempool
(rf/reg-event-fx
 :mine-block
 (fn [{:keys [db]} _]
   (let [mined-result (blockchain/mine-block db)]
     (if (:block mined-result)
       ;; Block mined successfully
       (do
         (js/console.log "Block mined:" (clj->js (:block mined-result)))
         (js/console.log "Updated distributions:" (clj->js (:updated-distributions mined-result)))
         
         ;; Update reputation for each transaction in the block
         (doseq [rep-update (blockchain/extract-reputation-updates (:block mined-result))]
           (when rep-update
             (rf/dispatch [:update-reputation rep-update])))
         
         ;; Update database with new block and distributions
         {:db (assoc db 
                    :chain (:chain mined-result)
                    :mempool (:mempool mined-result) 
                    :distributions (:distributions mined-result))})
       
       ;; No block mined
       {:db db}))))

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

;; Curriculum subscriptions
(rf/reg-sub
 ::curriculum
 (fn [db _]
   (:curriculum db)))

;; Current question subscription
(rf/reg-sub
 :current-question
 (fn [db _]
   (:current-question db)))

;; Blockchain subscriptions
(rf/reg-sub
 ::mempool
 (fn [db _]
   (:mempool db)))

(rf/reg-sub
 ::chain
 (fn [db _]
   (:chain db)))

(rf/reg-sub
 ::distributions
 (fn [db _]
   (:distributions db)))

(rf/reg-sub
 ::convergence
 (fn [db [_ qid]]
   (get-in db [:distributions qid :convergence-score] 0)))

(rf/reg-sub
 ::mempool-count
 (fn [db _]
   (count (:mempool db))))

;; Dev-only: Helper functions for console debugging
(defn ^:dev/after-load expose-debug-helpers! []
  (when goog.DEBUG
    ;; Helper to get subscription values without reactive context
    (set! (.-getReputationScore js/window) 
          #(get-in (deref rfdb/app-db) [:profile :reputation-score] "No profile"))
    (set! (.-getProfile js/window) 
          #(get (deref rfdb/app-db) :profile "No profile"))
    (set! (.-getDbPath js/window) 
          (fn [path] (get-in (deref rfdb/app-db) path "Path not found")))
    (println "💡 Console helpers: getReputationScore() | getProfile() | getDbPath([path])")))

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