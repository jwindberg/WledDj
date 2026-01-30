package com.marsraver.wleddj.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AnimationType(val displayName: String, val isAudioReactive: Boolean = false) {
    @SerialName("Ball") BALL("Bouncing Ball"),
    @SerialName("Aurora Borealis") AURORA_BOREALIS("Aurora Borealis"),
    @SerialName("Blurz") BLURZ("Blurz"),
    @SerialName("GEQ") GEQ("GEQ", true),
    @SerialName("MusicBall") MUSIC_BALL("Music Ball", true),
    @SerialName("Flashlight") FLASHLIGHT("Flashlight"),
    @SerialName("AllSeeingEye") ALL_SEEING_EYE("All Seeing Eye"),
    @SerialName("Spectrogram") SPECTROGRAM("Spectrogram", true),
    @SerialName("Fireflies") FIREFLIES("Fireflies"),
    @SerialName("TronRecognizer") TRON_RECOGNIZER("Tron Recognizer"),
    @SerialName("SpectrumTree") SPECTRUM_TREE("Spectrum Tree", true),
    @SerialName("Soap") SOAP("Soap"),
    @SerialName("Akemi") AKEMI("Akemi"),
    @SerialName("Fire 2012 2D") FIRE_2012_2D("Fire 2012 2D"),
    @SerialName("FireNoise2D") FIRE_NOISE_2D("Fire Noise 2D"),
    @SerialName("Noise2D") NOISE_2D("Noise 2D"),
    @SerialName("PlasmaBall2D") PLASMA_BALL_2D("Plasma Ball 2D"),
    @SerialName("Matrix") MATRIX("Matrix"),
    @SerialName("MetaBalls") META_BALLS("MetaBalls"),
    @SerialName("Game Of Life") GAME_OF_LIFE("Game Of Life"),
    @SerialName("Julia") JULIA("Julia"),
    @SerialName("Swirl") SWIRL("Swirl"),
    @SerialName("Pacifica") PACIFICA("Pacifica"),
    @SerialName("Blobs") BLOBS("Blobs"),
    @SerialName("DistortionWaves") DISTORTION_WAVES("Distortion Waves"),
    @SerialName("Plasmoid") PLASMOID("Plasmoid"),
    @SerialName("PolarLights") POLAR_LIGHTS("Polar Lights"),
    @SerialName("Space Ships") SPACE_SHIPS("Space Ships"),
    @SerialName("SquareSwirl") SQUARE_SWIRL("Square Swirl"),
    @SerialName("Puddles") PUDDLES("Puddles"),
    @SerialName("Lissajous") LISSAJOUS("Lissajous"),
    @SerialName("Tartan") TARTAN("Tartan"),
    @SerialName("Waverly") WAVERLY("Waverly"),
    @SerialName("CrazyBees") CRAZY_BEES("Crazy Bees"),
    @SerialName("GhostRider") GHOST_RIDER("Ghost Rider"),
    @SerialName("SunRadiation") SUN_RADIATION("Sun Radiation"),
    @SerialName("WashingMachine") WASHING_MACHINE("Washing Machine"),
    @SerialName("RotoZoomer") ROTO_ZOOMER("Roto Zoomer"),
    @SerialName("Tetrix") TETRIX("Tetrix"),
    @SerialName("Hiphotic") HIPHOTIC("Hiphotic"),
    @SerialName("BlackHole") BLACK_HOLE("Black Hole"),
    @SerialName("FunkyPlank") FUNKY_PLANK("Funky Plank"),
    @SerialName("DriftRose") DRIFT_ROSE("Drift Rose"),
    @SerialName("Matripix") MATRIPIX("Matripix"),
    @SerialName("WavingCell") WAVING_CELL("Waving Cell"),
    @SerialName("Frizzles") FRIZZLES("Frizzles"),
    @SerialName("PixelWave") PIXEL_WAVE("Pixel Wave"),
    @SerialName("FreqMatrix") FREQ_MATRIX("Freq Matrix"),
    @SerialName("Lake") LAKE("Lake"),
    @SerialName("DnaSpiral") DNA_SPIRAL("DNA Spiral"),
    @SerialName("Globe") GLOBE("Globe"),
    @SerialName("Fireworks") FIREWORKS("Fireworks", true),
    @SerialName("InfiniteTunnel") INFINITE_TUNNEL("Infinite Tunnel", true),
    @SerialName("Sonar") SONAR("Sonar", true),
    @SerialName("ScrollingText") SCROLLING_TEXT("Scrolling Text"),
    @SerialName("Aquarium") AQUARIUM("Aquarium"),
    @SerialName("FractalZoom") FRACTAL_ZOOM("Fractal Zoom"),
    @SerialName("Physarum") PHYSARUM("Physarum"),
    @SerialName("ReactionDiffusion") REACTION_DIFFUSION("Reaction Diffusion"),
    @SerialName("Snow") SNOW("Snow"),
    @SerialName("McQueen") MCQUEEN("McQueen"),
    @SerialName("Popcorn") POPCORN("Popcorn"),
    
    @SerialName("Unknown") UNKNOWN("Unknown");

    companion object {
        fun fromId(id: String): AnimationType {
            return entries.find { it.name.equals(id, true) } 
                ?: entries.find { 
                    // Fallback to check SerialName via reflection or manual mapping if needed, 
                    // but simple string matching might fail due to spaces.
                    // For now, let's rely on basic values or add a map.
                    false
                } ?: UNKNOWN
        }
    }
}
