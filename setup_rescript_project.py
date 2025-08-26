#!/usr/bin/env python3
"""
ReScript Project Setup Automation Script
Gathers files from the current PoK blockchain repo and creates a new ReScript project directory.
"""

import os
import shutil
import json
import argparse
import glob
from pathlib import Path

def ensure_dir(path):
    """Create directory if it doesn't exist."""
    os.makedirs(path, exist_ok=True)

def safe_copy_file(src, dst, description=""):
    """Copy file with error handling."""
    try:
        if os.path.exists(src):
            ensure_dir(os.path.dirname(dst))
            shutil.copy2(src, dst)
            print(f"✓ Copied {description or src} -> {dst}")
            return True
        else:
            print(f"⚠ File not found: {src} (skipping)")
            return False
    except Exception as e:
        print(f"✗ Error copying {src}: {e}")
        return False

def safe_copy_tree(src, dst, description=""):
    """Copy directory tree with error handling."""
    try:
        if os.path.exists(src):
            if os.path.exists(dst):
                shutil.rmtree(dst)
            shutil.copytree(src, dst)
            print(f"✓ Copied directory {description or src} -> {dst}")
            return True
        else:
            print(f"⚠ Directory not found: {src} (skipping)")
            return False
    except Exception as e:
        print(f"✗ Error copying directory {src}: {e}")
        return False

def find_file(filename, search_paths):
    """Find file in multiple possible locations."""
    for path in search_paths:
        full_path = os.path.join(path, filename)
        if os.path.exists(full_path):
            return full_path
    return None

def generate_bsconfig(project_name):
    """Generate bsconfig.json content."""
    config = {
        "name": project_name,
        "sources": {
            "dir": "src",
            "subdirs": True
        },
        "package-specs": ["es6"],
        "suffix": ".bs.js",
        "bs-dependencies": ["@rescript/react"],
        "jsx": {"version": 4}
    }
    return json.dumps(config, indent=2)

def generate_package_json(project_name):
    """Generate package.json content."""
    package = {
        "name": project_name,
        "version": "0.1.0",
        "description": "ReScript port of AP Statistics PoK Blockchain application",
        "scripts": {
            "build": "rescript",
            "clean": "rescript clean",
            "start": "rescript build -w",
            "dev": "rescript build -w"
        },
        "dependencies": {
            "@rescript/react": "^0.12.0",
            "chart.js": "^4.4.0",
            "rescript": "^12.0.0",
            "react": "^18.0.0",
            "react-dom": "^18.0.0"
        },
        "devDependencies": {
            "@rescript/core": "^1.0.0"
        }
    }
    return json.dumps(package, indent=2)

def generate_index_html(project_name, has_chartjs=False):
    """Generate index.html content."""
    chartjs_script = ""
    if has_chartjs:
        chartjs_script = '    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.js"></script>\n'
    
    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{project_name} - AP Statistics PoK Blockchain</title>
{chartjs_script}    <style>
        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
            padding: 30px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div id="root">
            <div style="text-align: center; padding: 40px;">
                <h1>🎓 AP Statistics PoK Blockchain</h1>
                <p>ReScript Implementation Loading...</p>
                <div style="margin-top: 20px;">
                    <div style="border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto;"></div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- ReScript compiled output -->
    <script type="module" src="./lib/js/src/Index.bs.js"></script>
    
    <style>
        @keyframes spin {{
            0% {{ transform: rotate(0deg); }}
            100% {{ transform: rotate(360deg); }}
        }}
    </style>
</body>
</html>"""
    return html

def generate_readme(project_name):
    """Generate README.md content."""
    readme = f"""# {project_name}

ReScript port of the AP Statistics Proof of Knowledge (PoK) Blockchain application.

## Overview

This is a modern ReScript implementation of the educational blockchain system originally developed in ClojureScript. The application implements a decentralized proof-of-knowledge system for AP Statistics education with emergent consensus and peer attestation.

## Key Features

- **Decentralized Consensus**: No centralized answer keys - truth emerges through peer agreement
- **Reputation System**: Dynamic scoring with time decay and minority bonuses
- **Blockchain Architecture**: Immutable transaction history with distribution tracking
- **Educational Archetypes**: Students classified as Aces, Strategists, Explorers, Learners, or Socials
- **QR Code Sync**: Share state between devices via QR codes
- **Chart.js Integration**: Rich data visualization for statistics problems

## Architecture References

This ReScript implementation is based on:
- **ClojureScript Original** (`reference/cljs/`): Re-frame + Reagent web application
- **Racket Prototypes** (`reference/racket/`): Original algorithm implementations
- **ADRs** (`adr/`): Architectural Decision Records explaining design choices

Key ADRs:
- ADR-012: Social Consensus and Proof of Knowledge
- ADR-028: Emergent Attestation with Optional Reveals

## Quick Start

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Start development**:
   ```bash
   npm start
   ```
   This runs ReScript in watch mode, rebuilding on file changes.

3. **Build for production**:
   ```bash
   npm run build
   ```

4. **Clean build artifacts**:
   ```bash
   npm run clean
   ```

## Development

- **Source**: `src/` directory (ReScript files)
- **Output**: `lib/js/src/` directory (compiled JavaScript)
- **Entry Point**: `src/Index.res` (create this file to start)

### Recommended File Structure

```
src/
├── Index.res              # Main entry point
├── State.res              # App state management
├── Components/            # React components
│   ├── QuestionPanel.res
│   ├── ProfileDisplay.res
│   └── BlockchainView.res
├── Types.res              # Type definitions
├── Blockchain.res         # Core blockchain logic
├── Reputation.res         # Reputation calculations
└── Utils.res              # Helper functions
```

## Implementation Notes

### Key Differences from ClojureScript Version

1. **Type Safety**: ReScript provides compile-time type checking
2. **Pattern Matching**: More expressive than ClojureScript's cond
3. **Immutability**: Built-in immutable data structures
4. **React Integration**: Native JSX support with ReScript-React

### Porting Guidelines

1. **State Management**: Convert Re-frame subscriptions to React hooks/context
2. **Data Structures**: Use ReScript records and variants instead of maps
3. **Async Operations**: Use promises/async-await instead of core.async
4. **UI Components**: Convert Hiccup syntax to JSX

## Reference Implementation Comparison

| Feature | ClojureScript | Racket | ReScript Target |
|---------|---------------|---------|-----------------|
| State Management | Re-frame | Parameters | React Context |
| UI Framework | Reagent | Console | React |
| Data Structures | Maps/Vectors | Structs/Lists | Records/Arrays |
| Type System | Dynamic | Dynamic | Static |
| Async | core.async | Threads | Promises |

## Data Flow

1. **Question Display**: Load from curriculum.json
2. **Answer Submission**: Create transaction, add to mempool
3. **Mining**: Validate transactions, create block with attestations
4. **Reputation Update**: Calculate new scores based on accuracy
5. **State Persistence**: Save to localStorage, export via QR

## Testing

The reference implementations include comprehensive test suites:
- Racket: `rackunit` tests in prototype modules
- ClojureScript: Integration tests in browser

Plan to add ReScript tests using Jest/ReScript-Jest.

## Contributing

1. Study the reference implementations in `reference/` 
2. Read the ADRs in `adr/` for design context
3. Implement features incrementally, starting with core types
4. Test against the Racket behavioral specifications

## License

Educational use - AP Statistics PoK Blockchain Project

---

🚀 **Ready to start coding!** Begin by creating `src/Index.res` and implementing the core types and state management.
"""
    return readme

def create_rescript_project(repo_root=".", new_dir_name="my-rescript-app"):
    """Main function to create ReScript project."""
    
    print(f"🚀 Creating ReScript project: {new_dir_name}")
    print(f"📁 Source repository: {os.path.abspath(repo_root)}")
    
    # Create main project directory
    project_root = os.path.join(".", new_dir_name)
    ensure_dir(project_root)
    
    # Create subdirectories
    subdirs = [
        "src",
        "reference/racket",
        "reference/cljs", 
        "reference/data",
        "adr",
        "assets"
    ]
    
    for subdir in subdirs:
        ensure_dir(os.path.join(project_root, subdir))
    
    print("\n📂 Directory structure created")
    
    # Copy Racket files
    print("\n🔧 Copying Racket prototype files...")
    racket_files = [
        ("racket-digital-twin.rkt", "racket-digital-twin.rkt"),
        ("analysis/racket-proto/main.rkt", "main.rkt"),
        ("analysis/racket-proto/blockchain.rkt", "blockchain.rkt"),
        ("analysis/racket-proto/consensus.rkt", "consensus.rkt"),
        ("analysis/racket-proto/profile.rkt", "profile.rkt"),
        ("analysis/racket-proto/parser.rkt", "parser.rkt"),
        ("analysis/racket-proto/transaction.rkt", "transaction.rkt"),
        ("analysis/racket-proto/video-integration.rkt", "video-integration.rkt"),
        ("analysis/racket-proto/test-runner.rkt", "test-runner.rkt"),
        ("prototype.rkt", "prototype.rkt")
    ]
    
    for src_file, dst_file in racket_files:
        src_path = os.path.join(repo_root, src_file)
        dst_path = os.path.join(project_root, "reference/racket", dst_file)
        safe_copy_file(src_path, dst_path, f"Racket: {dst_file}")
    
    # Copy CLJS files - only copy the pok directory to avoid copying the Python script
    print("\n⚛️ Copying ClojureScript source files...")
    pok_src = os.path.join(repo_root, "src", "pok")
    if os.path.exists(pok_src):
        # Copy all .cljs files from src/pok/
        cljs_files = glob.glob(os.path.join(pok_src, "*.cljs"))
        ensure_dir(os.path.join(project_root, "reference/cljs/src/pok"))
        for cljs_file in cljs_files:
            filename = os.path.basename(cljs_file)
            dst_path = os.path.join(project_root, "reference/cljs/src/pok", filename)
            safe_copy_file(cljs_file, dst_path, f"CLJS: {filename}")
    
    # Copy ADRs
    print("\n📋 Copying Architecture Decision Records...")
    if os.path.exists(os.path.join(repo_root, "adr")):
        adr_files = glob.glob(os.path.join(repo_root, "adr", "*.md"))
        for adr_file in adr_files:
            filename = os.path.basename(adr_file)
            dst_path = os.path.join(project_root, "adr", filename)
            safe_copy_file(adr_file, dst_path, f"ADR: {filename}")
    
    # Copy data files
    print("\n📊 Copying data files...")
    data_files = [
        ("curriculum.json", ["public", "legacy", "."]),
        ("allUnitsData.js", ["legacy", "."]),
    ]
    
    for filename, search_paths in data_files:
        full_search_paths = [os.path.join(repo_root, p) for p in search_paths]
        src_path = find_file(filename, full_search_paths)
        
        if src_path and os.path.exists(src_path):
            dst_path = os.path.join(project_root, "reference/data", filename)
            safe_copy_file(src_path, dst_path, f"Data: {filename}")
    
    # Copy assets
    print("\n🎨 Copying asset files...")
    asset_files = [
        "legacy/quiz_renderer.html",
        "public/index.html",
    ]
    
    has_chartjs = False
    for asset_file in asset_files:
        src_path = os.path.join(repo_root, asset_file)
        if os.path.exists(src_path):
            dst_name = os.path.basename(asset_file)
            if asset_file.startswith("legacy/"):
                dst_name = "quiz_renderer.html"  # Preserve original name
            dst_path = os.path.join(project_root, "assets", dst_name)
            safe_copy_file(src_path, dst_path, f"Asset: {dst_name}")
            
            # Check if file contains Chart.js references
            try:
                with open(src_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if 'chart.js' in content.lower():
                        has_chartjs = True
            except:
                pass
    
    # Generate ReScript configuration files
    print("\n⚙️ Generating ReScript configuration files...")
    
    # bsconfig.json
    bsconfig_path = os.path.join(project_root, "bsconfig.json")
    with open(bsconfig_path, 'w') as f:
        f.write(generate_bsconfig(new_dir_name))
    print("✓ Generated bsconfig.json")
    
    # package.json
    package_path = os.path.join(project_root, "package.json")
    with open(package_path, 'w') as f:
        f.write(generate_package_json(new_dir_name))
    print("✓ Generated package.json")
    
    # index.html
    index_path = os.path.join(project_root, "index.html")
    with open(index_path, 'w') as f:
        f.write(generate_index_html(new_dir_name, has_chartjs))
    print("✓ Generated index.html")
    
    # README.md
    readme_path = os.path.join(project_root, "README.md")
    with open(readme_path, 'w') as f:
        f.write(generate_readme(new_dir_name))
    print("✓ Generated README.md")
    
    # Create a starter Index.res file
    index_res_path = os.path.join(project_root, "src", "Index.res")
    starter_code = '''// Main entry point for AP Statistics PoK Blockchain ReScript app
// Based on ClojureScript implementation in reference/cljs/

// TODO: Implement core types and state management
// See reference/racket/ for behavioral specifications
// See adr/ for architectural decisions

@react.component
let make = () => {
  <div className="app-container">
    <h1> {"🎓 AP Statistics PoK Blockchain"->React.string} </h1>
    <p> {"ReScript implementation starting..."->React.string} </p>
    <div className="next-steps">
      <h3> {"Next Steps:"->React.string} </h3>
      <ol>
        <li> {"Define core types (Profile, Transaction, Block)"->React.string} </li>
        <li> {"Implement state management"->React.string} </li>
        <li> {"Create UI components"->React.string} </li>
        <li> {"Port blockchain logic from reference implementations"->React.string} </li>
      </ol>
    </div>
  </div>
}

// Initialize the app
switch ReactDOM.querySelector("#root") {
| Some(rootElement) => {
    let root = ReactDOM.Client.createRoot(rootElement)
    root->ReactDOM.Client.Root.render(<Index />)
  }
| None => Js.Console.error("Root element not found")
}
'''
    
    with open(index_res_path, 'w') as f:
        f.write(starter_code)
    print("✓ Generated starter src/Index.res")
    
    # Create .gitignore
    gitignore_path = os.path.join(project_root, ".gitignore")
    gitignore_content = """# ReScript build artifacts
lib/
*.bs.js
*.gen.ts

# Dependencies
node_modules/

# IDE
.vscode/
.idea/

# OS
.DS_Store
Thumbs.db

# Logs
npm-debug.log*
yarn-debug.log*
yarn-error.log*
"""
    
    with open(gitignore_path, 'w') as f:
        f.write(gitignore_content)
    print("✓ Generated .gitignore")
    
    print(f"\n🎉 Setup complete!")
    print(f"📁 Project created in: {os.path.abspath(project_root)}")
    print(f"\n🚀 Next steps:")
    print(f"   1. Move {new_dir_name}/ to your desired location")
    print(f"   2. cd {new_dir_name}")
    print(f"   3. git init")
    print(f"   4. npm install")
    print(f"   5. npm start")
    print(f"\n📖 Read README.md for detailed implementation guidance")
    print(f"🔍 Study reference/ directory for ClojureScript and Racket implementations")

def main():
    """Main entry point with argument parsing."""
    parser = argparse.ArgumentParser(
        description="Create a ReScript project from the current PoK blockchain repository"
    )
    parser.add_argument(
        "--name", 
        default="my-rescript-app",
        help="Name of the new ReScript project directory (default: my-rescript-app)"
    )
    parser.add_argument(
        "--source",
        default=".",
        help="Path to the source repository (default: current directory)"
    )
    
    args = parser.parse_args()
    
    if not os.path.exists(args.source):
        print(f"❌ Source directory not found: {args.source}")
        return 1
    
    try:
        create_rescript_project(args.source, args.name)
        return 0
    except Exception as e:
        print(f"❌ Error creating project: {e}")
        return 1

if __name__ == "__main__":
    exit(main())