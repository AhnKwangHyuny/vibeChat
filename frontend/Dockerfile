# Stage 1: Install dependencies and set up the workspace
FROM node:20-alpine AS base

WORKDIR /app

# Copy package.json and lock file
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Expose the Vite development server port
EXPOSE 5173

# Default command to run the development server
CMD ["npm", "run", "dev"]

# --- Production Build Stage ---
# This stage can be targeted in docker-compose for production builds
FROM base AS build

# Build the application
RUN npm run build
