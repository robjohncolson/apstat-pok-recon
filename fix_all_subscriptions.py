#!/usr/bin/env python3
"""
Fix all subscriptions to return proper defaults instead of nil
"""

import re

def fix_subscriptions():
    """Add nil handling to all subscriptions"""
    
    # Read the state.cljs file
    with open('src/pok/state.cljs', 'r') as f:
        content = f.read()
    
    # Fix ::curriculum subscription
    curriculum_pattern = r'(\(rf/reg-sub\s+::curriculum\s+\(fn \[db _\]\s+)\(:curriculum db\)(\))'
    curriculum_replacement = r'\1(or (:curriculum db) [])\2'
    content = re.sub(curriculum_pattern, curriculum_replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    # Fix ::mempool subscription
    mempool_pattern = r'(\(rf/reg-sub\s+::mempool\s+\(fn \[db _\]\s+)\(:mempool db\)(\))'
    mempool_replacement = r'\1(or (:mempool db) [])\2'
    content = re.sub(mempool_pattern, mempool_replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    # Fix ::chain subscription
    chain_pattern = r'(\(rf/reg-sub\s+::chain\s+\(fn \[db _\]\s+)\(:chain db\)(\))'
    chain_replacement = r'\1(or (:chain db) [])\2'
    content = re.sub(chain_pattern, chain_replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    # Fix ::mempool-count subscription
    mempool_count_pattern = r'(\(rf/reg-sub\s+::mempool-count\s+\(fn \[db _\]\s+)\(count \(:mempool db\)\)(\))'
    mempool_count_replacement = r'\1(count (or (:mempool db) []))\2'
    content = re.sub(mempool_count_pattern, mempool_count_replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    # Fix ::qr-modal-visible subscription (already has default false, but let's check)
    
    # Write the updated file
    with open('src/pok/state.cljs', 'w') as f:
        f.write(content)
    
    print("Added nil handling to all subscriptions")

if __name__ == '__main__':
    fix_subscriptions()