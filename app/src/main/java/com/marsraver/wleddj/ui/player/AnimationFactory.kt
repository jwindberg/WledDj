package com.marsraver.wleddj.ui.player

import android.content.Context
import com.marsraver.wleddj.animations.*
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.model.AnimationRegion
import com.marsraver.wleddj.model.SavedAnimation
import com.marsraver.wleddj.model.AnimationType
import com.marsraver.wleddj.engine.color.Palette

object AnimationFactory {

    fun createAnimation(type: AnimationType, context: Context, dropX: Float = 0f, dropY: Float = 0f): Animation {
        return when(type) {
            AnimationType.BALL -> BouncingBallAnimation(dropX, dropY, 30f)
            AnimationType.AURORA_BOREALIS -> AuroraBorealisAnimation()
            AnimationType.BLURZ -> BlurzAnimation()
            AnimationType.GEQ -> GeqAnimation()
            AnimationType.MUSIC_BALL -> MusicBallAnimation()
            AnimationType.FLASHLIGHT -> FlashlightAnimation()
            AnimationType.ALL_SEEING_EYE -> AllSeeingEyeAnimation(context)
            AnimationType.SPECTROGRAM -> SpectrogramAnimation()
            AnimationType.FIREFLIES -> FirefliesAnimation()
            AnimationType.TRON_RECOGNIZER -> TronRecognizerAnimation(context)
            AnimationType.SPECTRUM_TREE -> SpectrumTreeAnimation()
            AnimationType.SOAP -> SoapAnimation()
            AnimationType.AKEMI -> AkemiAnimation()
            AnimationType.FIRE_2012_2D -> Fire2012_2DAnimation()
            AnimationType.FIRE_NOISE_2D -> FireNoise2DAnimation()
            AnimationType.NOISE_2D -> Noise2DAnimation()
            AnimationType.PLASMA_BALL_2D -> PlasmaBall2DAnimation()
            AnimationType.MATRIX -> MatrixAnimation()
            AnimationType.META_BALLS -> MetaBallsAnimation()
            AnimationType.GAME_OF_LIFE -> GameOfLifeAnimation()
            AnimationType.JULIA -> JuliaAnimation()
            AnimationType.SWIRL -> SwirlAnimation()
            AnimationType.PACIFICA -> PacificaAnimation()
            AnimationType.BLOBS -> BlobsAnimation()
            AnimationType.DISTORTION_WAVES -> DistortionWavesAnimation()
            AnimationType.PLASMOID -> PlasmoidAnimation()
            AnimationType.POLAR_LIGHTS -> PolarLightsAnimation()
            AnimationType.SPACE_SHIPS -> SpaceShipsAnimation()
            AnimationType.SQUARE_SWIRL -> SquareSwirlAnimation()
            AnimationType.PUDDLES -> PuddlesAnimation()
            AnimationType.LISSAJOUS -> LissajousAnimation()
            AnimationType.TARTAN -> TartanAnimation()
            AnimationType.WAVERLY -> WaverlyAnimation()
            AnimationType.CRAZY_BEES -> CrazyBeesAnimation()
            AnimationType.GHOST_RIDER -> GhostRiderAnimation()
            AnimationType.SUN_RADIATION -> SunRadiationAnimation()
            AnimationType.WASHING_MACHINE -> WashingMachineAnimation()
            AnimationType.ROTO_ZOOMER -> RotoZoomerAnimation()
            AnimationType.TETRIX -> TetrixAnimation()
            AnimationType.HIPHOTIC -> HiphoticAnimation()
            AnimationType.BLACK_HOLE -> BlackHoleAnimation()
            AnimationType.FUNKY_PLANK -> FunkyPlankAnimation()
            AnimationType.DRIFT_ROSE -> DriftRoseAnimation()
            AnimationType.MATRIPIX -> MatripixAnimation()
            AnimationType.WAVING_CELL -> WavingCellAnimation()
            AnimationType.FRIZZLES -> FrizzlesAnimation()
            AnimationType.PIXEL_WAVE -> PixelWaveAnimation()
            AnimationType.FREQ_MATRIX -> FreqMatrixAnimation()
            AnimationType.LAKE -> LakeAnimation()
            AnimationType.DNA_SPIRAL -> DnaSpiralAnimation()
            AnimationType.GLOBE -> GlobeAnimation()
            AnimationType.FIREWORKS -> FireworksAnimation()
            AnimationType.INFINITE_TUNNEL -> InfiniteTunnelAnimation()
            AnimationType.SONAR -> SonarAnimation()
            AnimationType.SCROLLING_TEXT -> ScrollingTextAnimation()
            AnimationType.AQUARIUM -> AquariumAnimation()
            AnimationType.FRACTAL_ZOOM -> FractalZoomAnimation()
            AnimationType.PHYSARUM -> PhysarumAnimation()
            AnimationType.REACTION_DIFFUSION -> ReactionDiffusionAnimation()
            AnimationType.SNOW -> SnowAnimation()
            AnimationType.POPCORN -> PopcornAnimation()
            AnimationType.MCQUEEN -> McQueenAnimation(context)
            
            AnimationType.UNKNOWN -> BouncingBallAnimation(dropX, dropY, 30f)
        }
    }

    fun createSavedAnimation(region: AnimationRegion): SavedAnimation {
        val type = when(region.animation) {
            is GlobeAnimation -> AnimationType.GLOBE
            is BouncingBallAnimation -> AnimationType.BALL
            is FireworksAnimation -> AnimationType.FIREWORKS
            is InfiniteTunnelAnimation -> AnimationType.INFINITE_TUNNEL
            is FractalZoomAnimation -> AnimationType.FRACTAL_ZOOM
            is PhysarumAnimation -> AnimationType.PHYSARUM
            is ReactionDiffusionAnimation -> AnimationType.REACTION_DIFFUSION
            is SonarAnimation -> AnimationType.SONAR
            is ScrollingTextAnimation -> AnimationType.SCROLLING_TEXT
            is AquariumAnimation -> AnimationType.AQUARIUM
            is AuroraBorealisAnimation -> AnimationType.AURORA_BOREALIS
            is BlurzAnimation -> AnimationType.BLURZ
            is GeqAnimation -> AnimationType.GEQ
            is SpectrogramAnimation -> AnimationType.SPECTROGRAM
            is FlashlightAnimation -> AnimationType.FLASHLIGHT
            is MusicBallAnimation -> AnimationType.MUSIC_BALL
            is AllSeeingEyeAnimation -> AnimationType.ALL_SEEING_EYE
            is FirefliesAnimation -> AnimationType.FIREFLIES
            is TronRecognizerAnimation -> AnimationType.TRON_RECOGNIZER
            is SpectrumTreeAnimation -> AnimationType.SPECTRUM_TREE
            is SoapAnimation -> AnimationType.SOAP
            is AkemiAnimation -> AnimationType.AKEMI
            is Fire2012_2DAnimation -> AnimationType.FIRE_2012_2D
            is FireNoise2DAnimation -> AnimationType.FIRE_NOISE_2D
            is Noise2DAnimation -> AnimationType.NOISE_2D
            is PlasmaBall2DAnimation -> AnimationType.PLASMA_BALL_2D
            is MatrixAnimation -> AnimationType.MATRIX
            is MetaBallsAnimation -> AnimationType.META_BALLS
            is GameOfLifeAnimation -> AnimationType.GAME_OF_LIFE
            is JuliaAnimation -> AnimationType.JULIA
            is SwirlAnimation -> AnimationType.SWIRL
            is PacificaAnimation -> AnimationType.PACIFICA
            is BlobsAnimation -> AnimationType.BLOBS
            is DistortionWavesAnimation -> AnimationType.DISTORTION_WAVES
            is PlasmoidAnimation -> AnimationType.PLASMOID
            is PolarLightsAnimation -> AnimationType.POLAR_LIGHTS
            is SpaceShipsAnimation -> AnimationType.SPACE_SHIPS
            is SquareSwirlAnimation -> AnimationType.SQUARE_SWIRL
            is PuddlesAnimation -> AnimationType.PUDDLES
            is LissajousAnimation -> AnimationType.LISSAJOUS
            is TartanAnimation -> AnimationType.TARTAN
            is WaverlyAnimation -> AnimationType.WAVERLY
            is CrazyBeesAnimation -> AnimationType.CRAZY_BEES
            is GhostRiderAnimation -> AnimationType.GHOST_RIDER
            is SunRadiationAnimation -> AnimationType.SUN_RADIATION
            is WashingMachineAnimation -> AnimationType.WASHING_MACHINE
            is RotoZoomerAnimation -> AnimationType.ROTO_ZOOMER
            is TetrixAnimation -> AnimationType.TETRIX
            is HiphoticAnimation -> AnimationType.HIPHOTIC
            is BlackHoleAnimation -> AnimationType.BLACK_HOLE
            is FunkyPlankAnimation -> AnimationType.FUNKY_PLANK
            is DriftRoseAnimation -> AnimationType.DRIFT_ROSE
            is MatripixAnimation -> AnimationType.MATRIPIX
            is WavingCellAnimation -> AnimationType.WAVING_CELL
            is FrizzlesAnimation -> AnimationType.FRIZZLES
            is PixelWaveAnimation -> AnimationType.PIXEL_WAVE
            is FreqMatrixAnimation -> AnimationType.FREQ_MATRIX
            is LakeAnimation -> AnimationType.LAKE
            is PopcornAnimation -> AnimationType.POPCORN
            is DnaSpiralAnimation -> AnimationType.DNA_SPIRAL
            else -> AnimationType.BALL
        }
        
        // Palette is now an Enum, so we can directly access it from the animation
        val palette = region.animation.currentPalette

        return SavedAnimation(
            id = region.id,
            type = type,
            rectLeft = region.rect.left,
            rectTop = region.rect.top,
            rectRight = region.rect.right,
            rectBottom = region.rect.bottom,
            rotation = region.rotation,
            text = if (region.animation.supportsText()) region.animation.getText() else null,
            primaryColor = if (region.animation.supportsPrimaryColor()) region.animation.primaryColor else null,
            secondaryColor = if (region.animation.supportsSecondaryColor()) region.animation.secondaryColor else null,
            palette = palette
        )
    }

    fun getAvailableAnimations(): List<AnimationMetadata> {
        return AnimationType.entries.filter { it != AnimationType.UNKNOWN }.sortedBy { it.displayName }.map { type ->
            AnimationMetadata(type, type.isAudioReactive)
        }
    }

    data class AnimationMetadata(val type: AnimationType, val isAudioReactive: Boolean) {
        val name get() = type.displayName
    }
}
