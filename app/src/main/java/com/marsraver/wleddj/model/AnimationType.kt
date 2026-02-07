package com.marsraver.wleddj.model

import android.content.Context
import com.marsraver.wleddj.animations.*
import com.marsraver.wleddj.engine.Animation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
enum class AnimationType(
    val displayName: String,
    val isAudioReactive: Boolean = false,
    val animClass: KClass<out Animation>? = null, // Can be null if abstract/unknown
    private val factory: ((Context) -> Animation)? = null
) {
    @SerialName("Ball") 
    BALL("Bouncing Ball", false, BouncingBallAnimation::class, { _ -> BouncingBallAnimation() }),
    
    @SerialName("Aurora Borealis") 
    AURORA_BOREALIS("Aurora Borealis", false, AuroraBorealisAnimation::class, { _ -> AuroraBorealisAnimation() }),
    
    @SerialName("Blurz") 
    BLURZ("Blurz", true, BlurzAnimation::class, { _ -> BlurzAnimation() }),
    
    @SerialName("GEQ") 
    GEQ("GEQ", true, GeqAnimation::class, { _ -> GeqAnimation() }),
    
    @SerialName("MusicBall") 
    MUSIC_BALL("Music Ball", true, MusicBallAnimation::class, { _ -> MusicBallAnimation() }),
    
    @SerialName("Flashlight") 
    FLASHLIGHT("Flashlight", false, FlashlightAnimation::class, { _ -> FlashlightAnimation() }),
    
    @SerialName("AllSeeingEye") 
    ALL_SEEING_EYE("All Seeing Eye", false, AllSeeingEyeAnimation::class, { ctx -> AllSeeingEyeAnimation(ctx) }),
    
    @SerialName("Spectrogram") 
    SPECTROGRAM("Spectrogram", true, SpectrogramAnimation::class, { _ -> SpectrogramAnimation() }),
    
    @SerialName("Fireflies") 
    FIREFLIES("Fireflies", true, FirefliesAnimation::class, { _ -> FirefliesAnimation() }),
    
    @SerialName("TronRecognizer") 
    TRON_RECOGNIZER("Tron Recognizer", false, TronRecognizerAnimation::class, { ctx -> TronRecognizerAnimation(ctx) }),
    
    @SerialName("SpectrumTree") 
    SPECTRUM_TREE("Spectrum Tree", true, SpectrumTreeAnimation::class, { _ -> SpectrumTreeAnimation() }),
    
    @SerialName("Soap") 
    SOAP("Soap", false, SoapAnimation::class, { _ -> SoapAnimation() }),
    
    @SerialName("Akemi") 
    AKEMI("Akemi", true, AkemiAnimation::class, { _ -> AkemiAnimation() }),
    
    @SerialName("Fire 2012 2D") 
    FIRE_2012_2D("Fire 2012", false, Fire2012Animation::class, { _ -> Fire2012Animation() }),
    
    @SerialName("FireNoise2D") 
    FIRE_NOISE_2D("Fire Noise", false, FireNoiseAnimation::class, { _ -> FireNoiseAnimation() }),
    
    @SerialName("Noise2D") 
    NOISE_2D("Noise", false, NoiseAnimation::class, { _ -> NoiseAnimation() }),
    
    @SerialName("PlasmaBall2D") 
    PLASMA_BALL_2D("Plasma Ball", false, PlasmaBallAnimation::class, { _ -> PlasmaBallAnimation() }),
    
    @SerialName("Matrix") 
    MATRIX("Matrix", false, MatrixAnimation::class, { _ -> MatrixAnimation() }),
    
    @SerialName("MetaBalls") 
    META_BALLS("MetaBalls", false, MetaBallsAnimation::class, { _ -> MetaBallsAnimation() }),
    
    @SerialName("Game Of Life") 
    GAME_OF_LIFE("Game Of Life", false, GameOfLifeAnimation::class, { _ -> GameOfLifeAnimation() }),
    
    @SerialName("Julia") 
    JULIA("Julia", false, JuliaAnimation::class, { _ -> JuliaAnimation() }),
    
    @SerialName("Swirl") 
    SWIRL("Swirl", true, SwirlAnimation::class, { _ -> SwirlAnimation() }),
    
    @SerialName("Pacifica") 
    PACIFICA("Pacifica", false, PacificaAnimation::class, { _ -> PacificaAnimation() }),
    
    @SerialName("Blobs") 
    BLOBS("Blobs", false, BlobsAnimation::class, { _ -> BlobsAnimation() }),
    
    @SerialName("DistortionWaves") 
    DISTORTION_WAVES("Distortion Waves", false, DistortionWavesAnimation::class, { _ -> DistortionWavesAnimation() }),
    
    @SerialName("Plasmoid") 
    PLASMOID("Plasmoid", true, PlasmoidAnimation::class, { _ -> PlasmoidAnimation() }),
    
    @SerialName("PolarLights") 
    POLAR_LIGHTS("Polar Lights", false, PolarLightsAnimation::class, { _ -> PolarLightsAnimation() }),
    
    @SerialName("Space Ships") 
    SPACE_SHIPS("Space Ships", false, SpaceShipsAnimation::class, { _ -> SpaceShipsAnimation() }),
    
    @SerialName("SquareSwirl") 
    SQUARE_SWIRL("Square Swirl", false, SquareSwirlAnimation::class, { _ -> SquareSwirlAnimation() }),
    
    @SerialName("Puddles") 
    PUDDLES("Puddles", true, PuddlesAnimation::class, { _ -> PuddlesAnimation() }),
    
    @SerialName("Lissajous") 
    LISSAJOUS("Lissajous", false, LissajousAnimation::class, { _ -> LissajousAnimation() }),
    
    @SerialName("Tartan") 
    TARTAN("Tartan", false, TartanAnimation::class, { _ -> TartanAnimation() }),
    
    @SerialName("Waverly") 
    WAVERLY("Waverly", true, WaverlyAnimation::class, { _ -> WaverlyAnimation() }),
    
    @SerialName("CrazyBees") 
    CRAZY_BEES("Crazy Bees", false, CrazyBeesAnimation::class, { _ -> CrazyBeesAnimation() }),
    
    @SerialName("GhostRider") 
    GHOST_RIDER("Ghost Rider", false, GhostRiderAnimation::class, { _ -> GhostRiderAnimation() }),
    
    @SerialName("SunRadiation") 
    SUN_RADIATION("Sun Radiation", false, SunRadiationAnimation::class, { _ -> SunRadiationAnimation() }),
    
    @SerialName("WashingMachine") 
    WASHING_MACHINE("Washing Machine", false, WashingMachineAnimation::class, { _ -> WashingMachineAnimation() }),
    
    @SerialName("RotoZoomer") 
    ROTO_ZOOMER("Roto Zoomer", false, RotoZoomerAnimation::class, { _ -> RotoZoomerAnimation() }),
    
    @SerialName("Tetrix") 
    TETRIX("Tetrix", false, TetrixAnimation::class, { _ -> TetrixAnimation() }),
    
    @SerialName("Hiphotic") 
    HIPHOTIC("Hiphotic", false, HiphoticAnimation::class, { _ -> HiphoticAnimation() }),
    
    @SerialName("BlackHole") 
    BLACK_HOLE("Black Hole", false, BlackHoleAnimation::class, { _ -> BlackHoleAnimation() }),
    
    @SerialName("FunkyPlank") 
    FUNKY_PLANK("Funky Plank", true, FunkyPlankAnimation::class, { _ -> FunkyPlankAnimation() }),
    
    @SerialName("DriftRose") 
    DRIFT_ROSE("Drift Rose", false, DriftRoseAnimation::class, { _ -> DriftRoseAnimation() }),
    
    @SerialName("Matripix") 
    MATRIPIX("Matripix", true, MatripixAnimation::class, { _ -> MatripixAnimation() }),
    
    @SerialName("WavingCell") 
    WAVING_CELL("Waving Cell", false, WavingCellAnimation::class, { _ -> WavingCellAnimation() }),
    
    @SerialName("Frizzles") 
    FRIZZLES("Frizzles", false, FrizzlesAnimation::class, { _ -> FrizzlesAnimation() }),
    
    @SerialName("PixelWave") 
    PIXEL_WAVE("Pixel Wave", true, PixelWaveAnimation::class, { _ -> PixelWaveAnimation() }),
    
    @SerialName("FreqMatrix") 
    FREQ_MATRIX("Freq Matrix", true, FreqMatrixAnimation::class, { _ -> FreqMatrixAnimation() }),
    
    @SerialName("Lake") 
    LAKE("Lake", false, LakeAnimation::class, { _ -> LakeAnimation() }),
    
    @SerialName("DnaSpiral") 
    DNA_SPIRAL("DNA Spiral", false, DnaSpiralAnimation::class, { _ -> DnaSpiralAnimation() }),
    
    @SerialName("Globe") 
    GLOBE("Globe", false, GlobeAnimation::class, { _ -> GlobeAnimation() }),
    
    @SerialName("Fireworks") 
    FIREWORKS("Fireworks", true, FireworksAnimation::class, { _ -> FireworksAnimation() }),
    
    @SerialName("InfiniteTunnel") 
    INFINITE_TUNNEL("Infinite Tunnel", true, InfiniteTunnelAnimation::class, { _ -> InfiniteTunnelAnimation() }),
    
    @SerialName("Sonar") 
    SONAR("Sonar", true, SonarAnimation::class, { _ -> SonarAnimation() }),
    
    @SerialName("ScrollingText") 
    SCROLLING_TEXT("Scrolling Text", false, ScrollingTextAnimation::class, { _ -> ScrollingTextAnimation() }),
    
    @SerialName("Aquarium") 
    AQUARIUM("Aquarium", false, AquariumAnimation::class, { _ -> AquariumAnimation() }),
    
    @SerialName("FractalZoom") 
    FRACTAL_ZOOM("Fractal Zoom", false, FractalZoomAnimation::class, { _ -> FractalZoomAnimation() }),
    
    @SerialName("Physarum") 
    PHYSARUM("Physarum", false, PhysarumAnimation::class, { _ -> PhysarumAnimation() }),
    
    @SerialName("ReactionDiffusion") 
    REACTION_DIFFUSION("Reaction Diffusion", false, ReactionDiffusionAnimation::class, { _ -> ReactionDiffusionAnimation() }),
    
    @SerialName("Snow") 
    SNOW("Snow", false, SnowAnimation::class, { _ -> SnowAnimation() }),
    
    @SerialName("McQueen") 
    MCQUEEN("McQueen", false, McQueenAnimation::class, { ctx -> McQueenAnimation(ctx) }),
    
    @SerialName("Popcorn") 
    POPCORN("Popcorn", false, PopcornAnimation::class, { _ -> PopcornAnimation() }),
    
    @SerialName("Unknown") 
    UNKNOWN("Unknown", false, null, { _ -> BouncingBallAnimation() });

    fun create(context: Context): Animation {
        return factory?.invoke(context) ?: BouncingBallAnimation()
    }

    companion object {
        fun fromId(id: String): AnimationType {
            return entries.find { it.name.equals(id, true) } 
                ?: entries.find { 
                    // Fallback to check SerialName via reflection or manual mapping if needed
                    false
                } ?: UNKNOWN
        }
        
        fun fromInstance(animation: Animation): AnimationType {
             return entries.find { it.animClass != null && it.animClass.isInstance(animation) } 
                ?: UNKNOWN
        }
    }
}
