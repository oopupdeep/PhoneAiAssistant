package com.wang.phoneaiassistant.data.embeddings

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import org.junit.Assert.*

class EmbeddingServiceTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var embeddingService: EmbeddingService
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        embeddingService = EmbeddingService(mockContext)
    }
    
    @Test
    fun `generateEmbedding produces consistent results for same input`() = runBlocking {
        val text = "Hello, this is a test message"
        val embedding1 = embeddingService.generateEmbedding(text)
        val embedding2 = embeddingService.generateEmbedding(text)
        
        assertEquals(384, embedding1.size)
        assertEquals(384, embedding2.size)
        
        // Embeddings should be identical for same input
        for (i in embedding1.indices) {
            assertEquals(embedding1[i], embedding2[i], 0.001f)
        }
    }
    
    @Test
    fun `generateEmbedding produces normalized embeddings`() = runBlocking {
        val text = "This is another test message"
        val embedding = embeddingService.generateEmbedding(text)
        
        // Calculate L2 norm
        val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        
        // Normalized embeddings should have norm close to 1
        assertEquals(1.0f, norm, 0.01f)
    }
    
    @Test
    fun `similar texts produce similar embeddings`() = runBlocking {
        val text1 = "I love programming in Kotlin"
        val text2 = "I enjoy coding in Kotlin"
        val text3 = "The weather is nice today"
        
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)
        val embedding3 = embeddingService.generateEmbedding(text3)
        
        val similarity12 = embeddingService.calculateCosineSimilarity(embedding1, embedding2)
        val similarity13 = embeddingService.calculateCosineSimilarity(embedding1, embedding3)
        
        // Similar texts should have higher similarity
        assertTrue("Similar texts should have higher similarity", similarity12 > similarity13)
        assertTrue("Similarity should be positive for related texts", similarity12 > 0.3f)
    }
    
    @Test
    fun `question texts are detected properly`() = runBlocking {
        val questions = listOf(
            "What is the weather today?",
            "How can I help you?",
            "Where are you going?",
            "什么时候开始？",
            "为什么这样做？"
        )
        
        val statements = listOf(
            "The weather is nice today.",
            "I can help you.",
            "I am going home.",
            "现在开始。",
            "因为需要这样做。"
        )
        
        for (question in questions) {
            val embedding = embeddingService.generateEmbedding(question)
            // Check semantic features - questions should have feature[0] = 1
            // Feature 0 is at index 330 in the full embedding
            assertTrue("Question should be detected: $question", embedding[330] > 0.9f)
        }
        
        for (statement in statements) {
            val embedding = embeddingService.generateEmbedding(statement)
            // Statements should have feature[0] = 0
            assertTrue("Statement should not be detected as question: $statement", embedding[330] < 0.1f)
        }
    }
    
    @Test
    fun `chinese and english texts are distinguished`() = runBlocking {
        val chineseText = "你好，今天天气怎么样？"
        val englishText = "Hello, how is the weather today?"
        val mixedText = "Hello 你好 weather 天气"
        
        val chineseEmbedding = embeddingService.generateEmbedding(chineseText)
        val englishEmbedding = embeddingService.generateEmbedding(englishText)
        val mixedEmbedding = embeddingService.generateEmbedding(mixedText)
        
        // Check language features - index 342 indicates if primarily Chinese
        assertTrue("Chinese text should be detected", chineseEmbedding[342] > 0.9f)
        assertTrue("English text should not be detected as Chinese", englishEmbedding[342] < 0.1f)
        
        // Mixed text might have intermediate value
        assertTrue("Mixed text should have intermediate value", 
            mixedEmbedding[342] > 0.2f && mixedEmbedding[342] < 0.8f)
    }
    
    @Test
    fun `cosine similarity is bounded between -1 and 1`() = runBlocking {
        val texts = listOf(
            "Hello world",
            "你好世界",
            "Programming is fun",
            "编程很有趣",
            "The quick brown fox jumps over the lazy dog",
            "1234567890",
            "!@#$%^&*()"
        )
        
        val embeddings = texts.map { embeddingService.generateEmbedding(it) }
        
        for (i in embeddings.indices) {
            for (j in embeddings.indices) {
                val similarity = embeddingService.calculateCosineSimilarity(embeddings[i], embeddings[j])
                assertTrue("Similarity should be >= -1", similarity >= -1f)
                assertTrue("Similarity should be <= 1", similarity <= 1f)
                
                if (i == j) {
                    // Same embedding should have similarity close to 1
                    assertTrue("Same embedding should have similarity ~1", abs(similarity - 1f) < 0.01f)
                }
            }
        }
    }
    
    @Test
    fun `document frequency update works correctly`() = runBlocking {
        // Clean up first
        embeddingService.cleanup()
        
        // Add some documents
        val docs = listOf(
            listOf("hello", "world", "test"),
            listOf("hello", "kotlin", "test"),
            listOf("world", "programming", "test")
        )
        
        for (doc in docs) {
            embeddingService.updateDocumentFrequency(doc)
        }
        
        // Generate embeddings - should use updated IDF values
        val embedding1 = embeddingService.generateEmbedding("hello test")
        val embedding2 = embeddingService.generateEmbedding("kotlin programming")
        
        // These should produce different embeddings due to different IDF weights
        var difference = 0f
        for (i in embedding1.indices) {
            difference += abs(embedding1[i] - embedding2[i])
        }
        
        assertTrue("Different texts should produce different embeddings", difference > 0.1f)
    }
    
    @Test
    fun `empty text produces valid embedding`() = runBlocking {
        val embedding = embeddingService.generateEmbedding("")
        
        assertEquals(384, embedding.size)
        // Should still be normalized
        val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        assertTrue("Empty text should produce zero or normalized embedding", norm < 0.01f || abs(norm - 1f) < 0.01f)
    }
    
    @Test
    fun `very long text is handled correctly`() = runBlocking {
        val longText = "This is a very long text. ".repeat(100)
        val embedding = embeddingService.generateEmbedding(longText)
        
        assertEquals(384, embedding.size)
        // Should be normalized
        val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        assertEquals(1.0f, norm, 0.01f)
    }
    
    @Test
    fun `special characters are handled correctly`() = runBlocking {
        val specialText = "Hello! @#$%^&*() How are you? 你好！😊"
        val embedding = embeddingService.generateEmbedding(specialText)
        
        assertEquals(384, embedding.size)
        // Should be normalized
        val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble()).toFloat()
        assertEquals(1.0f, norm, 0.01f)
    }
}