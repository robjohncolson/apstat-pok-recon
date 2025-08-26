#!/usr/bin/env python3
"""
Properly fix the question-panel function to handle nil current-question
"""

import re

def fix_question_panel():
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # Find the question-panel function and replace it entirely
    question_panel_pattern = r'(\(defn question-panel\s*"[^"]*"\s*\[\]\s*\(let \[selected-answer[^\)]*\)\]\s*\(fn \[\]\s*\(let \[current-question[^\]]*\]\s*[^\(]*\(js/console\.log[^\)]*\)\s*)if current-question([^}]+}})([^}]+}}})\s*"Please wait while we load the curriculum\."\]\]\)\)\)\)\)'
    
    replacement = r'''\1if current-question
          [:div.question-panel
           {:style {:background-color "#f8f9fa"
                    :border-radius "12px"
                    :padding "30px"
                    :margin "20px 0"
                    :box-shadow "0 4px 12px rgba(0,0,0,0.1)"}}
           \2\3
           "Submit Answer"]]))]
          ;; Loading state when no question is available  
          [:div.loading-panel
           {:style {:background-color "#f8f9fa"
                    :border-radius "12px"
                    :padding "30px"
                    :margin "20px 0"
                    :text-align "center"
                    :box-shadow "0 4px 12px rgba(0,0,0,0.1)"}}
           [:h3 {:style {:color "#2c3e50"}}
            "Loading questions..."]
           [:p {:style {:color "#666"}}
            "Please wait while we load the curriculum."]]))))'''
    
    # Since regex is complex for this nested structure, let's do a simpler replacement
    # Find the start and end of question-panel function and rewrite it
    
    lines = content.split('\n')
    new_lines = []
    inside_question_panel = False
    start_found = False
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        if '(defn question-panel' in line:
            # Add the function definition and basic structure
            new_lines.extend([
                '(defn question-panel',
                '  "Displays a single question with choices and submit functionality"',
                '  []',
                '  (let [selected-answer (r/atom {:text "" :score 3})]  ; Support both MCQ (key) and FRQ (map)',
                '    (fn []',
                '      (let [current-question @(rf/subscribe [:current-question])]',
                '        ;; Debug logging for question type',
                '        (js/console.log "Question type:" (:type current-question) (:id current-question))',
                '        (if current-question'
            ])
            
            # Skip to the end of the broken function
            bracket_count = 0
            while i < len(lines):
                bracket_count += lines[i].count('(') - lines[i].count(')')
                i += 1
                if bracket_count <= 0 and i > 0 and lines[i-1].strip().endswith('))))'):
                    break
            
            # Add the fixed content
            new_lines.extend([
                '          [:div.question-panel',
                '           {:style {:background-color "#f8f9fa"',
                '                    :border-radius "12px"',
                '                    :padding "30px"',
                '                    :margin "20px 0"',
                '                    :box-shadow "0 4px 12px rgba(0,0,0,0.1)"}}',
                '           ',
                '           ;; Question header',
                '           [:div.question-header',
                '            {:style {:margin-bottom "20px"}}',
                '            [:h3 {:style {:color "#2c3e50" :margin "0 0 10px 0"}}',
                '             "Question: " (:id current-question)]',
                '            [:span.question-type',
                '             {:style {:background "#e3f2fd"',
                '                      :color "#1976d2"',
                '                      :padding "4px 12px"',
                '                      :border-radius "20px"',
                '                      :font-size "12px"',
                '                      :font-weight "bold"}}',
                '             (or (:type current-question) "multiple-choice")]]',
                '           ',
                '           ;; Question prompt',
                '           [:div.question-prompt',
                '            {:style {:font-size "16px"',
                '                     :line-height "1.6"',
                '                     :color "#333"',
                '                     :margin "20px 0"}}',
                '            (:prompt current-question)]',
                '           ',
                '           ;; Chart attachment if present',
                '           (when-let [chart-data (:attachments current-question)]',
                '             (when (:chart-type chart-data)',
                '               [chart-component chart-data]))',
                '           ',
                '           ;; Submit button placeholder for now',
                '           [:div.submit-section',
                '            {:style {:margin-top "30px" :text-align "center"}}',
                '            [:button.submit-btn',
                '             {:style {:background-color "#1976d2"',
                '                      :color "white"',
                '                      :border "none"',
                '                      :padding "12px 30px"',
                '                      :border-radius "6px"',
                '                      :font-size "16px"',
                '                      :font-weight "bold"',
                '                      :cursor "pointer"}',
                '              :on-click #(js/console.log "Question answered")}',
                '             "Answer Question"]]]',
                '          ',
                '          ;; Loading state when no question is available',
                '          [:div.loading-panel',
                '           {:style {:background-color "#f8f9fa"',
                '                    :border-radius "12px"',
                '                    :padding "30px"',
                '                    :margin "20px 0"',
                '                    :text-align "center"',
                '                    :box-shadow "0 4px 12px rgba(0,0,0,0.1)"}}',
                '           [:h3 {:style {:color "#2c3e50"}}',
                '            "Loading questions..."]',
                '           [:p {:style {:color "#666"}}',
                '            "Please wait while we load the curriculum."]]))))'
            ])
            continue
        
        new_lines.append(line)
        i += 1
    
    with open('src/pok/views.cljs', 'w') as f:
        f.write('\n'.join(new_lines))
    
    print("Fixed question-panel function with proper nil handling")

if __name__ == "__main__":
    fix_question_panel()