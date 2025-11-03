const { MongoClient } = require('mongodb');

const MONGODB_URI = 'mongodb://localhost:27017';
const DB_NAME = 'lilranker_auth';

// Sample keys to create
const sampleKeys = [
    'TEST-KEY-2025-001',
    'TEST-KEY-2025-002',
    'TEST-KEY-2025-003',
    'DEMO-ACCESS-001',
    'DEMO-ACCESS-002'
];

async function createSampleKeys() {
    const client = new MongoClient(MONGODB_URI);
    
    try {
        await client.connect();
        console.log('✅ Connected to MongoDB');
        
        const db = client.db(DB_NAME);
        const keysCollection = db.collection('keys');
        
        // Create index
        await keysCollection.createIndex({ key: 1 }, { unique: true });
        
        // Insert sample keys
        for (const key of sampleKeys) {
            try {
                const existingKey = await keysCollection.findOne({ key: key });
                
                if (existingKey) {
                    console.log(`⚠️  Key already exists: ${key}`);
                } else {
                    await keysCollection.insertOne({
                        key: key,
                        isUsed: false,
                        usedBy: null,
                        usedAt: null,
                        createdAt: Date.now()
                    });
                    console.log(`✅ Created key: ${key}`);
                }
            } catch (error) {
                console.error(`❌ Error creating key ${key}:`, error.message);
            }
        }
        
        // Show all keys
        const allKeys = await keysCollection.find({}).toArray();
        console.log('\n📋 All keys in database:');
        console.log('─────────────────────────────────────');
        allKeys.forEach((keyDoc, index) => {
            console.log(`${index + 1}. ${keyDoc.key} - ${keyDoc.isUsed ? '🔴 USED' : '🟢 AVAILABLE'}`);
        });
        console.log(`\nTotal keys: ${allKeys.length}`);
        
    } catch (error) {
        console.error('❌ Error:', error);
    } finally {
        await client.close();
        console.log('\n✅ Connection closed');
    }
}

createSampleKeys();
