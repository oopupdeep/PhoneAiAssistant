package com.wang.phoneaiassistant.data.embeddings

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

@Singleton
class EmbeddingService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val EMBEDDING_DIM = 384 // Common embedding dimension
        private const val MAX_SEQUENCE_LENGTH = 512
        private const val VOCAB_SIZE = 10000 // Approximate vocabulary size for hashing
        private const val CHAR_NGRAM_SIZE = 3
        private const val WORD_NGRAM_SIZE = 2
        
        // Common stop words in multiple languages
        private val STOP_WORDS = setOf(
            // English
            "the", "is", "at", "which", "on", "a", "an", "and", "or", "but", "in", "with", "to", "for", "of", "as", "by", "from", "up", "out", "if", "then", "than", "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they", "what", "when", "where", "why", "how", "can", "could", "would", "should", "will", "be", "been", "being", "have", "has", "had", "do", "does", "did", "are", "was", "were", "am",
            // Chinese common particles
            "的", "了", "是", "在", "和", "有", "我", "你", "他", "她", "它", "我们", "你们", "他们", "这", "那", "就", "也", "都", "吗", "呢", "吧", "啊", "嗯", "哦"
        )
        
        // Semantic categories for enhanced embeddings
        private val QUESTION_WORDS = setOf("what", "when", "where", "why", "how", "who", "which", "whose", "whom", "什么", "怎么", "为什么", "哪里", "谁", "哪个", "何时", "如何")
        private val COMMAND_WORDS = setOf("please", "can", "could", "would", "should", "must", "need", "want", "help", "show", "tell", "explain", "create", "make", "build", "请", "能", "可以", "需要", "帮助", "告诉", "解释", "创建", "制作")
        private val EMOTION_WORDS = setOf("happy", "sad", "angry", "excited", "worried", "love", "hate", "like", "dislike", "开心", "难过", "生气", "兴奋", "担心", "喜欢", "讨厌", "爱", "恨")
    }
    
    // Cache for document frequency
    private val documentFrequencyCache = mutableMapOf<String, Int>()
    private var totalDocuments = 0
    
    suspend fun generateEmbedding(text: String): FloatArray {
        return generateAdvancedEmbedding(text)
    }
    
    private fun generateAdvancedEmbedding(text: String): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIM) { 0f }
        
        // Preprocess text
        val cleanText = preprocessText(text)
        val tokens = tokenize(cleanText)
        val words = cleanText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        // 1. TF-IDF based features (first 150 dimensions)
        val tfidfFeatures = calculateTFIDFFeatures(tokens, 150)
        System.arraycopy(tfidfFeatures, 0, embedding, 0, tfidfFeatures.size)
        
        // 2. Character n-gram features (next 100 dimensions)
        val charNgramFeatures = calculateCharNgramFeatures(cleanText, 100)
        System.arraycopy(charNgramFeatures, 0, embedding, 150, charNgramFeatures.size)
        
        // 3. Word n-gram features (next 80 dimensions)
        val wordNgramFeatures = calculateWordNgramFeatures(words, 80)
        System.arraycopy(wordNgramFeatures, 0, embedding, 250, wordNgramFeatures.size)
        
        // 4. Semantic and structural features (next 30 dimensions)
        val semanticFeatures = calculateSemanticFeatures(text, words)
        System.arraycopy(semanticFeatures, 0, embedding, 330, semanticFeatures.size)
        
        // 5. Statistical features (last 24 dimensions)
        val statisticalFeatures = calculateStatisticalFeatures(text, words)
        System.arraycopy(statisticalFeatures, 0, embedding, 360, statisticalFeatures.size)
        
        // Normalize the embedding
        return normalizeEmbedding(embedding)
    }
    
    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5\\s]"), " ") // Keep alphanumeric and Chinese characters
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun tokenize(text: String): List<String> {
        // Simple tokenization that handles both English and Chinese
        val tokens = mutableListOf<String>()
        val words = text.split(Regex("\\s+"))
        
        for (word in words) {
            if (word.isEmpty()) continue
            
            // For Chinese characters, treat each character as a token
            if (word.contains(Regex("[\\u4e00-\\u9fa5]"))) {
                tokens.addAll(word.toCharArray().map { it.toString() })
            } else {
                tokens.add(word)
            }
        }
        
        return tokens
    }
    
    private fun calculateTFIDFFeatures(tokens: List<String>, dimensions: Int): FloatArray {
        val features = FloatArray(dimensions)
        val tokenFrequency = tokens.groupingBy { it }.eachCount()
        val totalTokens = tokens.size.toFloat()
        
        // Calculate TF-IDF scores
        val tfidfScores = mutableMapOf<String, Float>()
        for ((token, freq) in tokenFrequency) {
            if (token in STOP_WORDS) continue
            
            val tf = freq / totalTokens
            val idf = calculateIDF(token)
            tfidfScores[token] = tf * idf
        }
        
        // Hash tokens into fixed dimensions
        for ((token, score) in tfidfScores) {
            val hash = token.hashCode()
            // Use multiple hash functions to reduce collisions
            for (i in 0..2) {
                val index = ((hash + i * 31) % dimensions + dimensions) % dimensions
                features[index] += score
            }
        }
        
        return features
    }
    
    private fun calculateIDF(token: String): Float {
        // Simplified IDF calculation with pseudo-counts
        val docFreq = documentFrequencyCache.getOrDefault(token, 1)
        val totalDocs = max(totalDocuments, 1000) // Assume minimum 1000 documents
        return ln((totalDocs + 1).toFloat() / (docFreq + 1).toFloat())
    }
    
    private fun calculateCharNgramFeatures(text: String, dimensions: Int): FloatArray {
        val features = FloatArray(dimensions)
        val ngrams = mutableMapOf<String, Int>()
        
        // Extract character n-grams
        for (i in 0 until text.length - CHAR_NGRAM_SIZE + 1) {
            val ngram = text.substring(i, i + CHAR_NGRAM_SIZE)
            ngrams[ngram] = ngrams.getOrDefault(ngram, 0) + 1
        }
        
        // Hash n-grams into features
        for ((ngram, count) in ngrams) {
            val hash = ngram.hashCode()
            val index = (hash % dimensions + dimensions) % dimensions
            features[index] += count.toFloat() / text.length
        }
        
        return features
    }
    
    private fun calculateWordNgramFeatures(words: List<String>, dimensions: Int): FloatArray {
        val features = FloatArray(dimensions)
        
        // Extract word bigrams
        for (i in 0 until words.size - 1) {
            val bigram = "${words[i]}_${words[i + 1]}"
            val hash = bigram.hashCode()
            val index = (hash % dimensions + dimensions) % dimensions
            features[index] += 1f / words.size
        }
        
        return features
    }
    
    private fun calculateSemanticFeatures(text: String, words: List<String>): FloatArray {
        val features = FloatArray(30)
        val lowerText = text.lowercase()
        val wordSet = words.toSet()
        
        // Feature 0-4: Text type indicators
        features[0] = if (words.any { it in QUESTION_WORDS }) 1f else 0f // Is question
        features[1] = if (words.any { it in COMMAND_WORDS }) 1f else 0f // Is command
        features[2] = if (words.any { it in EMOTION_WORDS }) 1f else 0f // Has emotion
        features[3] = if (lowerText.contains("http") || lowerText.contains("www")) 1f else 0f // Has URL
        features[4] = if (Regex("[0-9]+").containsMatchIn(text)) 1f else 0f // Has numbers
        
        // Feature 5-9: Punctuation features
        features[5] = text.count { it == '?' }.toFloat() / max(1, words.size)
        features[6] = text.count { it == '!' }.toFloat() / max(1, words.size)
        features[7] = text.count { it == '.' }.toFloat() / max(1, words.size)
        features[8] = text.count { it == ',' }.toFloat() / max(1, words.size)
        features[9] = if (text.contains("...")) 1f else 0f
        
        // Feature 10-14: Language features
        val chineseChars = text.count { it in '\u4e00'..'\u9fa5' }
        val englishChars = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        features[10] = chineseChars.toFloat() / max(1, text.length)
        features[11] = englishChars.toFloat() / max(1, text.length)
        features[12] = if (chineseChars > englishChars) 1f else 0f // Is primarily Chinese
        features[13] = words.count { it.length > 10 }.toFloat() / max(1, words.size) // Long words ratio
        features[14] = words.count { it.all { char -> char.isUpperCase() } }.toFloat() / max(1, words.size) // All caps ratio
        
        // Feature 15-19: Topic indicators
        features[15] = if (wordSet.any { it in setOf("code", "function", "class", "variable", "程序", "代码", "函数", "类") }) 1f else 0f
        features[16] = if (wordSet.any { it in setOf("time", "date", "today", "tomorrow", "时间", "日期", "今天", "明天") }) 1f else 0f
        features[17] = if (wordSet.any { it in setOf("weather", "temperature", "rain", "天气", "温度", "雨") }) 1f else 0f
        features[18] = if (wordSet.any { it in setOf("food", "eat", "drink", "meal", "食物", "吃", "喝", "餐") }) 1f else 0f
        features[19] = if (wordSet.any { it in setOf("money", "price", "cost", "pay", "钱", "价格", "费用", "支付") }) 1f else 0f
        
        // Feature 20-29: Reserved for future semantic features
        // Can be extended with more domain-specific features
        
        return features
    }
    
    private fun calculateStatisticalFeatures(text: String, words: List<String>): FloatArray {
        val features = FloatArray(24)
        
        // Basic statistics
        features[0] = min(1f, text.length.toFloat() / 1000f) // Normalized text length
        features[1] = min(1f, words.size.toFloat() / 100f) // Normalized word count
        features[2] = if (words.isNotEmpty()) text.length.toFloat() / words.size else 0f // Avg word length
        features[3] = words.toSet().size.toFloat() / max(1, words.size) // Vocabulary richness
        
        // Sentence statistics
        val sentences = text.split(Regex("[.!?。！？]")).filter { it.isNotBlank() }
        features[4] = min(1f, sentences.size.toFloat() / 10f) // Normalized sentence count
        features[5] = if (sentences.isNotEmpty()) words.size.toFloat() / sentences.size else 0f // Avg sentence length
        
        // Character distribution
        val charCounts = text.groupingBy { it }.eachCount()
        val totalChars = text.length.toFloat()
        features[6] = charCounts.size.toFloat() / max(1f, totalChars) // Character diversity
        
        // Stop word ratio
        features[7] = words.count { it in STOP_WORDS }.toFloat() / max(1, words.size)
        
        // Case features
        features[8] = text.count { it.isUpperCase() }.toFloat() / max(1, text.length)
        features[9] = text.count { it.isLowerCase() }.toFloat() / max(1, text.length)
        
        // Digit features
        features[10] = text.count { it.isDigit() }.toFloat() / max(1, text.length)
        
        // Whitespace features
        features[11] = text.count { it.isWhitespace() }.toFloat() / max(1, text.length)
        
        // N-gram diversity
        val bigrams = mutableSetOf<String>()
        for (i in 0 until words.size - 1) {
            bigrams.add("${words[i]}_${words[i + 1]}")
        }
        features[12] = bigrams.size.toFloat() / max(1, words.size - 1)
        
        // Word length distribution
        val shortWords = words.count { it.length <= 3 }
        val mediumWords = words.count { it.length in 4..7 }
        val longWords = words.count { it.length > 7 }
        features[13] = shortWords.toFloat() / max(1, words.size)
        features[14] = mediumWords.toFloat() / max(1, words.size)
        features[15] = longWords.toFloat() / max(1, words.size)
        
        // Special character ratio
        features[16] = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }.toFloat() / max(1, text.length)
        
        // Parentheses and brackets
        features[17] = (text.count { it == '(' || it == ')' }).toFloat() / max(1, text.length)
        features[18] = (text.count { it == '[' || it == ']' }).toFloat() / max(1, text.length)
        features[19] = (text.count { it == '{' || it == '}' }).toFloat() / max(1, text.length)
        
        // Quote features
        features[20] = (text.count { it == '"' || it == '\'' }).toFloat() / max(1, text.length)
        
        // Emoji/special unicode detection (simplified)
        features[21] = text.count { it.code > 127 && it !in '\u4e00'..'\u9fa5' }.toFloat() / max(1, text.length)
        
        // Repeated character detection
        var repeatedChars = 0
        for (i in 1 until text.length) {
            if (text[i] == text[i - 1]) repeatedChars++
        }
        features[22] = repeatedChars.toFloat() / max(1, text.length - 1)
        
        // Text entropy (simplified)
        var entropy = 0f
        for ((_, count) in charCounts) {
            val p = count.toFloat() / totalChars
            if (p > 0) entropy -= p * ln(p)
        }
        features[23] = min(1f, entropy / 5f) // Normalized entropy
        
        return features
    }
    
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        // L2 normalization
        val norm = sqrt(embedding.map { it * it }.sum())
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        return embedding
    }
    
    fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) { "Embeddings must have the same dimension" }
        
        // Since embeddings are normalized, dot product equals cosine similarity
        var dotProduct = 0.0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        
        // Ensure result is in [-1, 1] range
        return max(-1f, min(1f, dotProduct))
    }
    
    fun updateDocumentFrequency(tokens: List<String>) {
        // Update document frequency cache for better IDF calculation
        totalDocuments++
        val uniqueTokens = tokens.toSet()
        for (token in uniqueTokens) {
            documentFrequencyCache[token] = documentFrequencyCache.getOrDefault(token, 0) + 1
        }
        
        // Limit cache size to prevent memory issues
        if (documentFrequencyCache.size > VOCAB_SIZE * 2) {
            // Keep only the most frequent tokens
            val sortedTokens = documentFrequencyCache.entries.sortedByDescending { it.value }
            documentFrequencyCache.clear()
            sortedTokens.take(VOCAB_SIZE).forEach { (token, freq) ->
                documentFrequencyCache[token] = freq
            }
        }
    }
    
    fun cleanup() {
        documentFrequencyCache.clear()
        totalDocuments = 0
    }
}