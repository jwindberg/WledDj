package com.marsraver.wleddj.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marsraver.wleddj.repository.InstallationRepository
import com.marsraver.wleddj.engine.RenderEngine
// Animations imports delegated to AnimationFactory
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.model.AnimationType

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
    private val repository: InstallationRepository,
    private val networkManager: com.marsraver.wleddj.wled.NetworkManager = com.marsraver.wleddj.wled.NetworkManager() // Default for now
) : AndroidViewModel(application) {

    private val _engine = MutableStateFlow<RenderEngine?>(null)
    val engine = _engine.asStateFlow()
    
    private val _installation = MutableStateFlow<com.marsraver.wleddj.model.Installation?>(null)
    val installation = _installation.asStateFlow()

    init {
        loadInstallation()
    }

    private var _originalInstallation: com.marsraver.wleddj.model.Installation? = null
    // HttpClient moved to NetworkManager

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
    private var lastViewW = 0f
    private var lastViewH = 0f

    // Called by UI when screen size is known
    fun onViewportSizeChanged(viewW: Float, viewH: Float) {
        // Debounce/Ignore duplicate calls
        if (kotlin.math.abs(viewW - lastViewW) < 1f && kotlin.math.abs(viewH - lastViewH) < 1f) {
            return
        }
        lastViewW = viewW
        lastViewH = viewH
        android.util.Log.d("WledDj", "Viewport changed: $viewW x $viewH")

        val orig = _originalInstallation ?: return
        
        // DISABLE AUTOMATIC SHIFTING/RESIZING
        // We want WYSIWYG from the Editor.
        // If the user placed devices at specific coordinates, we keep them there.
        // The Camera handles the view.
        
        if (_installation.value != orig) {
             // Rebuild logic
        }
        
         _engine.value?.destroy() // Ensure complete teardown
         _installation.value = orig
         val newEngine = RenderEngine(orig)
         
         // Restore Animations
         orig.animations.forEach { saved ->
             val anim = AnimationFactory.createAnimation(saved.type, getApplication())
             if (saved.text != null && anim.supportsText()) {
                 anim.setText(saved.text)
             }
             if (saved.primaryColor != null && anim.supportsPrimaryColor()) {
                 anim.primaryColor = saved.primaryColor
             }
             if (saved.secondaryColor != null && anim.supportsSecondaryColor()) {
                 anim.secondaryColor = saved.secondaryColor
             }
             if (saved.paletteName != null && anim.supportsPalette()) {
                 anim.currentPalette = saved.paletteName
             }
             val region = com.marsraver.wleddj.model.AnimationRegion(
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
             val anim = AnimationFactory.createAnimation(saved.type, getApplication())
             if (saved.text != null && anim.supportsText()) {
                 anim.setText(saved.text)
             }
             val region = com.marsraver.wleddj.model.AnimationRegion(
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
    private val _regions = MutableStateFlow<List<com.marsraver.wleddj.model.AnimationRegion>>(emptyList())
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
        
        val region = com.marsraver.wleddj.model.AnimationRegion(
            rect = regionSize.let { s -> android.graphics.RectF(100f, 100f, 100f+s, 100f+s) }, // explicit
            animation = com.marsraver.wleddj.animations.BouncingBallAnimation(50f, 50f, 30f)
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
        val currentPalette: Palette = Palette.DEFAULT,
        val currentText: String = ""
    )
    
    private val _animationControlsState = MutableStateFlow(AnimationControlsState())
    val animationControlsState = _animationControlsState.asStateFlow()
    
    private fun refreshControlsState() {
        val id = _selectedRegionId.value
        val region = if (id != null) _engine.value?.getRegions()?.find { it.id == id } else null
        val anim = region?.animation
        
        val pal = anim?.currentPalette ?: Palette.DEFAULT

        if (anim != null) {
            _animationControlsState.value = AnimationControlsState(
                hasSelection = true,
                supportsPrimary = anim.supportsPrimaryColor(),
                supportsSecondary = anim.supportsSecondaryColor(),
                supportsPalette = anim.supportsPalette(),
                supportsText = anim.supportsText(),
                primaryColor = anim.primaryColor,
                secondaryColor = anim.secondaryColor,
                currentPalette = pal,
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
    
    fun setPalette(palette: Palette) {
        val anim = getSelectedAnimation() ?: return
        anim.currentPalette = palette
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


    
    // Made Public for UI to call on Drag End
    fun saveAnimations() {
        val currentEngine = _engine.value ?: return
        val currentInst = _installation.value ?: return
        
        val regions = currentEngine.getRegions()
        val savedList = regions.map { region ->
            AnimationFactory.createSavedAnimation(region)
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

    fun onToolDropped(type: AnimationType, dropX: Float, dropY: Float, installW: Float, installH: Float, zoom: Float) {
        val isFullscreen = isFullscreenEffect(type)
        
        val size = if (isFullscreen) {
             // Fullscreen effects cover the entire installation
             // We use max dimension to ensure coverage if rotated, or just max(W,H)
             kotlin.math.max(installW, installH)
        } else {
             // Standard sizing: Constant Visual Size / Zoom, clamped
             val baseVisualSize = 300f
             val targetSize = baseVisualSize / zoom
             val maxAllowedSize = if (installW < installH) installW else installH
             if (targetSize > maxAllowedSize) maxAllowedSize else targetSize
        }
        
         val animation = AnimationFactory.createAnimation(type, getApplication(), dropX, dropY)
        
        // Center: If fullscreen, strictly center on installation. Else use drop point.
        val cx = if (isFullscreen) installW / 2f else dropX
        val cy = if (isFullscreen) installH / 2f else dropY
        
        val region = com.marsraver.wleddj.model.AnimationRegion(
            rect = android.graphics.RectF(cx - size/2, cy - size/2, cx + size/2, cy + size/2),
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

    private fun isFullscreenEffect(type: AnimationType): Boolean {
        // Only TronRecognizer remains fullscreen by default for now
        return type == AnimationType.TRON_RECOGNIZER
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



    val deviceStatuses = networkManager.deviceStatuses
    
    private fun startMonitoring() {
        // Update targets whenever we start, just in case
        val devices = _installation.value?.devices ?: emptyList()
        networkManager.setTargets(devices)
        networkManager.startMonitoring(viewModelScope)
    }
    
    private fun stopMonitoring() {
        networkManager.stopMonitoring()
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
        _engine.value?.destroy()
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
