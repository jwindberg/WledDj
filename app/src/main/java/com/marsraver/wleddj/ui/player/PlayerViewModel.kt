package com.marsraver.wleddj.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marsraver.wleddj.data.repository.InstallationRepository
import com.marsraver.wleddj.engine.RenderEngine
import com.marsraver.wleddj.engine.animations.BouncingBallAnimation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

class PlayerViewModel(
    application: Application,
    private val installationId: String,
    private val repository: InstallationRepository
) : AndroidViewModel(application) {

    private val _engine = MutableStateFlow<RenderEngine?>(null)
    val engine = _engine.asStateFlow()
    
    private val _installation = MutableStateFlow<com.marsraver.wleddj.data.model.Installation?>(null)
    val installation = _installation.asStateFlow()

    init {
        loadInstallation()
    }

    private var _originalInstallation: com.marsraver.wleddj.data.model.Installation? = null

    private fun loadInstallation() {
        viewModelScope.launch {
            val loadedInst = repository.getInstallation(installationId)
            if (loadedInst != null) {
                _originalInstallation = loadedInst
                // Initial load with existing bounds (square/default).
                android.util.Log.d("WledDj", "DEBUG: Loaded ${loadedInst.animations.size} animations")
                // We will resize correctly once UI reports screen size.
                // But initially, ensure we don't clip obviously.
                updateInstallationBounds(loadedInst.width, loadedInst.height)
            }
        }
    }
    
    // Called by UI when screen size is known
    // Called by UI when screen size is known
    fun onViewportSizeChanged(viewW: Float, viewH: Float) {
        val orig = _originalInstallation ?: return
        
        // DISABLE AUTOMATIC SHIFTING/RESIZING
        // We want WYSIWYG from the Editor.
        // If the user placed devices at specific coordinates, we keep them there.
        // The Camera handles the view.
        
        if (_installation.value != orig) {
             // Rebuild logic
        }
        
         _engine.value?.stop()
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         
         // Restore Animations
         orig.animations.forEach { saved ->
             val anim = createAnimation(saved.type)
             if (saved.text != null && anim.supportsText()) {
                 anim.setText(saved.text)
             }
             val region = com.marsraver.wleddj.data.model.AnimationRegion(
                 id = saved.id,
                 rect = android.graphics.RectF(saved.rectLeft, saved.rectTop, saved.rectRight, saved.rectBottom),
                 rotation = saved.rotation,
                 animation = anim
             )
             newEngine.addRegion(region)
         }
         
         _engine.value = newEngine
         newEngine.start()
         refreshRegions()
    }
    
    // Helper used by loadInstallation if viewport not ready
    private fun updateInstallationBounds(w: Float, h: Float) {
         val orig = _originalInstallation ?: return
         _engine.value?.stop()
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         
         // Restore Animations
         orig.animations.forEach { saved ->
             val anim = createAnimation(saved.type)
             if (saved.text != null && anim.supportsText()) {
                 anim.setText(saved.text)
             }
             val region = com.marsraver.wleddj.data.model.AnimationRegion(
                 id = saved.id,
                 rect = android.graphics.RectF(saved.rectLeft, saved.rectTop, saved.rectRight, saved.rectBottom),
                 rotation = saved.rotation,
                 animation = anim
             )
             newEngine.addRegion(region)
         }
         
         _engine.value = newEngine
         newEngine.start()
         refreshRegions()
    }

    // Regions State
    private val _regions = MutableStateFlow<List<com.marsraver.wleddj.data.model.AnimationRegion>>(emptyList())
    val regions = _regions.asStateFlow()

    fun refreshRegions() {
         _regions.value = _engine.value?.getRegions() ?: emptyList()
    }

    fun addTestAnimation() {
        // Add a region in the center
        val regionSize = 300f
        val installation = _engine.value?.let { 
             // We don't have direct access to installation dims here easily without refactor, 
             // but we can assume safe defaults or update model to expose it.
             // For now, let's just create a rect.
             android.graphics.RectF(100f, 100f, 100f + regionSize, 100f + regionSize)
        } ?: android.graphics.RectF(0f, 0f, 300f, 300f)
        
        val region = com.marsraver.wleddj.data.model.AnimationRegion(
            rect = regionSize.let { s -> android.graphics.RectF(100f, 100f, 100f+s, 100f+s) }, // explicit
            animation = BouncingBallAnimation(50f, 50f, 30f)
        )
        _engine.value?.addRegion(region)
        refreshRegions()
    }
    

    
    fun updateRegion(id: String, newRect: android.graphics.RectF, rotation: Float) {
        _engine.value?.updateRegion(id, newRect, rotation)
        refreshRegions()
        // Removed saveAnimations() to prevent flooding. 
        // Logic moved to onInteractionEnd in UI.
    }
    
    fun removeRegion(id: String) {
        _engine.value?.removeRegion(id)
        refreshRegions()
        saveAnimations()
    }

    fun clearAnimations() {
        _engine.value?.clearAnimations()
        refreshRegions()
        saveAnimations()
    }


    // Selection State
    private val _selectedRegionId = MutableStateFlow<String?>(null)
    val selectedRegionId: StateFlow<String?> = _selectedRegionId.asStateFlow()

    fun selectRegion(id: String?) {
        if (id != null) {
            _engine.value?.bringToFront(id)
            refreshRegions() // Update list order in VM
            saveAnimations() // Persist order
        }
        _selectedRegionId.value = id
        refreshControlsState()
    }
    
    // Controls State
    data class AnimationControlsState(
        val hasSelection: Boolean = false,
        val supportsPrimary: Boolean = false,
        val supportsSecondary: Boolean = false,
        val supportsPalette: Boolean = false,
        val supportsText: Boolean = false,
        val primaryColor: Int = android.graphics.Color.WHITE,
        val secondaryColor: Int = android.graphics.Color.BLACK,
        val currentPaletteName: String = "Default",
        val currentText: String = ""
    )
    
    private val _animationControlsState = MutableStateFlow(AnimationControlsState())
    val animationControlsState = _animationControlsState.asStateFlow()
    
    private fun refreshControlsState() {
        val id = _selectedRegionId.value
        val region = if (id != null) _engine.value?.getRegions()?.find { it.id == id } else null
        val anim = region?.animation
        
        if (anim != null) {
            _animationControlsState.value = AnimationControlsState(
                hasSelection = true,
                supportsPrimary = anim.supportsPrimaryColor(),
                supportsSecondary = anim.supportsSecondaryColor(),
                supportsPalette = anim.supportsPalette(),
                supportsText = anim.supportsText(),
                primaryColor = anim.primaryColor,
                secondaryColor = anim.secondaryColor,
                currentPaletteName = anim.currentPalette?.name ?: "Default",
                currentText = if (anim.supportsText()) anim.getText() else ""
            )
        } else {
            _animationControlsState.value = AnimationControlsState()
        }
    }
    
    fun setPrimaryColor(color: Int) {
        val anim = getSelectedAnimation() ?: return
        anim.primaryColor = color
        refreshControlsState()
        saveAnimations()
    }
    
    fun setSecondaryColor(color: Int) {
        val anim = getSelectedAnimation() ?: return
        anim.secondaryColor = color
        refreshControlsState()
        saveAnimations()
    }
    
    fun setPalette(paletteName: String) {
        val anim = getSelectedAnimation() ?: return
        val pal = Palettes.get(paletteName) ?: return
        anim.currentPalette = pal
        refreshControlsState()
        saveAnimations()
    }
    
    fun updateText(text: String) {
        val anim = getSelectedAnimation() ?: return
        if (anim.supportsText()) {
            anim.setText(text)
            refreshControlsState()
            saveAnimations()
        }
    }
    
    private fun getSelectedAnimation(): com.marsraver.wleddj.engine.Animation? {
        val id = _selectedRegionId.value ?: return null
        return _engine.value?.getRegions()?.find { it.id == id }?.animation
    }

    // Consolidated Delete Action
    fun deleteSelection() {
        val selected = _selectedRegionId.value
        if (selected != null) {
            removeRegion(selected)
            _selectedRegionId.value = null
            refreshControlsState()
        }
    }

    fun createAnimation(type: String, dropX: Float = 0f, dropY: Float = 0f): com.marsraver.wleddj.engine.Animation {
         return when(type) {
            "Ball" -> com.marsraver.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)


            "Aurora Borealis" -> com.marsraver.wleddj.engine.animations.AuroraBorealisAnimation()
            "Blurz" -> com.marsraver.wleddj.engine.animations.BlurzAnimation()
            "GEQ" -> com.marsraver.wleddj.engine.animations.GeqAnimation()
            "MusicBall" -> com.marsraver.wleddj.engine.animations.MusicBallAnimation()
            "Flashlight" -> com.marsraver.wleddj.engine.animations.FlashlightAnimation()
            "DeathStarRun" -> com.marsraver.wleddj.engine.animations.DeathStarAnimation(getApplication())
            "Spectrogram" -> com.marsraver.wleddj.engine.animations.SpectrogramAnimation()
            "Fireflies" -> com.marsraver.wleddj.engine.animations.FirefliesAnimation()
            "TronRecognizer" -> com.marsraver.wleddj.engine.animations.TronRecognizerAnimation(getApplication())
            "SpectrumTree" -> com.marsraver.wleddj.engine.animations.SpectrumTreeAnimation()
            "Soap" -> com.marsraver.wleddj.engine.animations.SoapAnimation()
            "Akemi" -> com.marsraver.wleddj.engine.animations.AkemiAnimation()
            "Fire 2012 2D" -> com.marsraver.wleddj.engine.animations.Fire2012_2DAnimation()
            "FireNoise2D" -> com.marsraver.wleddj.engine.animations.FireNoise2DAnimation()
            "Noise2D" -> com.marsraver.wleddj.engine.animations.Noise2DAnimation()
            "PlasmaBall2D" -> com.marsraver.wleddj.engine.animations.PlasmaBall2DAnimation()
            "Matrix" -> com.marsraver.wleddj.engine.animations.MatrixAnimation()
            "MetaBalls" -> com.marsraver.wleddj.engine.animations.MetaBallsAnimation()
            "Game Of Life" -> com.marsraver.wleddj.engine.animations.GameOfLifeAnimation()
            "Julia" -> com.marsraver.wleddj.engine.animations.JuliaAnimation()
            "Swirl" -> com.marsraver.wleddj.engine.animations.SwirlAnimation()
            "Pacifica" -> com.marsraver.wleddj.engine.animations.PacificaAnimation()
            "Blobs" -> com.marsraver.wleddj.engine.animations.BlobsAnimation()
            "DistortionWaves" -> com.marsraver.wleddj.engine.animations.DistortionWavesAnimation()
            "Plasmoid" -> com.marsraver.wleddj.engine.animations.PlasmoidAnimation()
            "PolarLights" -> com.marsraver.wleddj.engine.animations.PolarLightsAnimation()
            "Space Ships" -> com.marsraver.wleddj.engine.animations.SpaceShipsAnimation()
            "SquareSwirl" -> com.marsraver.wleddj.engine.animations.SquareSwirlAnimation()
            "Puddles" -> com.marsraver.wleddj.engine.animations.PuddlesAnimation()
            "Lissajous" -> com.marsraver.wleddj.engine.animations.LissajousAnimation()
            "Tartan" -> com.marsraver.wleddj.engine.animations.TartanAnimation()
            "Waverly" -> com.marsraver.wleddj.engine.animations.WaverlyAnimation()
            "CrazyBees" -> com.marsraver.wleddj.engine.animations.CrazyBeesAnimation()
            "GhostRider" -> com.marsraver.wleddj.engine.animations.GhostRiderAnimation()
            "SunRadiation" -> com.marsraver.wleddj.engine.animations.SunRadiationAnimation()
            "WashingMachine" -> com.marsraver.wleddj.engine.animations.WashingMachineAnimation()
            "RotoZoomer" -> com.marsraver.wleddj.engine.animations.RotoZoomerAnimation()
            "Tetrix" -> com.marsraver.wleddj.engine.animations.TetrixAnimation()
            "Hiphotic" -> com.marsraver.wleddj.engine.animations.HiphoticAnimation()
            "BlackHole" -> com.marsraver.wleddj.engine.animations.BlackHoleAnimation()
            "FunkyPlank" -> com.marsraver.wleddj.engine.animations.FunkyPlankAnimation()
            "DriftRose" -> com.marsraver.wleddj.engine.animations.DriftRoseAnimation()
            "Matripix" -> com.marsraver.wleddj.engine.animations.MatripixAnimation()
            "WavingCell" -> com.marsraver.wleddj.engine.animations.WavingCellAnimation()
            "Frizzles" -> com.marsraver.wleddj.engine.animations.FrizzlesAnimation()
            "PixelWave" -> com.marsraver.wleddj.engine.animations.PixelWaveAnimation()
            "FreqMatrix" -> com.marsraver.wleddj.engine.animations.FreqMatrixAnimation()
            "Lake" -> com.marsraver.wleddj.engine.animations.LakeAnimation()
            "DnaSpiral" -> com.marsraver.wleddj.engine.animations.DnaSpiralAnimation()
            "Globe" -> com.marsraver.wleddj.engine.animations.GlobeAnimation()
            "Fireworks" -> com.marsraver.wleddj.engine.animations.FireworksAnimation()
            "InfiniteTunnel" -> com.marsraver.wleddj.engine.animations.InfiniteTunnelAnimation()
            "Sonar" -> com.marsraver.wleddj.engine.animations.SonarAnimation()
            "ScrollingText" -> com.marsraver.wleddj.engine.animations.ScrollingTextAnimation()
            "Aquarium" -> com.marsraver.wleddj.engine.animations.AquariumAnimation()

            else -> com.marsraver.wleddj.engine.animations.BouncingBallAnimation(dropX, dropY, 30f)
        }
    }
    
    // Made Public for UI to call on Drag End
    fun saveAnimations() {
        val currentEngine = _engine.value ?: return
        val currentInst = _installation.value ?: return
        
        val regions = currentEngine.getRegions()
        val savedList = regions.map { region ->
            val type = when(region.animation) {
                is com.marsraver.wleddj.engine.animations.GlobeAnimation -> "Globe"
                is com.marsraver.wleddj.engine.animations.BouncingBallAnimation -> "Ball"

                is com.marsraver.wleddj.engine.animations.FireworksAnimation -> "Fireworks"
                is com.marsraver.wleddj.engine.animations.InfiniteTunnelAnimation -> "InfiniteTunnel"
                is com.marsraver.wleddj.engine.animations.SonarAnimation -> "Sonar"
                is com.marsraver.wleddj.engine.animations.ScrollingTextAnimation -> "ScrollingText"
                is com.marsraver.wleddj.engine.animations.AquariumAnimation -> "Aquarium"

                is com.marsraver.wleddj.engine.animations.AuroraBorealisAnimation -> "Aurora Borealis"
                is com.marsraver.wleddj.engine.animations.BlurzAnimation -> "Blurz"
                is com.marsraver.wleddj.engine.animations.GeqAnimation -> "GEQ"
                is com.marsraver.wleddj.engine.animations.SpectrogramAnimation -> "Spectrogram"
                is com.marsraver.wleddj.engine.animations.FlashlightAnimation -> "Flashlight"
                is com.marsraver.wleddj.engine.animations.MusicBallAnimation -> "MusicBall"
                is com.marsraver.wleddj.engine.animations.DeathStarAnimation -> "DeathStarRun"
                is com.marsraver.wleddj.engine.animations.FirefliesAnimation -> "Fireflies"

                is com.marsraver.wleddj.engine.animations.TronRecognizerAnimation -> "TronRecognizer"
                is com.marsraver.wleddj.engine.animations.SpectrumTreeAnimation -> "SpectrumTree"
                is com.marsraver.wleddj.engine.animations.SoapAnimation -> "Soap"
                is com.marsraver.wleddj.engine.animations.AkemiAnimation -> "Akemi"
                is com.marsraver.wleddj.engine.animations.Fire2012_2DAnimation -> "Fire 2012 2D"
                is com.marsraver.wleddj.engine.animations.FireNoise2DAnimation -> "FireNoise2D"
                is com.marsraver.wleddj.engine.animations.Noise2DAnimation -> "Noise2D"
                is com.marsraver.wleddj.engine.animations.PlasmaBall2DAnimation -> "PlasmaBall2D"
                is com.marsraver.wleddj.engine.animations.MatrixAnimation -> "Matrix"
                is com.marsraver.wleddj.engine.animations.MetaBallsAnimation -> "MetaBalls"
                is com.marsraver.wleddj.engine.animations.GameOfLifeAnimation -> "Game Of Life"
                is com.marsraver.wleddj.engine.animations.JuliaAnimation -> "Julia"
                is com.marsraver.wleddj.engine.animations.SwirlAnimation -> "Swirl"
                is com.marsraver.wleddj.engine.animations.PacificaAnimation -> "Pacifica"
                is com.marsraver.wleddj.engine.animations.BlobsAnimation -> "Blobs"
                is com.marsraver.wleddj.engine.animations.DistortionWavesAnimation -> "DistortionWaves"
                is com.marsraver.wleddj.engine.animations.PlasmoidAnimation -> "Plasmoid"
                is com.marsraver.wleddj.engine.animations.PolarLightsAnimation -> "PolarLights"
                is com.marsraver.wleddj.engine.animations.SpaceShipsAnimation -> "Space Ships"
                is com.marsraver.wleddj.engine.animations.SquareSwirlAnimation -> "SquareSwirl"
                is com.marsraver.wleddj.engine.animations.PuddlesAnimation -> "Puddles"
                is com.marsraver.wleddj.engine.animations.LissajousAnimation -> "Lissajous"
                is com.marsraver.wleddj.engine.animations.TartanAnimation -> "Tartan"
                is com.marsraver.wleddj.engine.animations.WaverlyAnimation -> "Waverly"
                is com.marsraver.wleddj.engine.animations.CrazyBeesAnimation -> "CrazyBees"
                is com.marsraver.wleddj.engine.animations.GhostRiderAnimation -> "GhostRider"
                is com.marsraver.wleddj.engine.animations.SunRadiationAnimation -> "SunRadiation"
                is com.marsraver.wleddj.engine.animations.WashingMachineAnimation -> "WashingMachine"
                is com.marsraver.wleddj.engine.animations.RotoZoomerAnimation -> "RotoZoomer"
                is com.marsraver.wleddj.engine.animations.TetrixAnimation -> "Tetrix"
                is com.marsraver.wleddj.engine.animations.HiphoticAnimation -> "Hiphotic"
                is com.marsraver.wleddj.engine.animations.BlackHoleAnimation -> "BlackHole"
                is com.marsraver.wleddj.engine.animations.FunkyPlankAnimation -> "FunkyPlank"
                is com.marsraver.wleddj.engine.animations.DriftRoseAnimation -> "DriftRose"
                is com.marsraver.wleddj.engine.animations.MatripixAnimation -> "Matripix"
                is com.marsraver.wleddj.engine.animations.WavingCellAnimation -> "WavingCell"
                is com.marsraver.wleddj.engine.animations.FrizzlesAnimation -> "Frizzles"
                is com.marsraver.wleddj.engine.animations.PixelWaveAnimation -> "PixelWave"
                is com.marsraver.wleddj.engine.animations.FreqMatrixAnimation -> "FreqMatrix"
                is com.marsraver.wleddj.engine.animations.LakeAnimation -> "Lake"
                is com.marsraver.wleddj.engine.animations.DnaSpiralAnimation -> "DnaSpiral"
                else -> "Ball"
            }
            
            com.marsraver.wleddj.data.model.SavedAnimation(
                id = region.id,
                type = type,
                rectLeft = region.rect.left,
                rectTop = region.rect.top,
                rectRight = region.rect.right,
                rectBottom = region.rect.bottom,
                rotation = region.rotation,
                text = if (region.animation.supportsText()) region.animation.getText() else null
            )
        }
        
        val newInst = currentInst.copy(animations = savedList)
        
        // UNCONDITIONAL UPDATE of Original to prevent Zombie state
        _originalInstallation = newInst
        _installation.value = newInst
        
        // Only write to disk if changed (optimization)
        if (currentInst.animations != savedList) {
            android.util.Log.d("WledDj", "DEBUG: Saving ${savedList.size} animations")
            viewModelScope.launch {
                repository.updateInstallation(newInst)
            }
        }
    }

    fun onToolDropped(type: String, dropX: Float, dropY: Float, installW: Float, installH: Float) {
        val size = 300f // Default Size
        
        val animation = createAnimation(type, dropX, dropY)
        
        // Center the rect on the drop point
        val region = com.marsraver.wleddj.data.model.AnimationRegion(
            rect = android.graphics.RectF(dropX - size/2, dropY - size/2, dropX + size/2, dropY + size/2),
            animation = animation
        )
        _engine.value?.addRegion(region)
        refreshRegions()
        
        // Auto-select
        val regions = _regions.value
        _selectedRegionId.value = regions.lastOrNull()?.id
        refreshControlsState()
        
        saveAnimations()
    }
    
    private fun minCode(a: Float, b: Float): Float = if (a < b) a else b
    private fun maxCode(a: Float, b: Float): Float = if (a > b) a else b

    // Interaction Mode
    private val _isInteractiveMode = MutableStateFlow(false)
    val isInteractiveMode = _isInteractiveMode.asStateFlow()
    
    fun toggleInteractiveMode() {
        _isInteractiveMode.value = !_isInteractiveMode.value
    }
    
    fun handleCanvasTouch(x: Float, y: Float) {
        if (_isInteractiveMode.value) {
            _engine.value?.handleTouch(x, y)
        }
    }
    
    fun handleCanvasTransform(targetX: Float, targetY: Float, panX: Float, panY: Float, zoom: Float, rotation: Float) {
        if (_isInteractiveMode.value) {
            _engine.value?.handleTransform(targetX, targetY, panX, panY, zoom, rotation)
        }
    }



    private val _deviceStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val deviceStatuses = _deviceStatuses.asStateFlow()
    
    private var monitorJob: kotlinx.coroutines.Job? = null

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            while (isActive) {
                val devices = _installation.value?.devices ?: emptyList()
                if (devices.isNotEmpty()) {
                    val scope = this
                    val tasks = devices.map { device ->
                         val task = scope.async { 
                             com.marsraver.wleddj.data.repository.WledApiHelper.pingDevice(device.ip) 
                         }
                         device.ip to task
                    }
                    
                    val map = mutableMapOf<String, Boolean>()
                    for ((ip, task) in tasks) {
                        map[ip] = task.await()
                    }
                    _deviceStatuses.value = map
                }
                delay(5000)
            }
        }
    }
    
    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun pauseEngine() {
        android.util.Log.d("PlayerViewModel", "Pausing Engine (Lifecycle)")
        _engine.value?.stop()
        stopMonitoring()
    }

    fun resumeEngine() {
        android.util.Log.d("PlayerViewModel", "Resuming Engine (Lifecycle)")
        _engine.value?.start()
        startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        _engine.value?.stop()
        stopMonitoring()
    }

    class Factory(
        private val application: Application,
        private val installationId: String,
        private val repository: InstallationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application, installationId, repository) as T
        }
    }
}
