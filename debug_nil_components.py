#!/usr/bin/env python3
"""
Debug nil components in ClojureScript by adding console logging
to identify exactly which component is returning nil
"""

import re

def add_debug_logging():
    """Add console.log statements to identify nil components"""
    
    # Read the views.cljs file
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # Find all component definitions
    component_pattern = r'\(defn\s+([a-zA-Z0-9-]+)\s*(?:\[.*?\])?\s*(?:"[^"]*")?\s*(?:\[.*?\])?\s*((?:\s|\S)*?)(?=\n\(defn|\n\n|\Z)'
    
    components = []
    for match in re.finditer(component_pattern, content, re.MULTILINE | re.DOTALL):
        component_name = match.group(1)
        if not component_name.startswith('->'):  # Skip record constructors
            components.append(component_name)
    
    print(f"Found {len(components)} components:")
    for comp in components:
        print(f"  - {comp}")
    
    # Add debug logging to main-panel specifically
    main_panel_pattern = r'(\(defn\s+main-panel\s*\[.*?\]\s*)((?:\s|\S)*?)(\[:div\.main-panel(?:\s|\S)*?\]\))'
    
    def add_main_panel_debug(match):
        start = match.group(1)
        body = match.group(2)
        div_content = match.group(3)
        
        # Add debug logging
        debug_log = '''(js/console.log "main-panel rendering with:"
                   {:current-question @(rf/subscribe [:current-question])
                    :profile @(rf/subscribe [:profile])
                    :profile-archetype @(rf/subscribe [:profile-archetype-data])
                    :curriculum @(rf/subscribe [:curriculum])
                    :mempool-count @(rf/subscribe [::state/mempool-count])
                    :chain @(rf/subscribe [::state/chain])})
  '''
        
        return start + debug_log + body + div_content
    
    updated_content = re.sub(main_panel_pattern, add_main_panel_debug, content, flags=re.MULTILINE | re.DOTALL)
    
    # Also add nil checking to each subscription call
    subscription_pattern = r'@\(rf/subscribe\s+\[([^\]]+)\]\)'
    
    def wrap_subscription(match):
        sub_key = match.group(1)
        return f'(let [sub-val @(rf/subscribe [{sub_key}])] (js/console.log "Subscription {sub_key}:" sub-val) sub-val)'
    
    # Apply to specific problematic areas
    updated_content = re.sub(r'@\(rf/subscribe\s+\[:current-question\]\)', 
                           '(let [cq @(rf/subscribe [:current-question])] (js/console.log "current-question:" cq) cq)', 
                           updated_content)
    
    updated_content = re.sub(r'@\(rf/subscribe\s+\[:profile-archetype-data\]\)', 
                           '(let [pad @(rf/subscribe [:profile-archetype-data])] (js/console.log "profile-archetype-data:" pad) pad)', 
                           updated_content)
    
    # Write the updated file
    with open('src/pok/views.cljs', 'w') as f:
        f.write(updated_content)
    
    print("Added debug logging to views.cljs")

if __name__ == '__main__':
    add_debug_logging()