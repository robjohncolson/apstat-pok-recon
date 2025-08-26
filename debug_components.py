#!/usr/bin/env python3
"""
Create a simplified main-panel to debug which component is causing nil rendering
"""

import re

def create_debug_main_panel():
    """Replace main-panel with a debugging version"""
    
    # Read the views.cljs file
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # Find the main-panel function
    main_panel_pattern = r'(\(defn main-panel\s+"Main application interface combining all components"\s+\[\]\s+)((?:\s|\S)*?)(\[:div\.main-panel(?:\s|\S)*?)\]\]\)\)'
    
    # Replace with a simplified debug version
    debug_main_panel = '''(defn main-panel
  "DEBUG: Simplified main panel to identify nil components"
  []
  (js/console.log "Rendering main-panel")
  (try
    (let [unlocked (do (js/console.log "Getting unlocked subscription") @(rf/subscribe [:unlocked]))
          profile (do (js/console.log "Getting profile-visible subscription") @(rf/subscribe [:profile-visible]))
          current-question (do (js/console.log "Getting current-question subscription") @(rf/subscribe [:current-question]))]
      (js/console.log "Subscriptions loaded - unlocked:" unlocked "profile:" profile "current-question:" current-question)
      [:div.main-panel
       {:style {:max-width "800px"
                :margin "0 auto"
                :padding "20px"
                :font-family "system-ui, -apple-system, sans-serif"}}
       
       ;; Header only for now
       [:div.app-header
        {:style {:text-align "center" :margin-bottom "30px"}}
        [:h1 {:style {:color "#1976d2" :margin "0"}}
         "🎓 AP Statistics PoK Blockchain - DEBUG MODE"]
        [:p {:style {:color "#666" :margin "10px 0 0 0"}}
         "Interactive Testing Interface"]]
       
       ;; Simple content to test
       [:div.debug-info
        [:p "Debug Info:"]
        [:p (str "Unlocked: " unlocked)]
        [:p (str "Profile: " (if profile "exists" "nil"))]
        [:p (str "Current question: " (if current-question (:id current-question) "nil"))]]
       
       ;; Try rendering question panel cautiously
       (if current-question
         [:div "Question panel would go here"]
         [:div "No current question available"])
         
       ])
    (catch js/Error e
      (js/console.error "Error in main-panel:" e)
      [:div.error "Error rendering main panel: " (str e)])))'''
    
    content = re.sub(main_panel_pattern, debug_main_panel, content, flags=re.MULTILINE | re.DOTALL)
    
    # Write the updated file
    with open('src/pok/views.cljs', 'w') as f:
        f.write(content)
    
    print("Created debug version of main-panel")

if __name__ == '__main__':
    create_debug_main_panel()