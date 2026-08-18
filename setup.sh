#!/bin/bash

echo "🍳 Food Recipe Recommendation Agent Setup"
echo "=========================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version $JAVA_VERSION detected. Please install Java 17 or higher."
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

echo "✅ Maven detected"

# Check for Gemini API key
if [ -z "$GEMINI_API_KEY" ] && [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  GEMINI_API_KEY environment variable is not set."
    echo "Please set your Gemini API key using one of these methods:"
    echo ""
    echo "Method 1: Add to .env file"
    echo "GEMINI_API_KEY=your-gemini-api-key-here"
    echo ""
    echo "Method 2: Export as environment variable"
    echo "export GEMINI_API_KEY=\"your-gemini-api-key-here\""
    echo ""
    read -p "Do you want to continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ Gemini API key found in environment"
fi

# Build the project
echo ""
echo "🔨 Building the project..."
mvn clean install

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🚀 Starting the application..."
    echo "The application will be available at: http://localhost:8080"
    echo "Press Ctrl+C to stop the application"
    echo ""
    mvn spring-boot:run
else
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi
