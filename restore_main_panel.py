#!/usr/bin/env python3
"""
Restore the original main-panel component now that we've fixed the keyword mismatch
"""

import re

def restore_main_panel():
    """Replace debug main-panel with the original full version"""
    
    # Read the views.cljs file
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # Replace the debug main-panel with the original
    debug_main_panel_pattern = r'\(defn main-panel\s+"DEBUG: Simplified main panel to identify nil components"(?:\s|\S)*?\]\]\)\)'
    
    original_main_panel = '''(defn main-panel
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
     
     ;; QR sync modal if shown
     (when @(rf/subscribe [:qr-modal-visible])
       [qr-modal])
     
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
       [test-controls]]]]))'''
    
    content = re.sub(debug_main_panel_pattern, original_main_panel, content, flags=re.MULTILINE | re.DOTALL)
    
    # Write the updated file
    with open('src/pok/views.cljs', 'w') as f:
        f.write(content)
    
    print("Restored original main-panel component")

if __name__ == '__main__':
    restore_main_panel()