# Git Repository Setup for SeatMap Pro

## Step 1: Initialize Git Repository (if not already done)
```bash
git init
```

## Step 2: Add All Files to Git
```bash
# Add all files except those in .gitignore
git add .

# Check what files will be committed
git status
```

## Step 3: Create Initial Commit
```bash
git commit -m "Initial commit: SeatMap Pro with Amadeus integration

- Complete flight booking application with real-time seat selection
- Amadeus API integration for flight search and seat maps
- React + TypeScript frontend with TailwindCSS
- Convex backend with real-time database
- User authentication with Convex Auth
- Interactive seat map visualization
- Airport search with caching
- Flight search with results display
- Responsive design for mobile and desktop"
```

## Step 4: Add Remote Repository
Replace `YOUR_GITHUB_USERNAME` and `YOUR_REPO_NAME` with your actual values:

```bash
# For GitHub
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME.git

# Or if you prefer SSH (recommended for frequent pushes)
git remote add origin git@github.com:YOUR_GITHUB_USERNAME/YOUR_REPO_NAME.git
```

## Step 5: Push to Remote Repository
```bash
# Push to main branch
git branch -M main
git push -u origin main
```

## Alternative: If Repository Already Exists
If you already have a remote repository with some content:

```bash
# Pull existing content first
git pull origin main --allow-unrelated-histories

# Then push your changes
git push origin main
```

## Verify Upload
```bash
# Check remote connection
git remote -v

# Check branch status
git branch -a

# Check last commit
git log --oneline -5
```

## Files That Will Be Uploaded
Based on your .gitignore, these files will be included:
- All source code in `src/` and `convex/`
- Configuration files (package.json, tsconfig.json, etc.)
- Documentation (README.md, CHANGELOG.md, etc.)
- Built assets in `dist/` (if you want to include them)

## Files That Will Be Excluded
- `node_modules/` (dependencies)
- `.env` files (environment variables)
- `.convex/` (Convex build files)
- IDE files (.vscode/, .idea/)
- OS files (.DS_Store, Thumbs.db)

## Important Notes
1. **Environment Variables**: Your `.env.local` file is excluded for security
2. **API Keys**: Make sure no API keys are hardcoded in your source files
3. **Convex Deployment**: Your Convex deployment (flexible-firefly-91) is separate from Git
4. **Dependencies**: Others will need to run `npm install` after cloning
