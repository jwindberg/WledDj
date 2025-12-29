package com.marsraver.wleddj.ui.player

import android.content.Context
import com.marsraver.wleddj.animations.*
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.model.AnimationRegion
import com.marsraver.wleddj.model.SavedAnimation

object AnimationFactory {

    fun createAnimation(type: String, context: Context, dropX: Float = 0f, dropY: Float = 0f): Animation {
        return when(type) {
            "Ball" -> BouncingBallAnimation(dropX, dropY, 30f)
            "Aurora Borealis" -> AuroraBorealisAnimation()
            "Blurz" -> BlurzAnimation()
            "GEQ" -> GeqAnimation()
            "MusicBall" -> MusicBallAnimation()
            "Flashlight" -> FlashlightAnimation()
            "DeathStarRun" -> DeathStarAnimation(context)
            "Spectrogram" -> SpectrogramAnimation()
            "Fireflies" -> FirefliesAnimation()
            "TronRecognizer" -> TronRecognizerAnimation(context)
            "SpectrumTree" -> SpectrumTreeAnimation()
            "Soap" -> SoapAnimation()
            "Akemi" -> AkemiAnimation()
            "Fire 2012 2D" -> Fire2012_2DAnimation()
            "FireNoise2D" -> FireNoise2DAnimation()
            "Noise2D" -> Noise2DAnimation()
            "PlasmaBall2D" -> PlasmaBall2DAnimation()
            "Matrix" -> MatrixAnimation()
            "MetaBalls" -> MetaBallsAnimation()
            "Game Of Life" -> GameOfLifeAnimation()
            "Julia" -> JuliaAnimation()
            "Swirl" -> SwirlAnimation()
            "Pacifica" -> PacificaAnimation()
            "Blobs" -> BlobsAnimation()
            "DistortionWaves" -> DistortionWavesAnimation()
            "Plasmoid" -> PlasmoidAnimation()
            "PolarLights" -> PolarLightsAnimation()
            "Space Ships" -> SpaceShipsAnimation()
            "SquareSwirl" -> SquareSwirlAnimation()
            "Puddles" -> PuddlesAnimation()
            "Lissajous" -> LissajousAnimation()
            "Tartan" -> TartanAnimation()
            "Waverly" -> WaverlyAnimation()
            "CrazyBees" -> CrazyBeesAnimation()
            "GhostRider" -> GhostRiderAnimation()
            "SunRadiation" -> SunRadiationAnimation()
            "WashingMachine" -> WashingMachineAnimation()
            "RotoZoomer" -> RotoZoomerAnimation()
            "Tetrix" -> TetrixAnimation()
            "Hiphotic" -> HiphoticAnimation()
            "BlackHole" -> BlackHoleAnimation()
            "FunkyPlank" -> FunkyPlankAnimation()
            "DriftRose" -> DriftRoseAnimation()
            "Matripix" -> MatripixAnimation()
            "WavingCell" -> WavingCellAnimation()
            "Frizzles" -> FrizzlesAnimation()
            "PixelWave" -> PixelWaveAnimation()
            "FreqMatrix" -> FreqMatrixAnimation()
            "Lake" -> LakeAnimation()
            "DnaSpiral" -> DnaSpiralAnimation()
            "Globe" -> GlobeAnimation()
            "Fireworks" -> FireworksAnimation()
            "InfiniteTunnel" -> InfiniteTunnelAnimation()
            "Sonar" -> SonarAnimation()
            "ScrollingText" -> ScrollingTextAnimation()
            "Aquarium" -> AquariumAnimation()
            "FractalZoom" -> FractalZoomAnimation()
            "Physarum" -> PhysarumAnimation()
            "ReactionDiffusion" -> ReactionDiffusionAnimation()
            else -> BouncingBallAnimation(dropX, dropY, 30f)
        }
    }

    fun createSavedAnimation(region: AnimationRegion): SavedAnimation {
        val type = when(region.animation) {
            is GlobeAnimation -> "Globe"
            is BouncingBallAnimation -> "Ball"
            is FireworksAnimation -> "Fireworks"
            is InfiniteTunnelAnimation -> "InfiniteTunnel"
            is FractalZoomAnimation -> "FractalZoom"
            is PhysarumAnimation -> "Physarum"
            is ReactionDiffusionAnimation -> "ReactionDiffusion"
            is SonarAnimation -> "Sonar"
            is ScrollingTextAnimation -> "ScrollingText"
            is AquariumAnimation -> "Aquarium"
            is AuroraBorealisAnimation -> "Aurora Borealis"
            is BlurzAnimation -> "Blurz"
            is GeqAnimation -> "GEQ"
            is SpectrogramAnimation -> "Spectrogram"
            is FlashlightAnimation -> "Flashlight"
            is MusicBallAnimation -> "MusicBall"
            is DeathStarAnimation -> "DeathStarRun"
            is FirefliesAnimation -> "Fireflies"
            is TronRecognizerAnimation -> "TronRecognizer"
            is SpectrumTreeAnimation -> "SpectrumTree"
            is SoapAnimation -> "Soap"
            is AkemiAnimation -> "Akemi"
            is Fire2012_2DAnimation -> "Fire 2012 2D"
            is FireNoise2DAnimation -> "FireNoise2D"
            is Noise2DAnimation -> "Noise2D"
            is PlasmaBall2DAnimation -> "PlasmaBall2D"
            is MatrixAnimation -> "Matrix"
            is MetaBallsAnimation -> "MetaBalls"
            is GameOfLifeAnimation -> "Game Of Life"
            is JuliaAnimation -> "Julia"
            is SwirlAnimation -> "Swirl"
            is PacificaAnimation -> "Pacifica"
            is BlobsAnimation -> "Blobs"
            is DistortionWavesAnimation -> "DistortionWaves"
            is PlasmoidAnimation -> "Plasmoid"
            is PolarLightsAnimation -> "PolarLights"
            is SpaceShipsAnimation -> "Space Ships"
            is SquareSwirlAnimation -> "SquareSwirl"
            is PuddlesAnimation -> "Puddles"
            is LissajousAnimation -> "Lissajous"
            is TartanAnimation -> "Tartan"
            is WaverlyAnimation -> "Waverly"
            is CrazyBeesAnimation -> "CrazyBees"
            is GhostRiderAnimation -> "GhostRider"
            is SunRadiationAnimation -> "SunRadiation"
            is WashingMachineAnimation -> "WashingMachine"
            is RotoZoomerAnimation -> "RotoZoomer"
            is TetrixAnimation -> "Tetrix"
            is HiphoticAnimation -> "Hiphotic"
            is BlackHoleAnimation -> "BlackHole"
            is FunkyPlankAnimation -> "FunkyPlank"
            is DriftRoseAnimation -> "DriftRose"
            is MatripixAnimation -> "Matripix"
            is WavingCellAnimation -> "WavingCell"
            is FrizzlesAnimation -> "Frizzles"
            is PixelWaveAnimation -> "PixelWave"
            is FreqMatrixAnimation -> "FreqMatrix"
            is LakeAnimation -> "Lake"
            is DnaSpiralAnimation -> "DnaSpiral"
            else -> "Ball"
        }

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
            paletteName = if (region.animation.supportsPalette()) region.animation.currentPalette?.name else null
        )
    }

    fun getAvailableAnimations(): List<AnimationMetadata> {
        val audioReactive = setOf(
            "GEQ", "MusicBall", "SpectrumTree", "Fireworks", "InfiniteTunnel", "Sonar"
        )
        
        return listOf(
            "Akemi", "Aquarium", "Fire 2012 2D", "FireNoise2D", "Noise2D", "PlasmaBall2D",
            "Matrix", "MetaBalls", "Game Of Life", "Julia", "Swirl", "Pacifica", "Blobs",
            "DistortionWaves", "Plasmoid", "PolarLights", "Space Ships", "SquareSwirl",
            "Puddles", "Lissajous", "Tartan", "Waverly", "CrazyBees", "GhostRider",
            "SunRadiation", "WashingMachine", "RotoZoomer", "Tetrix", "Hiphotic",
            "BlackHole", "FunkyPlank", "DriftRose", "Matripix", "WavingCell", "Frizzles",
            "PixelWave", "FreqMatrix", "Lake", "DnaSpiral", "Globe", "Ball", 
            "Spectrogram", "InfiniteTunnel", "FractalZoom", "Sonar", "ScrollingText", "Fireworks",
            "Aurora Borealis", "Blurz", "GEQ", "MusicBall", "DeathStarRun",
            "Flashlight", "Fireflies", "TronRecognizer", "SpectrumTree", "Soap", "Physarum",
            "ReactionDiffusion"
        ).sorted().map { name ->
            AnimationMetadata(name, audioReactive.contains(name))
        }
    }

    data class AnimationMetadata(val name: String, val isAudioReactive: Boolean)
}
