# MyGit - Java Git Implementation

A lightweight, educational implementation of Git version control system written in Java. This project recreates core Git functionality including commits, branches, staging, and more.

## Features

### Core Git Operations
- **Repository Management**: Initialize new repositories
- **Staging Area**: Add, remove, and track file changes
- **Commits**: Create commits with author information and timestamps
- **Branching**: Create, delete, and list branches
- **Checkout**: Switch between branches and commits (including detached HEAD)
- **Status**: View working directory and staging area state
- **Log**: View commit history
- **Tags**: Create, list, delete, and view tags
- **Reset**: Soft, mixed, and hard reset operations

### Advanced Features
- SHA-1 hashing for object identification
- Object compression using Deflate
- Index file with checksums
- Tree structure for directories
- Parent commit tracking
- File modification detection
- Empty directory cleanup

## Architecture

### Core Components

#### 1. Git Objects (`org.example.objects`)
- **Blob**: Stores file content
- **Tree**: Represents directory structure with entries (files and subdirectories)
- **Commit**: Contains tree reference, parent commits, author, committer, and message
- **GitObject**: Abstract base class with hash computation

#### 2. Repository Layer (`org.example.repository`)
- **Repository**: Main facade for all Git operations
- **ObjectStorage**: Handles object serialization, compression, and storage
- **RefStorage**: Manages branches, HEAD, and references
- **Index**: Staging area implementation with file tracking
- **IndexEntry**: Represents staged files with metadata

#### 3. Utilities (`org.example.utils`)
- **SHA1Hasher**: SHA-1 hash computation and hex conversion
- **Colors**: ANSI color codes for terminal output

#### 4. Commands (`org.example.commands`)
- Command pattern implementation for each Git operation
- **CommandParser**: Routes commands to appropriate handlers

### Data Storage

```
.git/
├── objects/           # Compressed Git objects (blobs, trees, commits)
│   └── [hash]/
├── refs/
│   ├── heads/        # Branch references
│   └── tags/         # Tag references
├── HEAD              # Current branch or commit reference
└── index             # Staging area with SHA-1 checksum
```

## Prerequisites

- Java 21 or higher
- Maven 3.6+

## Installation

### Local Build

```bash
# Clone the repository
git clone https://github.com/mshahnaza/simplegit.git
cd mygit

# Build with Maven
mvn clean package

# Run
java -jar target/simplegit-1.0-SNAPSHOT.jar <command>
```

### Docker Build

```bash
# Build Docker image
docker build -t mygit .

# Run container
docker run -v $(pwd):/workspace mygit <command>
```

## Usage

### Basic Workflow

```bash
# Initialize a new repository
mygit init

# Add files to staging area
mygit add file.txt
mygit add .

# Commit changes
mygit commit -m "Initial commit" -a "John Doe"

# View status
mygit status

# View commit history
mygit log

# Create a branch
mygit branch feature-branch

# Switch branches
mygit checkout feature-branch

# Create and switch to new branch
mygit checkout -b new-feature
```

## Commands

### Repository Initialization
```bash
mygit init
```

### Staging Operations
```bash
# Add single file
mygit add <file>

# Add all files
mygit add .

# Remove file from staging and/or working directory
mygit rm <file>
mygit rm --cached <file>  # Remove from staging only
mygit rm --force <file>   # Force removal even with modifications
```

### Committing
```bash
mygit commit -m "Commit message" -a "Author Name"
```

### Status and History
```bash
# View repository status
mygit status

# View commit log
mygit log
```

### Branch Operations
```bash
# List branches
mygit branch

# Create new branch
mygit branch <branch-name>

# Delete branch
mygit branch -d <branch-name>

# Switch to branch
mygit checkout <branch-name>

# Create and switch to new branch
mygit checkout -b <branch-name>

# Checkout specific commit (detached HEAD)
mygit checkout <commit-hash>
```

### Tag Operations
```bash
# Create tag
mygit tag <tag-name>

# List tags
mygit tag

# Show tag information
mygit tag -s <tag-name>

# Delete tag
mygit tag -d <tag-name>
```

### Reset Operations
```bash
# Soft reset (keep staging and working directory)
mygit reset --soft <commit>

# Mixed reset (default, reset staging but keep working directory)
mygit reset --mixed <commit>
mygit reset <commit>

# Hard reset (reset everything)
mygit reset --hard <commit>

# Reset to previous commits
mygit reset HEAD~1
mygit reset HEAD~3
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── org/
│           └── example/
│               ├── commands/          # Command implementations
│               │   ├── AddCommand.java
│               │   ├── BranchCommand.java
│               │   ├── CheckoutCommand.java
│               │   ├── CommitCommand.java
│               │   ├── InitCommand.java
│               │   ├── LogCommand.java
│               │   ├── RemoveCommand.java
│               │   ├── ResetCommand.java
│               │   ├── StatusCommand.java
│               │   ├── TagCommand.java
│               │   ├── Command.java
│               │   └── CommandParser.java
│               ├── objects/           # Git object models
│               │   ├── Blob.java
│               │   ├── Commit.java
│               │   ├── GitObject.java
│               │   └── Tree.java
│               ├── repository/        # Repository management
│               │   ├── Index.java
│               │   ├── IndexEntry.java
│               │   ├── ObjectStorage.java
│               │   ├── RefStorage.java
│               │   └── Repository.java
│               └── utils/             # Utility classes
│                   ├── Colors.java
│                   └── SHA1Hasher.java
└── test/
    └── java/
        └── org/
            └── example/
                ├── integration/       # Integration tests
                └── unit/              # Unit tests
```

## CI/CD Pipeline

The project includes a comprehensive GitHub Actions workflow:

### Pipeline Stages

1. **Test and Build**
   - Checkout code
   - Set up Java 21 (Temurin distribution)
   - Run unit tests (`mvn test`)
   - Build JAR package (`mvn package`)

2. **Deploy**
   - Build Docker image
   - Push to Docker Hub
   - Deploy JAR to remote server
   - Configure server environment
   - Create executable wrapper script

### Configuration

Required GitHub Secrets:
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password
- `SERVER_HOST`: Deployment server hostname/IP
- `SERVER_USERNAME`: Server SSH username
- `SERVER_PASSWORD`: Server SSH password

### Workflow File
`.github/workflows/github-action.yml`

Triggers on push to `main` branch.

## Testing

### Test Structure
- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions and workflows

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run with coverage
mvn test jacoco:report
```

## Deployment

### Server Deployment

The CI/CD pipeline automatically deploys to a remote server:

1. JAR is copied to `/opt/mygit/`
2. Renamed to `mygit.jar`
3. Wrapper script created at `/opt/mygit/run.sh`
4. Global executable created at `/usr/local/bin/mygit`

After deployment, users can run:
```bash
mygit <command>
```

### Docker Deployment

```bash
# Pull from Docker Hub
docker pull mshahnaza/mygit

# Run
docker run -v $(pwd):/workspace <docker-username>/mygit <command>
```

## Technical Details

### Hash Computation
Objects are hashed using SHA-1:
```
hash = SHA1(type + " " + size + "\0" + content)
```

### Object Storage
- Objects compressed with Deflate algorithm
- Stored in `.git/objects/<first-2-chars>/<remaining-38-chars>`
- Format: `type size\0content`

### Index Format
- Binary format with SHA-1 checksum
- Stores: path, hash, mode, size, modification time
- Sorted by path for consistent tree building

### Commit Format
```
tree <tree-hash>
parent <parent-hash>
author <name> <timestamp> <timezone>
committer <name> <timestamp> <timezone>

<commit message>
```

### Tree Format
Binary format with entries:
```
<mode> <name>\0<20-byte-sha1>
```

### Branch and HEAD
- Branches stored in `.git/refs/heads/<branch-name>`
- HEAD can point to:
  - A branch: `ref: refs/heads/<branch-name>`
  - A commit (detached): `<commit-hash>`
