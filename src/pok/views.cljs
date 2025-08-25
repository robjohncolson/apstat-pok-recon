(ns pok.views
  "Interactive UI Components for AP Statistics PoK Blockchain Testing
   Focuses on question display, answer submission, and reputation tracking"
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [pok.curriculum :as curriculum]
            [pok.state :as state]))

;; Chart.js wrapper component
(defn chart-component
  "Renders Chart.js charts from question attachments"
  [chart-data]
  (let [canvas-ref (r/atom nil)]
    (r/create-class
     {:display-name "chart-component"
      
      :component-did-mount
      (fn [this]
        (when-let [canvas @canvas-ref]
          (when js/Chart
            (let [ctx (.getContext canvas "2d")
                  config (curriculum/create-chart-config chart-data {:responsive true
                                                                    :maintainAspectRatio false
                                                                    :plugins {:legend {:display true}}})]
              (js/Chart. ctx (clj->js config))))))
      
      :reagent-render
      (fn [chart-data]
        [:div.chart-container
         {:style {:height "300px" :width "100%" :margin "20px 0"}}
         [:canvas 
          {:ref #(reset! canvas-ref %)
           :style {:max-height "300px"}}]])})))

;; Question display component
(defn question-panel
  "Displays a single question with choices and submit functionality"
  []
  (let [selected-answer (r/atom {:text "" :score 3})]  ; Support both MCQ (key) and FRQ (map)
    (fn []
      (let [current-question @(rf/subscribe [:current-question])]
        ;; Debug logging for question type
        (js/console.log "Question type:" (:type current-question) (:id current-question))
        [:div.question-panel
         {:style {:background-color "#f8f9fa"
                  :border-radius "12px"
                  :padding "30px"
                  :margin "20px 0"
                  :box-shadow "0 4px 12px rgba(0,0,0,0.1)"}}
         
         ;; Question header
         [:div.question-header
          {:style {:margin-bottom "20px"}}
          [:h3 {:style {:color "#2c3e50" :margin "0 0 10px 0"}}
           "Question: " (:id current-question)]
          [:span.question-type
           {:style {:background "#e3f2fd"
                    :color "#1976d2"
                    :padding "4px 12px"
                    :border-radius "20px"
                    :font-size "12px"
                    :font-weight "bold"}}
           (or (:type current-question) "multiple-choice")]]
         
         ;; Question prompt
         [:div.question-prompt
          {:style {:font-size "16px"
                   :line-height "1.6"
                   :color "#333"
                   :margin "20px 0"}}
          (:prompt current-question)]
         
         ;; Chart attachment if present
         (when-let [chart-data (:attachments current-question)]
           (when (:chart-type chart-data)
             [chart-component chart-data]))
         
         ;; Question type handling: MCQ vs FRQ
         (if (or (:choices current-question) (= (:type current-question) "multiple-choice"))
           ;; MCQ: Multiple choice options
           (when-let [choices (:choices current-question)]
             [:div.choices
              {:style {:margin "25px 0"}}
              (doall  ; Fix lazy seq warning
               (for [choice choices]
                 ^{:key (:key choice)}
                 [:label.choice-option
                  {:style {:display "flex"
                           :align-items "center"
                           :margin "12px 0"
                           :cursor "pointer"
                           :padding "12px"
                           :border-radius "8px"
                           :border (if (= @selected-answer (:key choice))
                                    "2px solid #1976d2"
                                    "2px solid #e0e0e0")
                           :background-color (if (= @selected-answer (:key choice))
                                              "#f3f8ff"
                                              "white")
                           :transition "all 0.2s ease"}}
                  [:input.choice-radio
                   {:type "radio"
                    :name "answer"
                    :value (:key choice)
                    :checked (= @selected-answer (:key choice))
                    :on-change #(reset! selected-answer (:key choice))
                    :style {:margin-right "10px"}}]
                  [:span.choice-text (:key choice) ". " (:value choice)]]))])
           
           ;; FRQ: Free response question
           (when (= (:type current-question) "free-response")
             [:div.free-response
              {:style {:margin "25px 0"}}
              [:label 
               {:style {:font-weight "bold" :display "block" :margin-bottom "10px"}}
               "Your Response:"]
              [:textarea
               {:value (:text @selected-answer)
                :on-change #(swap! selected-answer assoc :text (.. % -target -value))
                :rows 5
                :cols 50
                :style {:width "100%" 
                        :padding "12px"
                        :border-radius "8px"
                        :border "2px solid #e0e0e0"
                        :font-family "inherit"
                        :resize "vertical"}}]
              [:label
               {:style {:font-weight "bold" :display "block" :margin "15px 0 5px 0"}}
               "Self-Score (1-5 Rubric):"]
              [:input
               {:type "number"
                :min 1
                :max 5
                :value (:score @selected-answer)
                :on-change #(swap! selected-answer assoc :score (js/parseInt (.. % -target -value)))
                :style {:padding "8px"
                        :border-radius "4px"
                        :border "2px solid #e0e0e0"
                        :width "60px"}}]]))
         
         ;; Submit button
         [:div.submit-section
          {:style {:margin-top "30px" :text-align "center"}}
          (let [has-answer (if (= (:type current-question) "free-response")
                            (and (> (count (:text @selected-answer)) 0) 
                                 (number? (:score @selected-answer)))
                            (string? @selected-answer))]
            [:button.submit-btn
             {:style {:background-color (if has-answer "#1976d2" "#bdbdbd")
                      :color "white"
                      :border "none"
                      :padding "12px 30px"
                      :border-radius "6px"
                      :font-size "16px"
                      :font-weight "bold"
                      :cursor (if has-answer "pointer" "not-allowed")
                      :transition "background-color 0.3s ease"}
              :disabled (not has-answer)
              :on-click #(when has-answer
                          (rf/dispatch [:submit-answer (:id current-question) @selected-answer])
                          ;; Auto advance to next question after submit
                          (js/setTimeout (fn [] 
                                          (rf/dispatch [:load-next-question])
                                          (reset! selected-answer 
                                                   (if (= (:type current-question) "free-response")
                                                     {:text "" :score 3}
                                                     nil))) 
                                        1500))}
             "Submit Answer"])]]))))

;; Reputation display component
(defn reputation-panel
  "Displays current reputation score and profile info"
  []
  (let [reputation-score @(rf/subscribe [:reputation-score])
        profile @(rf/subscribe [:profile-visible])
        archetype-data @(rf/subscribe [:profile-archetype-data])
        mempool-count @(rf/subscribe [::state/mempool-count])
        chain @(rf/subscribe [::state/chain])]
    [:div.reputation-panel
     {:style {:background-color "white"
              :border-radius "12px"
              :padding "20px"
              :margin "20px 0"
              :box-shadow "0 2px 8px rgba(0,0,0,0.1)"
              :text-align "center"}}
     
     [:h4 {:style {:color "#2c3e50" :margin "0 0 15px 0"}}
      "🏆 Reputation System"]
     
     [:div.reputation-score
      {:style {:font-size "24px"
               :font-weight "bold"
               :color "#1976d2"
               :margin "10px 0"}}
      (if (number? reputation-score)
        (.toFixed reputation-score 1)
        "No Profile")]
     
     ;; Blockchain info
     [:div.blockchain-info
      {:style {:margin "15px 0" :padding "10px" :background "#f5f5f5" :border-radius "6px"}}
      [:div {:style {:color "#666" :font-size "14px"}}
       "Mempool: " mempool-count " | Chain: " (count chain)]
      [:div {:style {:color "#666" :font-size "12px" :margin-top "5px"}}
       "Curriculum: " (count @(rf/subscribe [::state/curriculum])) " questions"]]
     
     (when profile
       [:div.profile-info
        [:div.archetype
         {:style {:margin "10px 0"}}
         [:span {:style {:font-size "20px"}} (or (:emoji archetype-data) "🔍")]
         [:span {:style {:margin-left "8px" :font-weight "bold"}}
          (name (or (:archetype profile) :explorers))]]
        
        [:div.username
         {:style {:color "#666" :font-size "14px"}}
         "User: " (:username profile)]])]))

;; Test controls component
(defn test-controls
  "Controls for testing the reputation system"
  []
  (let [mempool-count @(rf/subscribe [::state/mempool-count])]
    [:div.test-controls
     {:style {:background-color "#fff3e0"
              :border-radius "8px"
              :padding "20px"
              :margin "20px 0"
              :border-left "4px solid #ff9800"}}
     
     [:h4 {:style {:color "#e65100" :margin "0 0 15px 0"}}
      "🧪 Test Controls"]
     
     [:div.control-buttons
      {:style {:display "flex" :flex-wrap "wrap" :gap "10px"}}
      
      [:button
       {:style {:background "#4caf50" :color "white" :border "none"
                :padding "8px 16px" :border-radius "4px" :cursor "pointer"}
        :on-click #(rf/dispatch [:create-profile "test-user"])}
       "Create Profile"]
      
      [:button
       {:style {:background (if (= 0 mempool-count) "#bdbdbd" "#9c27b0") 
                :color "white" :border "none"
                :padding "8px 16px" :border-radius "4px" 
                :cursor (if (= 0 mempool-count) "not-allowed" "pointer")}
        :disabled (= 0 mempool-count)
        :on-click #(rf/dispatch [:mine-block])}
       "Mine Block"]
      
      [:button
       {:style {:background "#2196f3" :color "white" :border "none"
                :padding "8px 16px" :border-radius "4px" :cursor "pointer"}
        :on-click #(let [results (js/pok.flow.test_complete_cycle)]
                    (js/console.log "Test cycle results:" results))}
       "Run Test Cycle"]
      
      [:button
       {:style {:background "#ff9800" :color "white" :border "none"
                :padding "8px 16px" :border-radius "4px" :cursor "pointer"}
        :on-click #(rf/dispatch [:load-prev-question])}
       "Prev Question"]
      
      [:button
       {:style {:background "#ff9800" :color "white" :border "none"
                :padding "8px 16px" :border-radius "4px" :cursor "pointer"}
        :on-click #(rf/dispatch [:load-next-question])}
       "Next Question"]]]))

;; Unlock modal component
(defn unlock-modal
  "Modal for entering seedphrase to unlock profile"
  []
  (let [seedphrase-input (r/atom "")]
    [:div.unlock-modal
     {:style {:position "fixed"
              :top 0 :left 0 :right 0 :bottom 0
              :background "rgba(0,0,0,0.5)"
              :display "flex"
              :align-items "center"
              :justify-content "center"
              :z-index 1000}}
     [:div.modal-content
      {:style {:background "white"
               :border-radius "12px"
               :padding "30px"
               :max-width "400px"
               :width "90%"
               :box-shadow "0 8px 24px rgba(0,0,0,0.2)"}}
      [:h2 {:style {:color "#2c3e50" :margin "0 0 20px 0" :text-align "center"}}
       "🔐 Unlock Profile"]
      [:p {:style {:color "#666" :margin "0 0 20px 0" :text-align "center"}}
       "Enter your 4-word seedphrase to continue:"]
      [:input
       {:type "text"
        :placeholder "word1 word2 word3 word4"
        :value @seedphrase-input
        :on-change #(reset! seedphrase-input (.. % -target -value))
        :style {:width "100%"
                :padding "12px"
                :border "2px solid #e0e0e0"
                :border-radius "6px"
                :font-size "16px"
                :margin-bottom "20px"
                :font-family "monospace"}}]
      [:div.modal-buttons
       {:style {:display "flex" :gap "10px" :justify-content "center"}}
       [:button
        {:style {:background "#1976d2" :color "white" :border "none"
                 :padding "12px 24px" :border-radius "6px" :cursor "pointer"
                 :font-size "16px" :font-weight "bold"}
         :on-click #(rf/dispatch [:unlock-profile @seedphrase-input])}
        "Unlock"]
       [:button
        {:style {:background "#4caf50" :color "white" :border "none"
                 :padding "12px 24px" :border-radius "6px" :cursor "pointer"
                 :font-size "16px" :font-weight "bold"}
         :on-click #(rf/dispatch [:create-profile "new-user"])}
        "Create New"]]]])


;; Main application panel
(defn main-panel
  "Main application interface combining all components"
  []
  (let [unlocked @(rf/subscribe [:unlocked])
        profile @(rf/subscribe [:profile-visible])]
    [:div.main-panel
     {:style {:max-width "800px"
              :margin "0 auto"
              :padding "20px"
              :font-family "system-ui, -apple-system, sans-serif"}}
     
     ;; Unlock modal if profile not unlocked
     (when (and (not unlocked) (not profile))
       [unlock-modal])
     
     ;; Header
     [:div.app-header
      {:style {:text-align "center" :margin-bottom "30px"}}
      [:h1 {:style {:color "#1976d2" :margin "0"}}
       "🎓 AP Statistics PoK Blockchain"]
      [:p {:style {:color "#666" :margin "10px 0 0 0"}}
       "Interactive Testing Interface"]]
     
     ;; Main content area
     [:div.content-area
      ;; Left column - Question
      [:div.question-section
       [question-panel]]
      
      ;; Right column - Reputation & Controls  
      [:div.sidebar-section
       [reputation-panel]
       [test-controls]]])))