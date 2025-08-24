#!/bin/bash
# AP Statistics PoK Blockchain - Phase 5 Deployment Script
# Creates production-ready deployment package

echo "🚀 AP Statistics PoK Blockchain - Phase 5 Deployment"
echo "=================================================="

# Create deployment directory
echo "📁 Creating deployment package..."
mkdir -p dist/apstat-pok-recon

# Copy essential files
cp -r public/* dist/apstat-pok-recon/
cp README.md dist/apstat-pok-recon/
cp PHASE5-COMPLETE.md dist/apstat-pok-recon/

# Create teacher guide
cat > dist/apstat-pok-recon/TEACHER-GUIDE.md << 'EOF'
# AP Statistics PoK Blockchain - Teacher Setup Guide

## Quick Start (5 minutes)

1. **Extract** the deployment package to any location
2. **Open** `index.html` in Chrome 90+ browser  
3. **Students** access via file:// URL (no server needed)
4. **QR Sync** built-in for sharing blockchain between devices

## Features for Classroom

- ✅ **816 AP Statistics Questions** with charts/tables/videos
- ✅ **Offline Operation** - no internet required
- ✅ **5 Student Archetypes** - gamified learning progression  
- ✅ **Peer Consensus** - collaborative answer validation
- ✅ **QR Sync** - share progress between devices
- ✅ **Sub-50ms Performance** - responsive on school hardware

## Technical Requirements

- **Browser**: Chrome 90+, Firefox 88+, Safari 14+
- **Hardware**: 4GB RAM, dual-core processor minimum
- **Storage**: 32KB for application + student data
- **Network**: None required after initial load

## Troubleshooting

- **Slow Performance**: Clear browser cache and restart
- **QR Sync Issues**: Ensure camera permissions enabled
- **Questions Not Loading**: Verify JavaScript enabled in browser
- **Charts Not Rendering**: Check Chart.js embedded properly

Support: See PHASE5-COMPLETE.md for technical details
EOF

# Create deployment package size summary
echo "📊 Analyzing package contents..."
cd dist/apstat-pok-recon
find . -type f -exec ls -lh {} \; | awk '{print $5 "\t" $9}' > PACKAGE-CONTENTS.txt
cd ../..

# Calculate total size
TOTAL_SIZE=$(du -sh dist/apstat-pok-recon | cut -f1)

echo "✅ Deployment package created:"
echo "   Location: dist/apstat-pok-recon/"
echo "   Total size: $TOTAL_SIZE" 
echo "   Files included:"
echo "   - index.html (application entry)"
echo "   - styles.css (responsive UI)"
echo "   - js/main.js (production bundle)"
echo "   - README.md (technical documentation)"
echo "   - PHASE5-COMPLETE.md (completion report)"
echo "   - TEACHER-GUIDE.md (setup instructions)"

echo ""
echo "🎯 Deployment Ready!"
echo "📦 Package: dist/apstat-pok-recon/"
echo "🚀 Setup: Open index.html in Chrome 90+"
echo "📱 Share: Use built-in QR sync for device transfer"
echo ""
echo "Phase 5 Complete ✅"
