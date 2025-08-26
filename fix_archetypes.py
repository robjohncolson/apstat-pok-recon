#!/usr/bin/env python3
"""
Fix the ARCHETYPES definition
"""

import re

def fix_archetypes():
    """Fix ARCHETYPES constant definition"""
    
    # Read the state.cljs file
    with open('src/pok/state.cljs', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace the ARCHETYPES definition with clean ASCII
    archetypes_pattern = r'\(def \^:const ARCHETYPES\s+\{:aces \{:emoji "[^"]*" :description "[^"]*"\}(?:\s|\S)*?\{:emoji "[^"]*" :description "[^"]*"\}\}\)'
    
    new_archetypes = '''(def ^:const ARCHETYPES
  {:aces {:emoji "🏆" :description "High accuracy, fast responses"}
   :strategists {:emoji "🧠" :description "Thoughtful, deliberate responses"}
   :explorers {:emoji "🔍" :description "Learning and discovering"}
   :learners {:emoji "📚" :description "Steady progress and improvement"}
   :socials {:emoji "🤝" :description "Collaborative and helpful"}})'''
    
    content = re.sub(archetypes_pattern, new_archetypes, content, flags=re.MULTILINE | re.DOTALL)
    
    # Write the updated file
    with open('src/pok/state.cljs', 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("Fixed ARCHETYPES constant definition")

if __name__ == '__main__':
    fix_archetypes()