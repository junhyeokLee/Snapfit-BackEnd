#!/bin/bash

# SnapFit Firebase Environment Setup Script
# This script helps setting up environment variables for the Spring Boot backend on EC2.

echo "--- SnapFit Firebase Setup ---"

# 1. Ask for Firebase Service Account JSON Path
read -p "Enter the absolute path to your Firebase Service Account JSON file (e.g., /home/ec2-user/firebase-key.json): " KEY_PATH

if [ ! -f "$KEY_PATH" ]; then
    echo "Warning: File $KEY_PATH does not exist. Please make sure the file is uploaded to the server."
fi

# 2. Ask for Firebase Storage Bucket Name
read -p "Enter your Firebase Storage Bucket name (e.g., snapfit-xxxxx.appspot.com): " BUCKET_NAME

# 3. Ask for JWT Secret (Optional, but recommended to set)
read -p "Enter your JWT Secret (press enter to skip): " JWT_SECRET

# 4. Generate environment export commands
echo ""
echo "--- Add these lines to your ~/.bashrc or /etc/environment ---"
echo "export FIREBASE_SERVICE_ACCOUNT_FILE=file:$KEY_PATH"
echo "export FIREBASE_STORAGE_BUCKET=$BUCKET_NAME"
if [ ! -z "$JWT_SECRET" ]; then
    echo "export JWT_SECRET=$JWT_SECRET"
fi

echo ""
echo "After adding these, run 'source ~/.bashrc' and restart your spring boot application."
echo "Command to restart: sudo systemctl restart snapfit"
