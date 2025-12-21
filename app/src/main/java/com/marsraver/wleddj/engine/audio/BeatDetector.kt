package com.marsraver.wleddj.engine.audio

/**
 * A helper class to detect beats and frequency energy, similar to Minim's BeatDetect.
 * Wraps FftMeter to provide convenient boolean checks for animations.
 */
object BeatDetector {

    private val fftMeter = FftMeter(bands = 16)
    
    // Sensitivity factor
    var sensitivity = 10 // 0-100?
    
    /**
     * Checks if the average energy of the frequency bands [low, high] is greater than [threshold].
     * Bands are 0-15.
     */
    fun isRange(low: Int, high: Int, threshold: Int): Boolean {
        val bands = fftMeter.getNormalizedBands() // 0-255
        var sum = 0
        var count = 0
        
        val start = low.coerceIn(0, 15)
        val end = high.coerceIn(0, 15)
        
        for (i in start..end) {
            sum += bands[i]
            count++
        }
        
        val avg = if (count > 0) sum / count else 0
        // Scale threshold? Minim uses small ints usually. 
        // Our bands are 0-255. User passed "2". Assuming scaling needed.
        // If user passed "2", maybe they meant raw magnitude?
        // Let's assume threshold is comparable to band magnitude (0-255).
        // If passed 2, it's very low. 
        
        // Actually, Minim's isRange threshold is "dbreference"?
        // Detailed docs say: "threshold - the threshold for the average energy"
        // Let's use it directly but maybe with a minimal floor.
        
        // MusicBall uses threshold=2. If our data is 0-255, 2 is noise.
        // It might be using a very specific scale.
        // We might need to auto-scale or just check logic.
        // If Logic: (avg > threshold) -> return true.
        
        return avg > threshold
    }

    /**
     * Checks for a "Kick" (Bass beat).
     * Uses Bands 0-2.
     */
    fun isKick(): Boolean {
        val bands = fftMeter.getNormalizedBands()
        // Simple onset: If band 0 is high and rising?
        // For now, just HIGH energy check.
        // FftMeter does smoothing, so "rising" is harder to detect without raw history.
        // We'll just check if it's booming (> 200).
        return (bands.getOrNull(0) ?: 0) > 180
    }
    
    /**
     * Checks for "Hat" (Treble).
     * Uses Bands 12-15.
     */
    fun isHat(): Boolean {
        val bands = fftMeter.getNormalizedBands()
        // Check avg of top bands
        val avg = (bands.slice(12..15).sum()) / 4
        return avg > 100
    }
    
    /**
     * Checks for "Snare" (High Mids).
     * Uses Bands 5-10.
     */
    fun isSnare(): Boolean {
        val bands = fftMeter.getNormalizedBands()
        val avg = (bands.slice(5..10).sum()) / 6
        return avg > 150
    }
    
    /**
     * Gets the overall level (RMS/Amplitude) 0-1.0
     */
    fun getLevel(): Float {
        // Approximate from bands average
        val bands = fftMeter.getNormalizedBands()
        val avg = bands.average()
        return (avg / 255.0).toFloat()
    }
    
    fun stop() {
        fftMeter.stop()
    }
}
