const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017';
const DB_NAME = process.env.DB_NAME || 'lilranker_auth';

// Middleware
app.use(cors());
app.use(express.json());

// MongoDB client
let db;
let keysCollection;

// Connect to MongoDB
MongoClient.connect(MONGODB_URI)
.then(client => {
    console.log('✅ Connected to MongoDB');
    db = client.db(DB_NAME);
    keysCollection = db.collection('keys');
    
    // Create index on key field for faster lookups
    keysCollection.createIndex({ key: 1 }, { unique: true });
    console.log('✅ Database and collections initialized');
})
.catch(error => {
    console.error('❌ MongoDB connection error:', error);
    process.exit(1);
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'OK', message: 'Server is running' });
});

/**
 * Validate and redeem a key
 * POST /api/validate-key
 * Body: { key: string, deviceId: string }
 */
app.post('/api/validate-key', async (req, res) => {
    try {
        const { key, deviceId } = req.body;
        
        if (!key || !deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Key and deviceId are required'
            });
        }
        
        // Find the key in database
        const keyDoc = await keysCollection.findOne({ key: key });
        
        if (!keyDoc) {
            return res.status(404).json({
                success: false,
                message: '❌ Invalid key. Key does not exist.'
            });
        }
        
        // Check if key is already used
        if (keyDoc.isUsed) {
            return res.status(403).json({
                success: false,
                message: '❌ Key already redeemed. Please use a new key.',
                data: {
                    key: keyDoc.key,
                    isUsed: keyDoc.isUsed,
                    usedBy: keyDoc.usedBy,
                    usedAt: keyDoc.usedAt
                }
            });
        }
        
        // Mark key as used
        const updateResult = await keysCollection.updateOne(
            { key: key },
            {
                $set: {
                    isUsed: true,
                    usedBy: deviceId,
                    usedAt: Date.now()
                }
            }
        );
        
        if (updateResult.modifiedCount === 0) {
            return res.status(500).json({
                success: false,
                message: 'Failed to redeem key. Please try again.'
            });
        }
        
        // Return success
        const updatedKey = await keysCollection.findOne({ key: key });
        
        return res.status(200).json({
            success: true,
            message: '✅ Key validated successfully!',
            data: {
                key: updatedKey.key,
                isUsed: updatedKey.isUsed,
                usedBy: updatedKey.usedBy,
                usedAt: updatedKey.usedAt,
                createdAt: updatedKey.createdAt
            }
        });
        
    } catch (error) {
        console.error('Error validating key:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error occurred'
        });
    }
});

/**
 * Create a new key (for admin use)
 * POST /api/create-key
 * Body: { key: string }
 */
app.post('/api/create-key', async (req, res) => {
    try {
        const { key } = req.body;
        
        if (!key) {
            return res.status(400).json({
                success: false,
                message: 'Key is required'
            });
        }
        
        // Check if key already exists
        const existing = await keysCollection.findOne({ key: key });
        if (existing) {
            return res.status(409).json({
                success: false,
                message: 'Key already exists'
            });
        }
        
        // Create new key
        const newKey = {
            key: key,
            isUsed: false,
            usedBy: null,
            usedAt: null,
            createdAt: Date.now()
        };
        
        await keysCollection.insertOne(newKey);
        
        return res.status(201).json({
            success: true,
            message: 'Key created successfully',
            data: newKey
        });
        
    } catch (error) {
        console.error('Error creating key:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error occurred'
        });
    }
});

/**
 * Get all keys (for admin use)
 * GET /api/keys
 */
app.get('/api/keys', async (req, res) => {
    try {
        const keys = await keysCollection.find({}).toArray();
        return res.status(200).json({
            success: true,
            count: keys.length,
            data: keys
        });
    } catch (error) {
        console.error('Error fetching keys:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error occurred'
        });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
    console.log(`📊 MongoDB: ${MONGODB_URI}`);
    console.log(`📁 Database: ${DB_NAME}`);
    console.log('\n📝 Available endpoints:');
    console.log('  GET  /health');
    console.log('  POST /api/validate-key');
    console.log('  POST /api/create-key');
    console.log('  GET  /api/keys');
});
